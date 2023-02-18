package cards.mana

import cards.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*

// TODO: Could also have a "condition"
case class Ability(cost: AbilityCost, text: String, effect: (BoardState, String) => List[Event])

trait AbilityCost {
  // TODO: Feel like the target should be on the Ability instance
  def canPay(target: Permanent): Boolean
  def pay(target: CardId, player: PlayerId): List[Event]
}
case object Tap extends AbilityCost {
  def canPay(target: Permanent): Boolean = target.status == Status.Untapped
  def pay(target: CardId, player: PlayerId): List[Event] = List(Tapped(target))
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ManaCost], name = "mana"),
  )
)
trait CastingCost {
  def canPay(state: BoardState, player: PlayerId): Boolean
  def pay(target: CardId, player: PlayerId): List[Event]
}
// TODO: Validate valid ManaCost text
case class ManaCost(text: String) extends CastingCost {
  def canPay(state: BoardState, player: PlayerId): Boolean = (state.players(player).manaPool - this).isSuccess
  def pay(target: CardId, player: PlayerId): List[Event] = List(ManaPaid(this, player))
}