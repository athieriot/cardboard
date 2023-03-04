package game.cards.types

import game.cards.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.mechanics.*
import game.*
import game.cards.*
import game.mana.*

import scala.util.{Success, Try}

abstract class Artifact extends PermanentCard {
  override def conditions(ctx: Context): Try[Unit] = Try {
    if ctx.state.activePlayer != ctx.player then
      throw new RuntimeException("You can only play artifacts during your turn")
    else if !ctx.state.currentStep.isMain then
      throw new RuntimeException("You can only play artifacts during a main phase")
    else if ctx.state.stack.nonEmpty then
      throw new RuntimeException("You can only play artifacts when the stack is empty")
  }

  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(EnteredTheBattlefield(id))
}