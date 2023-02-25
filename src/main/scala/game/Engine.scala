package game

import akka.actor.typed.{ActorRef, Behavior}
import akka.dispatch.Futures
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior, ReplyEffect}
import cards.*
import cards.mana.*
import cards.types.*
import com.typesafe.scalalogging.LazyLogging
import game.*
import game.mechanics.*
import game.mechanics.Triggers.triggersHandler
import monocle.AppliedOptional
import monocle.syntax.all.*

import java.util.UUID
import scala.collection.MapView
import scala.concurrent.Future
import scala.util.{Failure, Random, Success, Try}

object Engine {

  private val OPENING_HAND = 7
  private val MAX_HAND_SIZE = 7

  private case class Context(replyTo: ActorRef[StatusReply[State]], state: BoardState, player: PlayerId)

  private def parseContext(replyTo: ActorRef[StatusReply[State]], state: BoardState, player: PlayerId)(block: Context => ReplyEffect[Event, State]): ReplyEffect[Event, State] =
    block(Context(replyTo, state, player))

  // TODO: Have a wrapper to fetch Target once we have "from" parameter
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

  private val commandHandler: (State, Action) => ReplyEffect[Event, State] = { (state, command) =>
    state match {
      case EndState(_) => Effect.noReply
      case EmptyState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(StatusReply.Success(_))
          case New(replyTo, players: Map[String, Deck]) =>
            if players.exists(!_._2.isValid) then
              Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"Deck invalid"))
            else
              def randomOrder(n: Int) = Random.shuffle(1 to n * 2).toList

              Effect.persist(
                List(GameCreated(Random.nextInt(players.size), players))
                  ++ players.map { case (player, deck) => Shuffled(randomOrder(deck.cards.size), player) }
                  ++ players.keys.map(Drawn(OPENING_HAND, _))
              ).thenReply(replyTo)(state => StatusReply.Success(state))
          case _ => Effect.noReply
        }

      case state: BoardState =>
        command match {
          case Recover(replyTo) => Effect.none.thenReply(replyTo)(state => StatusReply.Success(state))

          case PlayLand(replyTo, player, id) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) {
            persistEvents(ctx) {
              state.getCard(id) match {
                case Some((zone: PlayerZone, UnPlayed(land: Land, _))) if zone.isOf(player) =>
                  land.checkConditions(state, player).map(_ => land.effects(id, state, player))
                case _ => Try(throw new RuntimeException("Land not found in your player zones"))
              }
            }
          }}

          case Cast(replyTo, player, target) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) { // Player Wrapper
            state.getCardFromZone(target, Hand(player)) match {
              // TODO: There can be alternative costs also
              case Some(cardState) => cardState.card.checkConditions(state, player) match {
                case Success(_) => Effect.persist(triggersHandler(state, cardState.card.cost.pay(target, player) ++ List(Stacked(target, player), PriorityPassed(state.nextPriority)))).thenReply(replyTo)(state => StatusReply.Success(state))
                case Failure(message) => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(message))
              }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }}
          // TODO: Only non-Mana Abilities go on the Stack
          case Activate(replyTo, player, target, abilityId) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) { // Player Wrapper
            state.battleField.filter(_._2.owner == player).get(target) match {
              case Some(permanent) =>
                permanent.card.activatedAbilities.get(abilityId) match {
                  case Some(ability) =>
                    if ability.cost.canPay(permanent) then
                      Effect.persist(triggersHandler(state, ability.cost.pay(target, player) ++ ability.effect(state, player))).thenReply(replyTo)(state => StatusReply.Success(state))
                    else
                      Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Cannot pay the cost"))
                  case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Abilities not found"))
                }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }}

          // TODO: Have a round of priority after
          case DeclareAttacker(replyTo, player, target) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) { checkStep(ctx, Step.declareAttackers) {
            state.potentialAttackers(player).get(target) match {
              case Some(_) =>
                Effect.persist(triggersHandler(state, List(Tapped(target), AttackerDeclared(target)))).thenReply(replyTo)(state => StatusReply.Success(state))
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }}}

          // TODO: Multiple blockers
          // TODO: Would not work for 4 Players
          case DeclareBlocker(replyTo, player, target, blocker) => parseContext(replyTo, state, player) { ctx => checkStep(ctx, Step.declareBlockers) {
            state.potentialBlockers(state.nextPlayer).get(blocker) match {
              case Some(_) =>
                state.combatZone.get(target) match {
                  case Some(_) => Effect.persist(triggersHandler(state, List(BlockerDeclared(target, blocker)))).thenReply(replyTo)(state => StatusReply.Success(state))
                  case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Attacker not found"))
                }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }}

          // TODO: Apparently there is a round of priority after declare attacker/blockers
          // TODO: Need to find a way to do that round of priority each step
          case Next(replyTo, player, skip: Option[Boolean]) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) {
            persistEvents(ctx) {
              if state.stack.isEmpty then
                Try {
                  val stepsCountUntilEnd = skip.filter(_ == true).map(_ => Step.values.length - Step.values.indexOf(state.currentStep) - 1).getOrElse(1)
                  val activePlayer = state.activePlayer
                  (1 to stepsCountUntilEnd).foldLeft((state: State, List.empty[Event])) { case ((stateAcc, events), _) =>
                    state match {
                      case state: BoardState =>
                        val newEvents = Try(state.currentStep.next()) match {
                          case Success(nextPhase) => triggersHandler(state, List(MovedToStep(nextPhase), PriorityPassed(activePlayer)))
                          case Failure(_) => List.empty
                        }
                        val newState = newEvents.foldLeft(stateAcc)(eventHandler(_, _))

                        (newState, events ++ newEvents)
                    }
                  }._2
                }
              else
                // Resolve the stack
                Try {
                  
                  // TODO: Take first one => Resolve => repeat
                  // TODO: Stop when stack is empty
                  // TODO: Real target is the one you have to choose on cast => Rename Target to Id then take an extra parameter
                  
                  // TODO: Should stop when a spell need interaction
                  // TODO: Actually Resolve would help to stop card by card
                  state.listCardsFromZone(Stack).toList.reverse
                    .flatMap { case (id, spell) => spell.card.effects(id, state, player) }
                    .concat(List(PriorityPassed(state.activePlayer)))
                }
            }
          }}
          case Discard(replyTo, player, target) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) { checkStep(ctx, Step.cleanup) {
            state.players(player).hand.get(target) match {
              case Some(_) =>
                state.players(player).hand match {
                  case hand if hand.size > MAX_HAND_SIZE =>
                    Effect.persist(triggersHandler(state, List(Discarded(target, player)))).thenReply(replyTo)(state => StatusReply.Success(state))
                  case hand if hand.size <= MAX_HAND_SIZE =>
                    Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Hand size does not exceed maximum, you can EndTurn"))
                }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Target not found"))
            }
          }}}
          case EndTurn(replyTo, player) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) { checkStep(ctx, Step.cleanup) {
            val nextPlayer = state.nextPlayer
            state.players(player).hand match {
              case hand if hand.size <= MAX_HAND_SIZE =>
                Effect.persist(triggersHandler(state, List(TurnEnded, MovedToStep(Step.unTap), PriorityPassed(nextPlayer)))).thenReply(replyTo)(state => StatusReply.Success(state))
              case hand if hand.size > MAX_HAND_SIZE =>
                Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Maximum hand size exceeded, discard down to 7"))
            }
          }}}

          case _ => Effect.noReply

          // TODO: When pass priority, check state based actions
          // TODO: Implement Mulligan
        }
    }
  }

  val eventHandler: (State, Event) => State = { (state, event) =>
    state match {
      case EmptyState =>
        event match {
          case GameCreated(die: Int, players: Map[String, Deck]) =>
            val startingUser = players.keys.toIndexedSeq(die)
            val decksWithIndex = players.zipWithIndex.toMap.map { case ((name, deck), playerIndex) =>
              (name, deck.cards.zipWithIndex.map(p => (p._2 + (playerIndex*100), UnPlayed(p._1, name))).toMap)
            }

            BoardState(
              startingUser,
              startingUser,
              decksWithIndex.view.mapValues(PlayerState(_)).toMap,
              decksWithIndex.flatMap(p => p._2.map(c => (c._1, Library(p._1).asInstanceOf[Zone[CardState[Card]]]))),
            )
          case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
        }

      case state: BoardState =>
        event match {
          case GameEnded(loser) => EndState(loser)

          case Shuffled(order, player) =>
            val shuffled = order.zip(state.listCardsFromZone(Library(player))).sortBy(_._1).map(_._2).toMap
            state.focusOnZone(Library(player)).replace(shuffled)

          case MovedToStep(phase) =>
            state.modifyPlayers(_.copy(manaPool = ManaPool.empty()))
              .focus(_.currentStep).replace(phase)

          case PriorityPassed(toPlayer) => state.focus(_.priority).replace(toPlayer)

          case CombatEnded => state.focus(_.combatZone).replace(Map.empty)

          case TurnEnded =>
            state.modifyPlayers(_.copy(landsToPlay = 1))
              .modifyAllCardsFromZone(Battlefield, _.copy(firstTurn = false, damages = 0))
              .focus(_.activePlayer).replace(state.nextPlayer)
              .focus(_.turnCount).modify(_ + 1)

          case ManaAdded(mana, player) => state.focusOnManaPool(player).modify(_ ++ mana)
          case ManaPaid(manaCost, player) => state.focusOnManaPool(player).modify(mp => (mp - manaCost).get)

          // TODO: Ability
          case Stacked(target, player) =>
            state.getCard(target) match {
              case Some((_, UnPlayed(_: Land, _))) => state
              case Some((zone, _)) => state.moveCard(target, player, zone, Stack)
              case None => state
            }

          case LandPlayed(target, player) =>
            state.getCard(target) match {
              case Some((zone, UnPlayed(_:Land, _))) =>
                state.moveCard(target, player, zone, Battlefield).modifyPlayer(player, p => p.copy(landsToPlay = p.landsToPlay - 1))
              case _ => state
            }

          case EnteredTheBattlefield(target) =>
            state.getCard(target) match {
              case Some((zone, s)) if s.card.isInstanceOf[PermanentCard] => state.moveCard(target, s.owner, zone, Battlefield)
              case _ => state
            }

          // TODO: Should move instances to the combat zone
          case AttackerDeclared(target) =>
            state.battleField.get(target) match {
              // TODO
              case Some(permanent) => state.focus(_.combatZone).modify(_ + (target -> CombatZoneEntry(permanent, state.nextPlayer)))
              case None => state
            }

            // TODO: Naming !
          case BlockerDeclared(target, blocker) =>
            // TODO
            (state.combatZone.get(target), state.battleField.get(blocker)) match {
              case (Some(_), Some(card)) => state.focus(_.combatZone.index(target).blockers).modify(_ + (blocker -> card))
              case _ => state
            }

          case Drawn(amount, player) =>
            val topXCards = state.listCardsFromZone(Library(player)).take(amount)
            state.moveCards(topXCards.keys.toList, player, Library(player), Hand(player))

          case DamageDealt(target, amount) =>
            target match {
              case id: CardId                   => state.modifyCardFromZone(id, Battlefield, _.takeDamage(amount))
              case player: PlayerId             => state.modifyPlayer(player, _.takeDamage(amount))
            }
          case Discarded(target, player)        => state.moveCard(target, player, Hand(player), Graveyard(state.getCardOwner(target).getOrElse(player)))
          case Destroyed(target, player)        => state.moveCard(target, player, Battlefield, Graveyard(state.getCardOwner(target).getOrElse(player)))
          case PutIntoGraveyard(target, player) => state.moveCard(target, player, Stack, Graveyard(state.getCardOwner(target).getOrElse(player)))
          case Tapped(target)                   => state.modifyCardFromZone(target, Battlefield, _.tap)
          case Untapped                         => state.modifyAllCardsFromZone(Battlefield, _.unTap)

          case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
        }

      case EndState(_) => state
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