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
  
  private val OPENING_HAND = 1

  private val commandHandler: (State, Action) => ReplyEffect[Event, State] = { (state, command) =>
    command match {
      // TODO: Check deck size
      case Start(replyTo) =>
        state.phase match {
          case None => Effect.persist(state.playerStates.keys.map(Drawn(OPENING_HAND, _)).toSeq).thenReply(replyTo)(state => StatusReply.Success(state))
          case _ => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"Game already in progress"))
        }

      case Draw(replyTo, player, count) =>
        state.playerStates(player).library match {
          // TODO: Should loose then
          case _ :: _ => Effect.persist(Drawn(count, player)).thenReply(replyTo)(state => StatusReply.Success(state))
          case Nil => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"No more cards in the Library"))
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

  def apply(id: UUID, decks: Map[String, List[Card]]): Behavior[Action] =
    EventSourcedBehavior.withEnforcedReplies[Action, Event, State](
      persistenceId = PersistenceId.ofUniqueId(id.toString),
      emptyState = State(
        id,
        decks.keys.toIndexedSeq(scala.util.Random.nextInt(2)),
        decks.map((name, deck) => (name, PlayerState(deck)))
      ),
      commandHandler = commandHandler,
      eventHandler = eventHandler,
    )
}