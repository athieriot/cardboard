package game

import cards.*
import game.Status.Untapped

import java.util.UUID
import scala.collection.mutable
import scala.util.{Failure, Try}

enum Status {
  case Tapped, Untapped
}

// TODO: Might be called a Spell ? Except for Lands
case class Instance(
  card: Card,
  owner: String,
  controller: String,
  status: Status = Untapped,
  firstTurn: Boolean = true
)

type CardId = Int

case class PlayerSide(
  library: List[Card] = List.empty,
  life: Int = 20,
  turn: TurnState = TurnState(),
  // TODO: Mana pool is lost by phase, not turn
  manaPool: Map[Color, Int] = Color.values.map((_, 0)).toMap,
  hand: Map[CardId, Card] = Map.empty,
  graveyard: Map[CardId, Card] = Map.empty,
  exile: Map[CardId, Card] = Map.empty,
  command: Option[Card] = None,
)

case class TurnState(
  landsToPlay: Int = 1,
)

trait State
case object EmptyState extends State

// TODO: State = Number of Mulligan
// TODO: State = Number of Turns
case class InProgressState(
  // TODO: Should that be State types ?
  playersTurn: String,
  priority: String,
  players: Map[String, PlayerSide],
  phase: Phase = Phase.preCombatMain,
  stack: Map[CardId, Instance] = Map.empty,
  battleField: Map[CardId, Instance] = Map.empty,
  highestId: CardId = 1,
) extends State {

  def landPlayCheck(player: String, target: CardId): Try[Unit] = Try {
    if !this.players(player).hand.get(target).exists(_.isInstanceOf[LandType]) then
      throw new RuntimeException("Target is not a Land")
    else if this.playersTurn != player then
      throw new RuntimeException("You can only play lands during your turn")
    else if !this.phase.isMain then
      throw new RuntimeException("You can only play lands during a main phase")
    else if this.players(player).turn.landsToPlay <= 0 then
      throw new RuntimeException("You already played a land this turn")
  }
}