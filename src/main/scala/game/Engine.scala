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
  private val MAX_HAND_SIZE = 7

  // Should there be a context object ?
  // TODO: Have a wrapper to fetch Target once we have "from" parameter
  private def checkPriority(replyTo: ActorRef[StatusReply[State]], state: BoardState, player: PlayerId)(block: => ReplyEffect[Event, State]): ReplyEffect[Event, State] =
    if state.priority == player then
      block
    else
      Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"$player does not have priority"))

  private def checkStep(replyTo: ActorRef[StatusReply[State]], state: BoardState, step: Step)(block: => ReplyEffect[Event, State]): ReplyEffect[Event, State] =
    if state.currentStep == step then
      block
    else
      Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"Action only available during ${step.toString}"))

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
          // TODO: Put Abilities on the Stack
          // TODO: Only non-Mana Abilities go on the Stack
          case Activate(replyTo, player, target, abilityId) => checkPriority(replyTo, state, player) { // Player Wrapper
            state.battleField.filter(_._2.owner == player).get(target) match {
              case Some(permanent) =>
                permanent.card.activatedAbilities.get(abilityId) match {
                  case Some(ability) =>
                    if ability.cost.canPay(permanent) then
                      Effect.persist(ability.cost.pay(target, player) ++ ability.effect(state, player)).thenReply(replyTo)(state => StatusReply.Success(state))
                    else
                      Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Cannot pay the cost"))
                  case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Abilities not found"))
                }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }

          case DeclareAttacker(replyTo, player, target) => checkPriority(replyTo, state, player) { checkStep(replyTo, state, Step.declareAttackers) {
            state.potentialAttackers(player).get(target) match {
              case Some(_) =>
                Effect.persist(List(Tapped(target), AttackerDeclared(target))).thenReply(replyTo)(state => StatusReply.Success(state))
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }}

          case Next(replyTo, player, skip: Option[Boolean]) => checkPriority(replyTo, state, player) {
            val stepsCountUntilEnd = skip.filter(_ == true).map(_ => Step.values.length - Step.values.indexOf(state.currentStep)).getOrElse(1)

            if state.stack.isEmpty then
              Try {
                (1 until stepsCountUntilEnd).foldLeft((state.currentStep, List.empty[Event])) { case ((phase, events), _) =>
                  val nextPhase = phase.next()
                  (nextPhase, events ++ nextPhase.turnBasedActions(player))
                }._2
              } match {
                case Success(events) => Effect.persist(events).thenReply(replyTo)(state => StatusReply.Success(state))
                case Failure(ex) => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(ex.getLocalizedMessage))
              }
            else
            // Resolve the stack
            // TODO: This should probably be a trigger loop for each event
              Effect.persist(state.stack.toList.reverse.flatMap { case (id, spell) => spell.card match {
                case _: Creature => List(EnteredTheBattlefield(id))
                case _ => List()
              }
              // TODO: Careful ! That will not work during DeclareBlockers step where opponent
              } :+ PriorityPassed(state.currentPlayer)).thenReply(replyTo)(state => StatusReply.Success(state))
          }
          case Discard(replyTo, player, target) => checkPriority(replyTo, state, player) { checkStep(replyTo, state, Step.cleanup) {
            state.players(player).hand.get(target) match {
              case Some(_) =>
                state.players(player).hand match {
                  case hand if hand.size > MAX_HAND_SIZE =>
                    Effect.persist(Discarded(target, player)).thenReply(replyTo)(state => StatusReply.Success(state))
                  case hand if hand.size <= MAX_HAND_SIZE =>
                    Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Hand size does not exceed maximum, you can EndTurn"))
                }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }}
          case EndTurn(replyTo, player) => checkPriority(replyTo, state, player) { checkStep(replyTo, state, Step.cleanup) {
            state.players(player).hand match {
              case hand if hand.size <= MAX_HAND_SIZE =>
                Effect.persist(Step.unTap.turnBasedActions(player)).thenReply(replyTo)(state => StatusReply.Success(state))
              case hand if hand.size > MAX_HAND_SIZE =>
                Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Maximum hand size exceeded, discard down to 7"))
            }
          }}

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
          case Shuffled(order, player) =>
            val shuffled = order.zip(state.players(player).library).sortBy(_._1).map(_._2)
            state.focus(_.players.index(player).library).replace(shuffled)

          case PriorityPassed(toPlayer) => state.focus(_.priority).replace(toPlayer)
          case MovedToStep(phase) =>
            state.players.keys.foldLeft(state) { (state, player) =>
              state.focus(_.players.index(player).manaPool).replace(ManaPool.empty())
            }
              .focus(_.currentStep).replace(phase).focus(_.priority).replace(state.currentPlayer)

          case CombatEnded => state.focus(_.combat).replace(CombatState())
          case TurnEnded =>
            state.players.keys.foldLeft(state) { (state, player) =>
              state.focus(_.players.index(player).landsToPlay).replace(1)
            }
              .focus(_.currentPlayer).replace(state.nextPlayer)
              .focus(_.priority).replace(state.nextPlayer)
              .focus(_.battleField).modify(_.map(p => (p._1, p._2.copy(firstTurn = false))))

          case ManaAdded(mana, player) => state.focus(_.players.index(player).manaPool).modify(_ ++ mana)
          case ManaPaid(manaCost, player) => state.focus(_.players.index(player).manaPool).modify(mp => (mp - manaCost).get)

          case Stacked(target, player) =>
            val spell = Spell(state.players(player).hand(target), player, player)
            state.focus(_.stack).modify(_ + (target -> spell))
              .focus(_.players.index(player).hand).modify(_.removed(target))

          // TODO: Oh no, target can come from the hand or the stack (Or other zones ?)
          case LandPlayed(target, player) =>
            state.players(player).hand.get(target) match {
              case Some(land: Land) =>
                state.focus(_.battleField).modify(_ + (target -> Permanent(land, player, player)))
                  .focus(_.players.index(player).hand).modify(_.removed(target))
                  .focus(_.players.index(player).landsToPlay).modify(_ - 1)
              case _ => state
            }

          case EnteredTheBattlefield(target) =>
            state.stack.get(target) match {
              case Some(Spell(permanentCard: PermanentCard, owner, controller)) =>
                state.focus(_.battleField).modify(_ + (target -> Permanent(permanentCard, owner, controller)))
                  .focus(_.stack).modify(_.removed(target))
              case _ => state
            }

          case AttackerDeclared(target) =>
            state.battleField.get(target) match {
              case Some(permanent) => state.focus(_.combat.attackers).modify(_ + (target -> permanent))
              case None => state
            }

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

          case Tapped(target) => state.focus(_.battleField.index(target)).modify(_.tap)
          case Untapped => state.focus(_.battleField).modify(_.map(p => (p._1, p._2.unTap)))

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