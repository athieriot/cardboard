package cards.types

import cards.*
import cards.mana.{ManaCost, *}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.Try

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[LlanowarElf], name = "LlanowarElf"),
  )
)
sealed abstract class Creature extends PermanentCard {
  def checkConditions(state: BoardState, player: PlayerId): Try[Unit] = Try {
    if state.activePlayer != player then
      throw new RuntimeException("You can only play creatures during your turn")
    else if !state.currentStep.isMain then
      throw new RuntimeException("You can only play creatures during a main phase")
    else if state.stack.nonEmpty then
      throw new RuntimeException("You can only play creatures when the stack is empty")
    else if !cost.canPay(state, player) then
      throw new RuntimeException("Cannot pay the cost")
  }
  def effects(id: CardId, state: BoardState, player: PlayerId): List[Event] = List(EnteredTheBattlefield(id))
}

class LlanowarElf(val set: MagicSet, val numberInSet: Int) extends Creature {
  val name: String = "Llanowar Elf"
  val subTypes: List[String] = List("Creature", "Elf Druid")
  val color: Color = Color.green
  val cost: CastingCost = ManaCost("G")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)
  
  // TODO: Effect = ETB
  def activatedAbilities: Map[Int, Ability] = Map(
    1 -> Ability(Tap, "Add one green mana", (_, player) => List(ManaAdded(Map(Color.green -> 1), player)))
  )
}