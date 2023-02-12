package game

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply

sealed trait Action {
  val replyTo: ActorRef[StatusReply[State]]
  val player: Int
}

//TODO: Initiate

final case class Draw(replyTo: ActorRef[StatusReply[State]], player: Int, count: Int) extends Action

// TODO: Use Card id so we can target specific instance of same card
final case class Tap(replyTo: ActorRef[StatusReply[State]], player: Int, target: String) extends Action