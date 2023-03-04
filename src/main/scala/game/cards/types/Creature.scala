package game.cards.types

import game.cards.*
import game.mana.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.cards.*
import game.mechanics.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.{Success, Try}

abstract class Creature extends PermanentCard {
  // TODO: I think it has to be a list of Conditions
  // TODO: Split catching condition to be able to override some of them
  override def conditions(ctx: Context): Try[Unit] = Try {
    if ctx.state.activePlayer != ctx.player then
      throw new RuntimeException("You can only play creatures during your turn")
    else if !ctx.state.currentStep.isMain then
      throw new RuntimeException("You can only play creatures during a main phase")
    else if ctx.state.stack.nonEmpty then
      throw new RuntimeException("You can only play creatures when the stack is empty")
  }
  
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(EnteredTheBattlefield(id))
}