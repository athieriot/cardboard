package game.mechanics

import cards.mana.AbilityCost
import game.{BoardState, Event}

enum KeywordAbilities {
  case haste
}
// TODO: Could also have a "condition"
case class Ability(
  cost: AbilityCost,
  text: String,
  effect: (BoardState, String) => List[Event]
)
