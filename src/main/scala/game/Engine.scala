package game

import akka.actor.typed.Behavior
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import cards.{Card, forest}
import game.*
import monocle.syntax.all._

import java.util.UUID

object Engine {

  private val commandHandler: (State, Action) => ReplyEffect[Event, State] = { (state, command) =>
    command match {
      case Draw(replyTo, player, count) =>
        state.playerStates(player).library match {
          // TODO: Should loose then
          case Nil => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"No more cards in the Library"))
          case _ => Effect.persist(Drawn(count, player)).thenReply(replyTo)(state => StatusReply.Success(state))
        }

      // TODO: Check for current tapStatus
      // TODO: Check for Cost
      case Tap(replyTo, player, name) =>
        state.battleField.filter(_._2.owner == player).find(_._2.card.name == name) match {
          case Some(id, _) => Effect.persist(Tapped(id, player)).thenReply(replyTo)(state => StatusReply.Success(state))
          case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"No $name found"))
        }
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case Drawn(count, player) =>
        state.focus(_.playerStates.index(player).hand).modify(_ + (2 -> state.playerStates(player).library.head))
          .focus(_.playerStates.index(player).library).modify(_.tail)

      case Tapped(id, player) =>
        // TODO: Manage to Tap before
        state.battleField(id).card.activatedAbilities("tap").effect(state, player)
          .focus(_.battleField.index(id).status).replace(Status.Tapped)
    }
  }

  def apply(id: UUID, decks: Map[Int, List[Card]]): Behavior[Action] =
    EventSourcedBehavior.withEnforcedReplies[Action, Event, State](
      persistenceId = PersistenceId.ofUniqueId(id.toString),
      emptyState = State(0, Phase.draw, decks.map((id, deck) => (id, PlayerState(deck)))),
      commandHandler = commandHandler,
      eventHandler = eventHandler,
    )
}