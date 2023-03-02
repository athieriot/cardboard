package collection.fourthEdition

import collection.sets.FourthEdition
import game.*
import game.cards.*
import game.cards.types.*
import game.mana.*
import game.mechanics.*

class LlanowarElf(val numberInSet: Int = 261) extends Creature with FourthEdition {
  val name: String = "Llanowar Elf"
  val subTypes: List[String] = List("Creature", "Elf Druid")
  val color: Color = Color.green
  val cost: Cost = ManaCost("G")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)

  override def activatedAbilities: Map[Int, ActivatedAbility] = Map(
    1 -> ActivatedAbility(new Tap(), "Add one green mana", (_, ctx: Context, _) => List(ManaAdded(Map(Color.green -> 1), ctx.player)), manaAbility = true)
  )
}

class WarMammoth(val numberInSet: Int = 286) extends Creature with FourthEdition {
  val name: String = "War Mammoth"
  val subTypes: List[String] = List("Creature", "Elephant")
  val color: Color = Color.green
  val cost: Cost = ManaCost("3G")
  val basePower: Option[Int] = Some(3)
  val baseToughness: Option[Int] = Some(3)

  override def keywordAbilities: List[KeywordAbilities] = List(KeywordAbilities.trample)
}