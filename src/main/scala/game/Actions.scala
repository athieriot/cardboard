package game

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply

sealed trait Action {
  val replyTo: ActorRef[StatusReply[State]]
}

//TODO: Initiate
final case class Start(replyTo: ActorRef[StatusReply[State]]) extends Action

final case class Draw(replyTo: ActorRef[StatusReply[State]], player: String, count: Int) extends Action

// TODO: Use Card id so we can target specific instance of same card
final case class Tap(replyTo: ActorRef[StatusReply[State]], player: String, target: String) extends Action