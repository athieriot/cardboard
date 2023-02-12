package cards

import game.State
import monocle.syntax.all._

enum Color {
  case red, green, black, white, blue
}

trait Cost

case object Tapping extends Cost
case class ManaCost() extends Cost

case class Ability(cost: Cost, effect: (State, Int) => State)

trait Card {
  val name: String
  val activatedAbilities: Map[String, Ability]
}
case class Land(name: String, staticAbilities: List[String], activatedAbilities: Map[String, Ability]) extends Card

// TODO: Try Monocle
// TODO: Should it generate commands/events ?
// TODO: Should use Pattern Matching
val forest = Land(
  "Forest", List.empty, Map(
    "tap" -> Ability(Tapping, (state, player) => state.focus(_.playerStates.index(player).manaPool.index(Color.red)).modify(_ + 1))
  ))
