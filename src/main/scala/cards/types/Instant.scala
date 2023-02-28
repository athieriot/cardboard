package cards.types

import cards.*
import cards.mana.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.mechanics.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.{Success, Try}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Counterspell], name = "counterspell"),
  )
)
sealed abstract class Instant extends Card {
  override def conditions(ctx: Context): Try[Unit] = Try {}
  
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(PutIntoGraveyard(id, ctx.player))
}

class Counterspell(val set: MagicSet, val numberInSet: Int) extends Instant {
  val name: String = "Counterspell"
  val subTypes: List[String] = List("Instant")
  val color: Color = Color.blue
  val cost: Cost = ManaCost("UU")

  override def text: String = "Counter target spell"

  override def conditions(ctx: Context): Try[Unit] = super.conditions(ctx).flatMap { _ =>
    Args.retrieveTarget(ctx.args).flatMap(target => Try { target match {
      case _: PlayerId => throw new RuntimeException("Target invalid, cannot be a player")
      case cardId: CardId =>
        ctx.state.getCardFromZone(cardId, Stack).filterNot(_.card.isToken).getOrElse(throw new RuntimeException("Target invalid, it has to be a card on the Stack"))
    }})
  }

  override def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = super.effects(id, ctx, cardState) ++ (
    cardState match {
      case Spell(_, _, _, args) =>
        Args.retrieveTarget(args) match {
          case Success(cardId: CardId) =>
            ctx.state.getCardFromZone(cardId, Stack).filterNot(_.card.isToken).map(_ => List(PutIntoGraveyard(cardId, ctx.player))).getOrElse(List())
          case _ => List()
        }
      case _ => List()
    }
  )
}