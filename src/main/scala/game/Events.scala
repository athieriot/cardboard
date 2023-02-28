package game

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import cards.*
import game.*
import cards.mana.*
import game.mechanics.Step

/**
 * EventSourcing Behavior Commands
 */
sealed trait Action {
  val replyTo: ActorRef[StatusReply[State]]
}

// Game initialisation
final case class Recover(replyTo: ActorRef[StatusReply[State]]) extends Action
final case class New(replyTo: ActorRef[StatusReply[State]], players: Map[String, Deck]) extends Action

final case class PlayLand(replyTo: ActorRef[StatusReply[State]], player: PlayerId, id: CardId, args: List[Arg[_]]) extends Action
final case class Cast(replyTo: ActorRef[StatusReply[State]], player: PlayerId, id: CardId, args: List[Arg[_]]) extends Action
final case class Activate(replyTo: ActorRef[StatusReply[State]], player: PlayerId, id: CardId, abilityId: Int, args: List[Arg[_]]) extends Action

final case class DeclareAttacker(replyTo: ActorRef[StatusReply[State]], player: PlayerId, id: CardId) extends Action
final case class DeclareBlocker(replyTo: ActorRef[StatusReply[State]], player: PlayerId, id: CardId, blocker: CardId) extends Action

// TODO: Stop stack resolution and ask for interaction. Command "choose" ?
final case class Next(replyTo: ActorRef[StatusReply[State]], player: PlayerId) extends Action
final case class Resolve(replyTo: ActorRef[StatusReply[State]], player: PlayerId) extends Action
final case class EndTurn(replyTo: ActorRef[StatusReply[State]], player: PlayerId) extends Action

final case class Discard(replyTo: ActorRef[StatusReply[State]], player: PlayerId, id: CardId) extends Action

/**
 * EventSourcing Behavior Events
 */
sealed trait Event

// TODO: Mulligan ?
final case class GameCreated(die: Int, players: Map[String, Deck]) extends Event
final case class GameEnded(loser: String) extends Event

sealed trait TurnBaseEvent extends Event
final case class MovedToStep(phase: Step) extends TurnBaseEvent
final case class PriorityPassed(to: PlayerId) extends Event
case object TurnEnded extends TurnBaseEvent
case object Untapped extends TurnBaseEvent
case object CombatEnded extends TurnBaseEvent

final case class Stacked(id: CardId, player: PlayerId, args: List[Arg[_]]) extends Event
final case class StackedAbility(id: CardId, abilityId: Int, player: PlayerId, args: List[Arg[_]]) extends Event
final case class LandPlayed(id: CardId, player: PlayerId) extends Event
final case class EnteredTheBattlefield(id: CardId) extends Event
final case class PutIntoGraveyard(id: CardId, player: PlayerId) extends Event

final case class Tapped(id: CardId) extends Event
final case class ManaAdded(mana: Map[Color, Int], player: PlayerId) extends Event
final case class ManaPaid(cost: ManaCost, player: PlayerId) extends Event

sealed trait CombatEvent extends Event
final case class AttackerDeclared(attacker: CardId) extends CombatEvent
final case class BlockerDeclared(id: CardId, blocker: CardId) extends CombatEvent
final case class DamageDealt(target: TargetId, amount: Int) extends CombatEvent

final case class Destroyed(id: CardId, player: PlayerId) extends Event
final case class Drawn(amount: Int, player: PlayerId) extends Event
final case class Shuffled(order: List[Int], player: PlayerId) extends Event
final case class Discarded(id: CardId, player: PlayerId) extends Event
