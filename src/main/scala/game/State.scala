package game

import cards.*

import java.util.UUID
import scala.collection.mutable

enum Status {
  case Tapped, Untapped
}

// TODO: Might be called a Spell then
case class Spell(card: Card, owner: String, controller: Int, status: Status, firstTurn: Boolean)

type CardId = Int

case class PlayerSide(
  library: List[Card] = List.empty,
  life: Int = 20,
  manaPool: Map[Color, Int] = Map.empty.withDefaultValue(0),
  hand: Map[CardId, Card] = Map.empty,
  graveyard: Map[CardId, Card] = Map.empty,
  exile: Map[CardId, Spell] = Map.empty,
  command: Option[Card] = None,
)

trait State
case object EmptyState extends State

// TODO: Unique ID between zones
// TODO: State = Number of Mulligan
// TODO: State = Number of Turns
case class InProgressState(
  // TODO: Should that be State types ?
  playersTurn: String,
  priority: String,
  players: Map[String, PlayerSide],
  phase: Phase = Phase.preCombatMain,
  stack: Map[CardId, Spell] = Map.empty,
  battleField: Map[CardId, Spell] = Map.empty,
  highestId: CardId = 1,
) extends State