package game.cards.types

import game.cards.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.cards.*
import game.mana.*
import game.mechanics.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.{Success, Try}

abstract class Instant extends Card {
  override def conditions(ctx: Context): Try[Unit] = Try {}
  
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(PutIntoGraveyard(id, ctx.player))
}