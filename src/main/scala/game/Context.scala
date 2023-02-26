package game

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
import game.mechanics.Step
import game.mechanics.Triggers.triggersHandler

import scala.util.{Failure, Success, Try}

case class Context(replyTo: ActorRef[StatusReply[State]], state: BoardState, player: PlayerId, args: List[Arg[_]])

private def parseContext(replyTo: ActorRef[StatusReply[State]], state: BoardState, player: PlayerId, args: List[Arg[_]] = List.empty)(block: Context => ReplyEffect[Event, State]): ReplyEffect[Event, State] =
  block(Context(replyTo, state, player, args))

// TODO: Have a wrapper to fetch card from id once we have "from" parameter
private def checkPriority(ctx: Context)(block: => ReplyEffect[Event, State]): ReplyEffect[Event, State] =
  if ctx.state.priority == ctx.player then
    block
  else
    Effect.none.thenReply(ctx.replyTo)(_ => StatusReply.Error(s"${ctx.player} does not have priority"))

private def checkStep(ctx: Context, step: Step)(block: => ReplyEffect[Event, State]): ReplyEffect[Event, State] =
  if ctx.state.currentStep == step then
    block
  else
    Effect.none.thenReply(ctx.replyTo)(_ => StatusReply.Error(s"Action only available during ${step.toString}"))

private def persistEvents(ctx: Context)(block: => Try[List[Event]]): ReplyEffect[Event, State] = {
  block match {
    case Success(events) => Effect.persist(triggersHandler(ctx.state, events)).thenReply(ctx.replyTo)(state => StatusReply.Success(state))
    case Failure(message) => Effect.none.thenReply(ctx.replyTo)(_ => StatusReply.Error(message))
  }
}