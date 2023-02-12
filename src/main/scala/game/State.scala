package game

import cards.*

// TODO: Need to define order
enum Phase {
  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup
}
enum Status {
  case Tapped, Untapped
}

case class CardInstance(card: Card, owner: Int, controller: Int, status: Status, firstTurn: Boolean)

case class PlayerState(
  library: List[Card],
  manaPool: Map[Color, Int] = Map.empty.withDefaultValue(0),
  hand: Map[Int, Card] = Map.empty,
  graveyard: Map[Int, Card] = Map.empty,
)

case class State(
  // TODO: Should that be State types ?
  activePlayer: Int,
  phase: Phase,
  playerStates: Map[Int, PlayerState],
  stack: List[CardInstance] = List.empty,
  battleField: Map[Int, CardInstance] = Map.empty,
  exile: Map[Int, CardInstance] = Map.empty,
)