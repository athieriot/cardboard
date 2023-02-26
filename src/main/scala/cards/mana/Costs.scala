package cards.mana

import cards.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*

trait AbilityCost {
  // TODO: Feel like the id should be on the Ability instance
  def canPay(card: Card|Permanent[PermanentCard]): Boolean
  def pay(id: CardId, player: PlayerId): List[Event]
}
case object Tap extends AbilityCost {
  // TODO: Also it could probably return a Try instead to return more precise messages
  def canPay(card: Card|Permanent[PermanentCard]): Boolean = card match {
    case permanent: Permanent[PermanentCard] => permanent.status == Status.Untapped && !permanent.hasSummoningSickness
    case _ => false
  }
  def pay(id: CardId, player: PlayerId): List[Event] = List(Tapped(id))
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ManaCost], name = "mana"),
  )
)
trait CastingCost {
  // TODO: Take context ?
  def canPay(state: BoardState, player: PlayerId): Boolean
  def pay(id: CardId, player: PlayerId): List[Event]
}
// TODO: Validate valid ManaCost text
case class ManaCost(text: String) extends CastingCost {
  def canPay(state: BoardState, player: PlayerId): Boolean = (state.players(player).manaPool - this).isSuccess
  def pay(id: CardId, player: PlayerId): List[Event] = List(ManaPaid(this, player))
}