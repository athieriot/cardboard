package game

import cards.*

import java.util.UUID
import scala.collection.mutable

// TODO: Need to define order/groups
enum Phase {
  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup
}
enum Status {
  case Tapped, Untapped
}

// TODO: Might be called a Spell then
case class CardInstance(card: Card, owner: String, controller: Int, status: Status, firstTurn: Boolean)

case class PlayerState(
  library: List[Card] = List.empty,
  life: Int = 20,
  manaPool: Map[Color, Int] = Map.empty.withDefaultValue(0),
  hand: Map[Int, Card] = Map.empty,
  graveyard: Map[Int, Card] = Map.empty,
  exile: Map[Int, CardInstance] = Map.empty,
  command: Option[Card] = None,
)

// TODO: State = Mulligan
case class State(
  id: UUID,
  // TODO: Should that be State types ?
  activePlayer: String,
  playerStates: Map[String, PlayerState],
  phase: Option[Phase] = None,
  stack: List[CardInstance] = List.empty,
  battleField: Map[Int, CardInstance] = Map.empty,
) {
  def render(state: State): Unit = {
    println(s"Game id: ${state.id}")
    println(s"Active Player: ${state.activePlayer}")
    playerStates.foreach { case (i, playerState) =>
      println(s"P$i - Life: ${playerState.life}")
      println(s"Library: ${playerState.library.length}")
      println(s"Hand: ${playerState.hand.map(p => s"${p._2.name}").mkString(", ")}")
      println("\n")
    }
  }
}