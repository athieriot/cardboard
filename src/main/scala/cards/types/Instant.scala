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
  // TODO: I think it has to be a list of Conditions
  def checkCastingConditions(ctx: Context): Try[Unit] = Try {
    if !cost.canPay(ctx.state, ctx.player) then
      throw new RuntimeException("Cannot pay the cost")
  }
  // TODO: Do we need an "overridableCheckCondition"
  def effects(id: CardId, ctx: Context): List[Event] = List(PutIntoGraveyard(id, ctx.player))
}

class Counterspell(val set: MagicSet, val numberInSet: Int) extends Instant {
  val name: String = "Counterspell"
  val subTypes: List[String] = List("Instant")
  val color: Color = Color.blue
  val cost: CastingCost = ManaCost("UU")

  // TODO: Check condition = Target still valid

  override def checkCastingConditions(ctx: Context): Try[Unit] = super.checkCastingConditions(ctx).flatMap { _ => Try {
    // TODO: Extract in a "CounterCondition"
    ctx.args.find(_.isInstanceOf[TargetArg]) match {
      case None => throw new RuntimeException("Please specify a target")
      case Some(TargetArg(target)) =>
        if ctx.state.getCardFromZone(target, Stack).isEmpty then
          throw new RuntimeException("Target invalid, it has to be in the Stack")
    }
  }}

  override def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = super.effects(id, ctx) ++ (
    cardState match {
      case Spell(_, _, _, args) =>
        args.find(_.isInstanceOf[TargetArg]) match {
          case Some(TargetArg(target)) =>
            // TODO: Don't forget to remove Abilities
            ctx.state.getCardFromZone(target, Stack).map(_ => List(PutIntoGraveyard(target, ctx.player))).getOrElse(List())
          case None => List()
        }
      case _ => List()
    }
  )
}