package game

import cards.*
import cards.mana.ManaPool
import cards.types.*
import game.*

import java.util.UUID
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import monocle.syntax.all.*

import scala.annotation.targetName

type CardId = Int
type PlayerId = String
type Target = CardId|PlayerId

//enum TargetZone {
//  case hand, stack, graveyard, library, battleField, exile, command
//}
//case class TargetId(id: Int, owner: PlayerId, from: TargetZone)

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

// TODO: Have a way to reassign order and amount of damages when more than one blocker
case class CombatZoneEntry(
  attacker: Permanent[PermanentCard],
  target: Target,
  blockers: Map[CardId, Permanent[PermanentCard]] = Map.empty,
)

trait State
case object EmptyState extends State
case class EndState(loser: String) extends State

// TODO: State = Number of Mulligan
// TODO: State = Number of Turns
case class BoardState(
   activePlayer: String,
   priority: String,
   players: Map[String, PlayerState],
   currentStep: Step = Step.preCombatMain,
   // TODO: Stack should have it's own class with Pop/Put/Resolve
   stack: Map[CardId, Spell[Card]] = Map.empty,
   battleField: Map[CardId, Permanent[PermanentCard]] = Map.empty,
   combatZone: Map[CardId, CombatZoneEntry] = Map.empty,
   highestId: CardId = 1,
) extends State {

  def nextPlayer: String = players.keys.sliding(2).find(_.head == activePlayer).map(_.last).getOrElse(players.keys.head)
  def nextPriority: String = players.keys.sliding(2).find(_.head == priority).map(_.last).getOrElse(players.keys.head)

  // TODO: Will need to add more restrictions based on static abilities
  def potentialAttackers(player: PlayerId): Map[CardId, Permanent[PermanentCard]] = battleField
    .filter(_._2.controller == player)
    .filter(_._2.card.isCreature)
    .filter(_._2.status == Status.Untapped)
    .filterNot(_._2.hasSummoningSickness)

  def potentialBlockers(player: PlayerId): Map[CardId, Permanent[PermanentCard]] = battleField
    .filter(_._2.controller == player)
    .filter(_._2.card.isCreature)
    .filter(_._2.status == Status.Untapped)
}

// TODO: One Map for Card repository
// TODO: One Map for Card Zones
enum Zone {
  case hand, stack, graveyard, library, battleField, exile, command
}