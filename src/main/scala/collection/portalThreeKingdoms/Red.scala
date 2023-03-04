package collection.portalThreeKingdoms

import collection.sets.PortalThreeKingdoms
import game.cards.types.Creature
import game.mana.{Color, Cost, ManaCost}
import game.mechanics.KeywordAbilities

class MountainBandit(val numberInSet: Int = 117) extends Creature with PortalThreeKingdoms {
  val name: String = "Mountain Bandit"
  val subTypes: List[String] = List("Creature", "Soldier")
  val color: Color = Color.red
  val cost: Cost = ManaCost("R")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)

  override def keywordAbilities: List[KeywordAbilities] = List(KeywordAbilities.haste)
}