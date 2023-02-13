package game

import akka.actor.typed.Behavior
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import cards.{Card, forest}
import game.*
import monocle.syntax.all.*

import java.util.UUID

object Engine {

  private val OPENING_HAND = 7
  private val MIN_DECK_SIZE = 40

  private val commandHandler: (State, Action) => ReplyEffect[Event, State] = { (state, command) =>
    state match {
      case EmptyState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(state => StatusReply.Success(state))
          case New(replyTo, decks: Map[String, List[Card]]) =>
            if decks.exists(_._2.length < MIN_DECK_SIZE) then
              Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"Deck invalid"))
            else
              Effect.persist(
                Created(scala.util.Random.nextInt(2), decks) +: decks.keys.map(Drawn(OPENING_HAND, _)).toSeq
              ).thenReply(replyTo)(state => StatusReply.Success(state))

          case _ => Effect.noReply
        }

      case state: InProgressState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(state => StatusReply.Success(state))
          case Mulligan(replyTo, player) =>
            Effect.persist(Discarded(None, player), Drawn(OPENING_HAND, player)).thenReply(replyTo)(state => StatusReply.Success(state))

          case Draw(replyTo, player, count) =>
            state.players(player).library match {
              // TODO: Should loose then
              case _ :: _ => Effect.persist(Drawn(count, player)).thenReply(replyTo)(state => StatusReply.Success(state))
              case Nil => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"No more cards in the Library"))
            }

          case Discard(replyTo, player, id) =>
            Effect.persist(Discarded(id, player)).thenReply(replyTo)(state => StatusReply.Success(state))

          // TODO: Check for current tapStatus
          // TODO: Check for Cost
          case Tap(replyTo, player, name) =>
            state.battleField.filter(_._2.owner == player).find(_._2.card.name == name) match {
              case Some(id, _) => Effect.persist(Tapped(id, player)).thenReply(replyTo)(state => StatusReply.Success(state))
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"No $name found"))
            }
          case _ => Effect.noReply
        }
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    state match {
      case EmptyState =>
        event match {
          case Created(die: Int, decks: Map[String, List[Card]]) =>
            InProgressState(
              decks.keys.toIndexedSeq(die),
              decks.map((name, deck) => (name, Player(deck)))
            )
          case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
        }

      case state: InProgressState =>
        event match {
          case Drawn(count, player) =>
            state.focus(_.players.index(player).hand).modify(_ + (2 -> state.players(player).library.head))
              .focus(_.players.index(player).library).modify(_.tail)

          // TODO: Handle id
          case Discarded(id, player) =>
            state.focus(_.players.index(player).graveyard).modify(_ + state.players(player).hand.head)
              .focus(_.players.index(player).hand).replace(Map.empty)

          case Tapped(id, player) =>
            // TODO: Manage to Tap before
            state.battleField(id).card.activatedAbilities("tap").effect(state, player)
              .focus(_.battleField.index(id).status).replace(Status.Tapped)
            
          case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
        }
    }
  }

  def apply(id: UUID): Behavior[Action] =
    EventSourcedBehavior.withEnforcedReplies[Action, Event, State](
      persistenceId = PersistenceId.ofUniqueId(id.toString),
      emptyState = EmptyState,
      commandHandler = commandHandler,
      eventHandler = eventHandler,
    )
}