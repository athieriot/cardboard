package collection.fourthEdition

import collection.sets.FourthEdition
import game.cards.*
import game.cards.*
import game.cards.types.*
import game.mana.*

class MonssGoblinRaiders(val numberInSet: Int = 213) extends Creature with FourthEdition {
  val name: String = "Mons's Goblin Raiders"
  val subTypes: List[String] = List("Creature", "Goblin")
  val color: Color = Color.red
  val cost: Cost = ManaCost("R")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)
}