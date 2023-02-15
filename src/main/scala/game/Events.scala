package game

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import cards.*
import game.*

/**
 * EventSourcing Behavior Commands
 */
sealed trait Action {
  val replyTo: ActorRef[StatusReply[State]]
}

final case class Recover(replyTo: ActorRef[StatusReply[State]]) extends Action
final case class New(replyTo: ActorRef[StatusReply[State]], players: Map[String, Deck]) extends Action

final case class Pass(replyTo: ActorRef[StatusReply[State]], player: PlayerId) extends Action
final case class PlayLand(replyTo: ActorRef[StatusReply[State]], player: PlayerId, target: CardId) extends Action
final case class Use(replyTo: ActorRef[StatusReply[State]], player: PlayerId, target: CardId, abilityId: Int) extends Action

final case class Discard(replyTo: ActorRef[StatusReply[State]], player: PlayerId, target: CardId) extends Action

/**
 * EventSourcing Behavior Events
 */
sealed trait Event

// TODO: Mulligan ?
final case class Created(die: Int, players: Map[String, Deck]) extends Event

sealed trait StateBaseEvent extends Event
final case class Moved(phase: Phase) extends StateBaseEvent
case object PlayerSwap extends StateBaseEvent
case object ManaPoolEmptied extends StateBaseEvent
case object Untapped extends StateBaseEvent

final case class LandPlayed(target: CardId, player: PlayerId) extends Event
final case class ManaAdded(mana: Map[Color, Int], player: PlayerId) extends Event

final case class Drawn(amount: Int, player: PlayerId) extends Event
final case class Shuffled(order: List[Int], player: PlayerId) extends Event
final case class Discarded(target: CardId, player: PlayerId) extends Event

final case class Tapped(target: CardId) extends Event