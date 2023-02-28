package cards.types

import cards.*
import cards.mana.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.mechanics.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.Try

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

  override def conditions(ctx: Context): Try[Unit] = super.conditions(ctx).flatMap { _ => Try {
    // TODO: Extract in a "CounterCondition" ?
    ctx.args.find(_.isInstanceOf[TargetArg]) match {
      case Some(TargetArg(target)) => target match {
        case _: PlayerId => throw new RuntimeException("Target invalid, cannot be a player")
        case cardId: CardId =>
          ctx.state.getCardFromZone(cardId, Stack).filterNot(_.card.isToken).getOrElse(throw new RuntimeException("Target invalid, it has to be a card on the Stack"))
      }
      case None => throw new RuntimeException("Please specify a target")
    }
  }}

  override def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = super.effects(id, ctx, cardState) ++ (
    cardState match {
      case Spell(_, _, _, args) => args.find(_.isInstanceOf[TargetArg]) match {
        case Some(TargetArg(target)) => target match {
          case _: PlayerId => List()
          case cardId: CardId =>
            ctx.state.getCardFromZone(cardId, Stack).filterNot(_.card.isToken).map(_ => List(PutIntoGraveyard(cardId, ctx.player))).getOrElse(List())
        }
        case None => List()
      }
      case _ => List()
    }
  )
}