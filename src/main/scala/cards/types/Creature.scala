package cards.types

import cards.*
import cards.mana.{ManaCost, *}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.mechanics.Ability
import monocle.syntax.all.*

import java.net.URL
import scala.util.Try

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[LlanowarElf], name = "llanowarElf"),
    new JsonSubTypes.Type(value = classOf[ProdigalSorcerer], name = "prodigalSorcerer"),
    new JsonSubTypes.Type(value = classOf[MonssGoblinRaiders], name = "monssGoblinRaiders"),
  )
)
sealed abstract class Creature extends PermanentCard {
  // TODO: I think it has to be a list of Conditions
  // TODO: Split catching condition to be able to override some of them
  override def checkCastingConditions(ctx: Context): Try[Unit] = super.checkCastingConditions(ctx).flatMap { _ => Try {
    if ctx.state.activePlayer != ctx.player then
      throw new RuntimeException("You can only play creatures during your turn")
    else if !ctx.state.currentStep.isMain then
      throw new RuntimeException("You can only play creatures during a main phase")
    else if ctx.state.stack.nonEmpty then
      throw new RuntimeException("You can only play creatures when the stack is empty")
  }}
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(EnteredTheBattlefield(id))
}

class LlanowarElf(val set: MagicSet, val numberInSet: Int) extends Creature {
  val name: String = "Llanowar Elf"
  val subTypes: List[String] = List("Creature", "Elf Druid")
  val color: Color = Color.green
  val cost: CastingCost = ManaCost("G")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)

  override def activatedAbilities: Map[Int, Ability] = Map(
    1 -> Ability(Tap, "Add one green mana", (_, player) => List(ManaAdded(Map(Color.green -> 1), player)))
  )
}

class ProdigalSorcerer(val set: MagicSet, val numberInSet: Int) extends Creature {
  val name: String = "Prodigal Sorcerer"
  val subTypes: List[String] = List("Creature", "Human Wizard")
  val color: Color = Color.blue
  val cost: CastingCost = ManaCost("2U")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)

  // TODO: Check for Target
  override def activatedAbilities: Map[Int, Ability] = Map(
    1 -> Ability(Tap, "Prodigal Sorcerer deals 1 damage to any target", (_, _) => List(DamageDealt("Jill", 1)))
  )
}

class MonssGoblinRaiders(val set: MagicSet, val numberInSet: Int) extends Creature {
  val name: String = "Mons's Goblin Raiders"
  val subTypes: List[String] = List("Creature", "Goblin")
  val color: Color = Color.red
  val cost: CastingCost = ManaCost("R")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)
}