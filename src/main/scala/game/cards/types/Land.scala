package game.cards.types

import game.cards.*
import collection.common.BasicLand
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.cards.{Card, CardState, PermanentCard}
import game.mana.{Color, Cost, NoCost, Tap}
import game.mechanics.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.Try

abstract class Land extends PermanentCard {
  override def conditions(ctx: Context): Try[Unit] = Try {
    if ctx.state.activePlayer != ctx.player then
      throw new RuntimeException("You can only play lands during your turn")
    else if !ctx.state.currentStep.isMain then
      throw new RuntimeException("You can only play lands during a main phase")
    else if ctx.state.stack.nonEmpty then
      throw new RuntimeException("You can only play lands when the stack is empty")
    else if ctx.state.players(ctx.player).landsToPlay <= 0 then
      throw new RuntimeException("You already played a land this turn")
  }
  
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(LandPlayed(id, ctx.player), EnteredTheBattlefield(id, ctx.player))
}