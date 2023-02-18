package game

import cards.*
import cards.mana.ManaPool
import cards.types.*
import game.Status.Untapped

import java.util.UUID
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import monocle.syntax.all.*

import scala.annotation.targetName

type CardId = Int // TODO: Should include "from"
type PlayerId = String

enum TargetZone {
  case hand, stack, graveyard, library, battleField, exile, command
}
case class TargetId(id: Int, owner: PlayerId, from: TargetZone)

enum Status {
  case Tapped, Untapped
}

trait Target
case class Spell(
  card: Card,
  owner: String,
  controller: String,
)

case class Permanent(
  card: Card,
  owner: String,
  controller: String,
  status: Status = Untapped,
  firstTurn: Boolean = true
) {
  def unTap: Permanent = {
    this.copy(status = Status.Untapped)
  }
}

case class PlayerState(
  library: List[Card] = List.empty,
  life: Int = 20,
  turn: TurnState = TurnState(),
  manaPool: ManaPool = ManaPool.empty(),
  hand: Map[CardId, Card] = Map.empty,
  graveyard: Map[CardId, Card] = Map.empty,
  exile: Map[CardId, Card] = Map.empty,
  command: Option[Card] = None,
)

// TODO: Put here cost when it's an interaction required ? Like discard
case class TurnState(
  landsToPlay: Int = 1,
)

trait State
case object EmptyState extends State

// TODO: State = Number of Mulligan
// TODO: State = Number of Turns
case class BoardState(
  playersTurn: String,
  priority: String,
  players: Map[String, PlayerState], // TODO: Change String to ID to be able to be a target ?
  currentStep: Step = Step.preCombatMain,
  stack: Map[CardId, Spell] = Map.empty,
  battleField: Map[CardId, Permanent] = Map.empty,
  highestId: CardId = 1,
) extends State {

  def nextPlayer: String = players.keys.sliding(2).find(_.head == playersTurn).map(_.last).getOrElse(players.keys.head)
  def nextPriority: String = players.keys.sliding(2).find(_.head == priority).map(_.last).getOrElse(players.keys.head)
}