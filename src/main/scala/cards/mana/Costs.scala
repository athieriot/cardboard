package cards.mana

import cards.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*

import scala.annotation.unused
import scala.util.Try

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ManaCost], name = "mana"),
    new JsonSubTypes.Type(value = classOf[Tap], name = "tap"),
  )
)
// TODO: Also have Suspend as a Cost Type
trait Cost {
  def pay(id: CardId, ctx: Context, cardState: CardState[Card]): Try[List[Event]]
}
class Tap extends Cost {
  def pay(id: CardId, ctx: Context, cardState: CardState[Card]): Try[List[Event]] = Try { cardState match {
    case permanent: Permanent[_] if permanent.card.isInstanceOf[PermanentCard] =>
      if permanent.status == Status.Tapped  then
        throw new RuntimeException("Permanent is already Tapped")
      else if permanent.hasSummoningSickness then
        throw new RuntimeException("Creature has summoning sickness")
      else List(Tapped(id))
    case _ => throw new RuntimeException("Target is not a Permanent")
  }}
}
// TODO: Validate valid ManaCost text ?
case class ManaCost(text: String) extends Cost {
  def pay(id: CardId, ctx: Context, cardState: CardState[Card]): Try[List[Event]] = (ctx.state.players(ctx.player).manaPool - this).map(_ => List(ManaPaid(this, ctx.player)))
}