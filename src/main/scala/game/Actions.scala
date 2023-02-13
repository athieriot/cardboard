package game

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import cards.*

sealed trait Action {
  val replyTo: ActorRef[StatusReply[State]]
}

final case class Recover(replyTo: ActorRef[StatusReply[State]]) extends Action
final case class New(replyTo: ActorRef[StatusReply[State]], players: Map[String, Deck]) extends Action

final case class Ready(replyTo: ActorRef[StatusReply[State]]) extends Action

final case class Draw(replyTo: ActorRef[StatusReply[State]], player: String, count: Int) extends Action
final case class Discard(replyTo: ActorRef[StatusReply[State]], player: String, target: Option[CardId]) extends Action

// TODO: Use Card id so we can target specific instance of same card
final case class Tap(replyTo: ActorRef[StatusReply[State]], player: String, target: CardId) extends Action