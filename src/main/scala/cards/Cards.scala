package cards

import com.fasterxml.jackson.annotation.JsonIgnore
import game.{InProgressState, State}
import monocle.syntax.all.*

enum Color {
  case red, green, black, white, blue
}

enum Type {
  case land
}

trait Cost

case object Tapping extends Cost
case class ManaCost() extends Cost

case class Ability(cost: Cost, effect: (InProgressState, String) => InProgressState)

case class Card(
  name: String,
  `type`: Type,
  staticAbilities: List[String],
  @JsonIgnore activatedAbilities: Map[String, Ability],
)

// TODO: Try Monocle
// TODO: Should it generate commands/events ?
// TODO: Should use Pattern Matching
val forest = Card(
  "Forest", Type.land, List.empty, Map(
    "tap" -> Ability(Tapping, (state, player) => state.focus(_.players.index(player).manaPool.index(Color.red)).modify(_ + 1))
  ))
