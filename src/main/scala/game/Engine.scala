package game

import akka.actor.typed.Behavior
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import cards.*
import game.*
import monocle.syntax.all.*

import java.util.UUID
import scala.util.{Failure, Success}

object Engine {

  private val OPENING_HAND = 7

  // TODO: Check if player is allowed to play (Wrapper)
  private val commandHandler: (State, Action) => ReplyEffect[Event, State] = { (state, command) =>
    state match {
      case EmptyState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(StatusReply.Success(_))
          case New(replyTo, players: Map[String, Deck]) =>
            if players.exists(!_._2.isValid) then
              Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"Deck invalid"))
            else
              def randomOrder(n: Int) = scala.util.Random.shuffle(1 to n*2).toList

              // TODO: How to orchestrate multiple effects and triggers !??
              Effect.persist(
                List(Created(scala.util.Random.nextInt(players.size), players))
                  ++ players.map { case (player, deck) => Shuffled(randomOrder(deck.cards.size), player) }
                  ++ players.keys.map(Drawn(OPENING_HAND, _))
              ).thenReply(replyTo)(state => StatusReply.Success(state))
          case _ => Effect.noReply
        }

      case state: InProgressState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(state => StatusReply.Success(state))

          case PlayLand(replyTo, player, target) =>
            state.landPlayCheck(player, target) match {
              case Success(_) => Effect.persist(LandPlayed(target, player)).thenReply(replyTo)(state => StatusReply.Success(state))
              case Failure(message) => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(message))
            }

          // TODO: Should we add events or persist first ?
          case Pass(replyTo, _) =>
            Effect.persist(Moved(state.phase.next()) +: state.phase.next().turnBasedActions())
              .thenReply(replyTo)(state => StatusReply.Success(state))

          case Discard(replyTo, player, target) =>
            if state.players(player).hand.contains(target) then
              Effect.persist(Discarded(target, player)).thenReply(replyTo)(state => StatusReply.Success(state))
            else
              Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
              
          // TODO: Tap should be "Use" instead and target an ability with a cost
          // TODO: Check for current tapStatus
          // TODO: Check for Cost
          case Tap(replyTo, player, target) =>
            state.battleField.filter(_._2.owner == player).get(target) match {
              case Some(_) => Effect.persist(Tapped(target, player)).thenReply(replyTo)(state => StatusReply.Success(state))
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"No target found"))
            }
          case _ => Effect.noReply

          // TODO: When pass priority, check state based actions
          // TODO: Implement priority/Mulligan
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
          case ManaPoolEmptied => state.players.keys.foldLeft(state) { (state, player) =>
            state.focus(_.players.index(player).manaPool).modify(_.map(p => (p._1, 0)))
          }

          case Shuffled(order, player) =>
            val shuffled = order.zip(state.players(player).library).sortBy(_._1).map(_._2)
            state.focus(_.players.index(player).library).replace(shuffled)

          case LandPlayed(target, player) =>
            val instance = Instance(state.players(player).hand(target), player, player)
            state.focus(_.battleField).modify(_ + (target -> instance))
              .focus(_.players.index(player).hand).modify(_.removed(target))
              .focus(_.players.index(player).turn.landsToPlay).modify(_ - 1)

          // TODO: Extract some of those focus
          case Drawn(amount, player) =>
            state.focus(_.players.index(player).hand).modify(_ ++ state.players(player).library.take(amount).zipWithIndex.map { case (card, i) => state.highestId + i -> card })
              .focus(_.players.index(player).library).modify(_.drop(amount))
              .focus(_.highestId).modify(_ + amount)

          case Discarded(target, player) =>
            if state.players(player).hand.contains(target) then
              state.focus (_.players.index (player).graveyard).modify (_ + (target -> state.players (player).hand (target) ) )
                .focus (_.players.index (player).hand).modify (_.removed (target) )
            else 
              state

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