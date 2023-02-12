package game

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply

sealed trait Action

// TODO: Use Card id so we can target specific instance of same card
final case class Tap(replyTo: ActorRef[StatusReply[String]], player: Int, target: String) extends Action