package game

import akka.actor.typed.Behavior
import akka.pattern.StatusReply.{Success, Error}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import cards.*
import game.*
import monocle.syntax.all.*

import java.util.UUID

object Engine {

  private val OPENING_HAND = 7

  private val commandHandler: (State, Action) => ReplyEffect[Event, State] = { (state, command) =>
    state match {
      case EmptyState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(Success(_))
          case New(replyTo, players: Map[String, Deck]) =>
            if players.exists(!_._2.isValid) then
              Effect.none.thenReply(replyTo)(_ => Error(s"Deck invalid"))
            else
              def randomOrder(n: Int) = scala.util.Random.shuffle(1 to n*2).toList
              
              // TODO: How to orchestrate multiple effects and triggers !??
              Effect.persist(
                Seq(Created(scala.util.Random.nextInt(players.size), players))
                  ++ (players.map { case (player, deck) => Shuffled(randomOrder(deck.cards.size), player) }.toSeq
                  ++ players.keys.map(Drawn(OPENING_HAND, _)).toSeq)
              ).thenReply(replyTo)(state => Success(state))
          case _ => Effect.noReply
        }

      case state: InProgressState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(state => Success(state))
          case Draw(replyTo, player, count) =>
            state.players(player).library match {
              // TODO: Should loose then, but only in the event handler as loosing a world check ?
              case _ :: _ => Effect.persist(Drawn(count, player)).thenReply(replyTo)(state => Success(state))
              case Nil => Effect.none.thenReply(replyTo)(_ => Error(s"No more cards in the Library"))
            }

          case Discard(replyTo, player, target) =>
            // TODO: Check if in hand ?
            Effect.persist(Discarded(target, player)).thenReply(replyTo)(state => Success(state))

          // TODO: Check for current tapStatus
          // TODO: Check for Cost
          case Tap(replyTo, player, target) =>
            state.battleField.filter(_._2.owner == player).get(target) match {
              case Some(_) => Effect.persist(Tapped(target, player)).thenReply(replyTo)(state => Success(state))
              case None => Effect.none.thenReply(replyTo)(_ => Error(s"No target found"))
            }
          case _ => Effect.noReply
        }
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    state match {
      case EmptyState =>
        event match {
          case Created(die: Int, players: Map[String, Deck]) =>
            InProgressState(
              players.keys.toIndexedSeq(die),
              players.keys.toIndexedSeq(die),
              players.map((name, deck) => (name, PlayerSide(deck.cards)))
            )
          case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
        }
      case state: InProgressState =>
        event match {
          case Moved(phase) => state.focus(_.phase).replace(phase)
          case Shuffled(order, player) =>
            val shuffled = order.zip(state.players(player).library).sortBy(_._1).map(_._2)
            state.focus(_.players.index(player).library).replace(shuffled)

          // TODO: Should we draw first ?
          case Drawn(count, player) =>
            state.focus(_.players.index(player).hand).modify(_ ++ state.players(player).library.take(count).zipWithIndex.map { case (card, i) => state.highestId + i -> card })
              .focus(_.players.index(player).library).modify(_.drop(count))
              .focus(_.highestId).modify(_ + count)

          case Discarded(target, player) =>
            target match {
              case None =>
                state.focus(_.players.index(player).graveyard).modify(_ ++ state.players(player).hand)
                  .focus(_.players.index(player).hand).replace(Map.empty)
              case Some(id) if state.players(player).hand.contains(id) =>
                state.focus(_.players.index(player).graveyard).modify(_ + (id -> state.players(player).hand(id)))
                  .focus(_.players.index(player).hand).modify(_.removed(id))
              case Some(_) => state
            }

          case Tapped(target, player) =>
            // TODO: Tap should trigger events instead
            state.battleField(target).card.activatedAbilities("tap").effect(state, player)
              .focus(_.battleField.index(target).status).replace(Status.Tapped)

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