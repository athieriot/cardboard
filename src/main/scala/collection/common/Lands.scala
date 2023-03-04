package collection.common

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.cards.types.Land
import game.{Context, ManaAdded}
import game.mana.{Color, Cost, NoCost, Tap}
import game.mechanics.ActivatedAbility

sealed abstract class BasicLand extends Land {
  val color: Color = Color.colorless

  def colorProduced: Color

  override def activatedAbilities: Map[Int, ActivatedAbility] = Map(
    1 -> ActivatedAbility(new Tap(), s"Add one ${colorProduced.toString} mana", (_, ctx: Context, _) => List(ManaAdded(Map(colorProduced -> 1), ctx.player)), manaAbility = true)
  )
}

abstract class Forest extends BasicLand {
  val name: String = "Forest"
  val subTypes: List[String] = List("Basic Land", "Forest")
  val cost: Cost = new NoCost
  val colorProduced: Color = Color.green
  val basePower: Option[Int] = None
  val baseToughness: Option[Int] = None
}

abstract class Island extends BasicLand {
  val name: String = "Island"
  val subTypes: List[String] = List("Basic Land", "Island")
  val cost: Cost = new NoCost
  val colorProduced: Color = Color.blue
  val basePower: Option[Int] = None
  val baseToughness: Option[Int] = None
}

abstract class Mountain extends BasicLand {
  val name: String = "Mountain"
  val subTypes: List[String] = List("Basic Land", "Mountain")
  val cost: Cost = new NoCost
  val colorProduced: Color = Color.red
  val basePower: Option[Int] = None
  val baseToughness: Option[Int] = None
}