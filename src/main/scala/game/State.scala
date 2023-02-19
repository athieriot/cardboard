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

//enum TargetZone {
//  case hand, stack, graveyard, library, battleField, exile, command
//}
//case class TargetId(id: Int, owner: PlayerId, from: TargetZone)

enum Status {
  case Tapped, Untapped
}

case class Spell[T <: Card](
  card: T,
  owner: String,
  controller: String,
)

case class Permanent[T <: PermanentCard](
  card: T,
  owner: String,
  controller: String,
  status: Status = Untapped,
  firstTurn: Boolean = true
) {
  // TODO: Check for Haste
  def hasSummoningSickness: Boolean = card.isCreature && firstTurn

  def tap: Permanent[T] = {
    this.copy(status = Status.Tapped)
  }

  def unTap: Permanent[T] = {
    this.copy(status = Status.Untapped)
  }
}

case class PlayerState(
  library: List[Card] = List.empty,
  life: Int = 20,
  landsToPlay: Int = 1,
  manaPool: ManaPool = ManaPool.empty(),
  hand: Map[CardId, Card] = Map.empty,
  graveyard: Map[CardId, Card] = Map.empty,
  exile: Map[CardId, Card] = Map.empty,
  command: Option[Card] = None,
)
case class CombatState(
  attackers: Map[CardId, Permanent[PermanentCard]] = Map.empty,
  blockers: Map[CardId, Permanent[PermanentCard]] = Map.empty,
)

trait State
case object EmptyState extends State

// TODO: State = Number of Mulligan
// TODO: State = Number of Turns
case class BoardState(
  currentPlayer: String,
  priority: String,
  players: Map[String, PlayerState], // TODO: Change String to ID to be able to be a target ?
  currentStep: Step = Step.preCombatMain,
  stack: Map[CardId, Spell[Card]] = Map.empty,
  battleField: Map[CardId, Permanent[PermanentCard]] = Map.empty,
  combat: CombatState = CombatState(),
  highestId: CardId = 1,
) extends State {

  def nextPlayer: String = players.keys.sliding(2).find(_.head == currentPlayer).map(_.last).getOrElse(players.keys.head)
  def nextPriority: String = players.keys.sliding(2).find(_.head == priority).map(_.last).getOrElse(players.keys.head)

  // TODO: Will need to add more restrictions
  def potentialAttackers(player: PlayerId): Map[CardId, Permanent[PermanentCard]] = battleField
    .filter(_._2.controller == player)
    .filter(_._2.card.isCreature)
    .filter(_._2.status == Status.Untapped)
    .filterNot(_._2.hasSummoningSickness)
}