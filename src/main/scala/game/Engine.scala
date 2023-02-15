package game

import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import cards.*
import game.*
import monocle.syntax.all.*

import java.util.UUID
import scala.util.{Failure, Success, Try}

object Engine {

  private val OPENING_HAND = 7

  // Should there be a context object ?
  private def checkPriority(replyTo: ActorRef[StatusReply[State]], state: InProgressState, player: PlayerId)(block: => ReplyEffect[Event, State]): ReplyEffect[Event, State] =
    if state.priority == player then
      block
    else
      Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"$player does not have priority"))

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

          case PlayLand(replyTo, player, target) => checkPriority(replyTo, state, player) {
            state.landPlayCheck(player, target) match {
              case Success(_) => Effect.persist(LandPlayed(target, player)).thenReply(replyTo)(state => StatusReply.Success(state))
              case Failure(message) => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(message))
            }
          }

          // TODO: Should we add events or persist first ?
          // TODO: Check conditions for End step, like hand size ?
          case Pass(replyTo, player) => checkPriority(replyTo, state, player) {
            Effect.persist(Moved(state.phase.next()) +: state.phase.next().turnBasedActions(player))
              .thenReply(replyTo)(state => StatusReply.Success(state))
          }

          // TODO: Have a command to Read the text and abilities
          case Use(replyTo, player, target, abilityId) => checkPriority(replyTo, state, player) { // Player Wrapper
            state.battleField.filter(_._2.owner == player).get(target) match {
              case Some(instance) =>
                instance.card.activatedAbilities.get(abilityId) match {
                  case Some(ability) =>
                    if ability.cost.check(instance) then
                      Effect.persist(ability.cost.pay(target, player) ++ ability.effect(state, player)).thenReply(replyTo)(state => StatusReply.Success(state))
                    else
                      Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Cannot pay the cost"))
                  case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Abilities not found"))
                }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }

          case Discard(replyTo, player, target) => checkPriority(replyTo, state, player) {
            if state.players(player).hand.contains(target) then
              Effect.persist(Discarded(target, player)).thenReply(replyTo)(state => StatusReply.Success(state))
            else
              Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
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
            val startingUser = players.keys.toIndexedSeq(die)

            InProgressState(
              startingUser,
              startingUser,
              players.map((name, deck) => (name, PlayerState(deck.cards)))
            )
          case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
        }

      case state: InProgressState =>
        event match {
          case Moved(phase) => state.focus(_.phase).replace(phase)
          case PlayerSwap =>
            val nextPlayer = state.players.keys.sliding(2).find(_.head == state.playersTurn).map(_.last).getOrElse(state.players.keys.head)
            // TODO: Priority should be anoter Event
            state.focus(_.playersTurn).replace(nextPlayer).focus(_.priority).replace(nextPlayer)
            
          case ManaPoolEmptied => state.players.keys.foldLeft(state) { (state, player) =>
            state.focus(_.players.index(player).manaPool).modify(_.map(p => (p._1, 0)))
          }
          case ManaAdded(mana, player) =>
            state.focus(_.players.index(player).manaPool).modify(_.map(p => (p._1, p._2 + mana.getOrElse(p._1, 0))))

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
              state.focus(_.players.index(player).graveyard).modify(_ + (target -> state.players (player).hand(target)))
                .focus(_.players.index (player).hand).modify(_.removed (target))
            else
              state

          case Tapped(target) =>
            state.focus(_.battleField.index(target).status).replace(Status.Tapped)
          case Untapped =>
            state.focus(_.battleField).modify(_.map(p => (p._1, p._2.unTap)))

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