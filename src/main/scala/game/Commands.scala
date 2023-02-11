package game

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply

sealed trait Command

// TODO: Use Card id so we can target specific instance of same card
final case class Tap(replyTo: ActorRef[StatusReply[String]], name: String) extends Command