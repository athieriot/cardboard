package cards.mana

import cards.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*

trait AbilityCost {
  def check(target: Spell): Boolean
  def pay(target: CardId, player: PlayerId): List[Event]
}
case object Tap extends AbilityCost {
  def check(target: Spell): Boolean = target.status == Status.Untapped
  def pay(target: CardId, player: PlayerId): List[Event] = List(Tapped(target))
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ManaCost], name = "mana"),
  )
)
trait CastingCost {
  def check(state: InProgressState, player: PlayerId): Boolean
  def pay(target: CardId, player: PlayerId): List[Event]
}
// TODO: Validate valid ManaCost
case class ManaCost(text: String) extends CastingCost {
  def check(state: InProgressState, player: PlayerId): Boolean = (state.players(player).manaPool - this).isSuccess
  def pay(target: CardId, player: PlayerId): List[Event] = List(ManaPaid(this, player))
}