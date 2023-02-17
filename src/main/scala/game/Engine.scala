package game

import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import cards.*
import cards.mana.*
import cards.types.*
import game.*
import monocle.syntax.all.*

import java.util.UUID
import scala.collection.MapView
import scala.util.{Failure, Success, Try}

object Engine {

  private val OPENING_HAND = 7

  // Should there be a context object ?
  // TODO: Have a wrapper to fetch Target once we have "from" parameter
  private def checkPriority(replyTo: ActorRef[StatusReply[State]], state: BoardState, player: PlayerId)(block: => ReplyEffect[Event, State]): ReplyEffect[Event, State] =
    if state.priority == player then
      block
    else
      Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"$player does not have priority"))

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

      case state: BoardState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(state => StatusReply.Success(state))

          case PlayLand(replyTo, player, target) => checkPriority(replyTo, state, player) {
            state.players(player).hand.get(target) match {
              case Some(land: Land) => land.checkConditions(state, player) match {
                case Success(_) => Effect.persist(LandPlayed(target, player)).thenReply(replyTo)(state => StatusReply.Success(state))
                case Failure(message) => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(message))
              }
              case Some(_) => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target is not a land"))
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }

          case Next(replyTo, player, times: Option[Int]) => checkPriority(replyTo, state, player) {
            // TODO: This should be a Step condition
            if state.stack.isEmpty then
              // Move phase
              Effect.persist(
                (1 to times.getOrElse(1)).foldLeft((state.currentStep, List.empty[Event])) { case ((phase, events), _) =>
                  val nextPhase = phase.next()
                  (nextPhase, events ++ (Moved(nextPhase) +: nextPhase.turnBasedActions(state, player)))
                }._2
              ).thenReply(replyTo)(state => StatusReply.Success(state))
            else
              // Resolve the stack
              // TODO: This should probably be a trigger loop for each event
              Effect.persist(state.stack.flatMap { case (id, spell) => spell.card match {
                case _: Creature => List(EnteredTheBattlefield(id))
                case _ => List()
              }}.toList :+ PriorityPassed(state.playersTurn)).thenReply(replyTo)(state => StatusReply.Success(state))
          }

          // TODO: Only none-Mana Abilities go on the Stack
          case Activate(replyTo, player, target, abilityId) => checkPriority(replyTo, state, player) { // Player Wrapper
            state.battleField.filter(_._2.owner == player).get(target) match {
              case Some(spell) =>
                spell.card.activatedAbilities.get(abilityId) match {
                  case Some(ability) =>
                    if ability.cost.canPay(spell) then
                      Effect.persist(ability.cost.pay(target, player) ++ ability.effect(state, player)).thenReply(replyTo)(state => StatusReply.Success(state))
                    else
                      Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Cannot pay the cost"))
                  case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Abilities not found"))
                }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }

          case Cast(replyTo, player, target) => checkPriority(replyTo, state, player) { // Player Wrapper
            state.players(player).hand.get(target) match {
              // TODO: There can be alternative costs also
              // TODO: ETB triggers
              case Some(card) => card.checkConditions(state, player) match {
                case Success(_) => Effect.persist(card.cost.pay(target, player) ++ List(Stacked(target, player), PriorityPassed(state.nextPriority))).thenReply(replyTo)(state => StatusReply.Success(state))
                case Failure(message) => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(message))
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
          // TODO: Implement Mulligan
        }
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    state match {
      case EmptyState =>
        event match {
          case Created(die: Int, players: Map[String, Deck]) =>
            val startingUser = players.keys.toIndexedSeq(die)

            BoardState(
              startingUser,
              startingUser,
              players.map((name, deck) => (name, PlayerState(deck.cards)))
            )
          case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
        }

      case state: BoardState =>
        event match {
          case Moved(phase) => state.focus(_.currentStep).replace(phase)
          case TurnStateCleaned => state.players.keys.foldLeft(state) { (state, player) =>
            state.focus(_.players.index(player).turn).replace(TurnState())
          }
          case TurnEnded =>
            state.focus(_.playersTurn).replace(state.nextPlayer)
              .focus(_.priority).replace(state.nextPlayer)

          case ManaPoolEmptied => state.players.keys.foldLeft(state) { (state, player) =>
            state.focus(_.players.index(player).manaPool).replace(ManaPool.empty())
          }
          case ManaAdded(mana, player) =>
            state.focus(_.players.index(player).manaPool).modify(_ ++ mana)
          case ManaPaid(manaCost, player) =>
            state.focus(_.players.index(player).manaPool).modify(mp => (mp - manaCost).get)

          case Shuffled(order, player) =>
            val shuffled = order.zip(state.players(player).library).sortBy(_._1).map(_._2)
            state.focus(_.players.index(player).library).replace(shuffled)

          case Stacked(target, player) =>
            val spell = Spell(state.players(player).hand(target), player, player)
            state.focus(_.stack).modify(_ + (target -> spell))
              .focus(_.players.index(player).hand).modify(_.removed(target))

          case PriorityPassed(toPlayer) => state.focus(_.priority).replace(toPlayer)

          case EnteredTheBattlefield(target) =>
            val spell = state.stack(target)
            state.focus(_.battleField).modify(_ + (target -> Permanent(spell.card, spell.owner, spell.controller)))
              .focus(_.stack).modify(_.removed(target))

          // TODO: Oh no, target can come from the hand or the stack (Or other zones ?)
          case LandPlayed(target, player) =>
            val land = Permanent(state.players(player).hand(target), player, player)
            state.focus(_.battleField).modify(_ + (target -> land))
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