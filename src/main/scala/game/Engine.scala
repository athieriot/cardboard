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
import scala.collection.immutable.ListMap
import scala.concurrent.Future
import scala.util.{Failure, Random, Success, Try}

object Engine {

  private val OPENING_HAND = 7
  private val MAX_HAND_SIZE = 7

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

          case PlayLand(replyTo, player, id, args) => parseContext(replyTo, state, player, args) { ctx => checkPriority(ctx) {
            persistEvents(ctx) {
              // TODO: One day implement play from Graveyard
              state.getCardFromZone(id, Hand(player)).filter(_.card.isLand) match {
                case Some(land) =>
                  land.checkConditions(ctx).map(_ =>  land.card.effects(id, ctx, land))
                case _ => Try(throw new RuntimeException("Land not found"))
              }
            }
          }}

          case Cast(replyTo, player, id, args) => parseContext(replyTo, state, player, args) { ctx => checkPriority(ctx) { // Player Wrapper
            persistEvents(ctx) {
              state.getCardFromZone(id, Hand(player)).filterNot(_.card.isLand) match {
                case Some(card) =>
                  card.checkConditions(ctx)
                    .flatMap(_ => card.payCost(id, ctx))
                    .map(costEvents => costEvents ++ List(Stacked(id, player, args), PriorityPassed(state.nextPriority)))
                case None => Try(throw new RuntimeException("Card not found"))
              }
            }
          }}

          case Activate(replyTo, player, id, abilityId, args) => parseContext(replyTo, state, player, args) { ctx => checkPriority(ctx) {
            persistEvents(ctx) {
              state.listCardsFromZone(Battlefield).filter(_._2.controller == player).get(id) match {
                case Some(permanent) => permanent.card.activatedAbilities.get(abilityId) match {
                  case Some(ability) =>
                    ability.checkConditions(ctx)
                      .flatMap(_ => ability.payCost(id, ctx, permanent))
                      .map(costEvents => costEvents ++ (if ability.manaAbility then
                        ability.effects(id, ctx, permanent)
                      else
                        List(StackedAbility(id, abilityId, player, ctx.args), PriorityPassed(state.nextPriority))
                      ))
                  case None => Try(throw new RuntimeException("Ability not found"))
                }
                case None => Try(throw new RuntimeException("Card not found"))
              }
            }
          }}

          // TODO: Have a round of priority after
          case DeclareAttacker(replyTo, player, id) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) { checkStep(ctx, Step.declareAttackers) {
            state.potentialAttackers(player).get(id) match {
              case Some(_) =>
                Effect.persist(triggersHandler(state, List(Tapped(id), AttackerDeclared(id)))).thenReply(replyTo)(state => StatusReply.Success(state))
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Card not found"))
            }
          }}}

          // TODO: Multiple blockers
          // TODO: Would not work for 4 Players
          case DeclareBlocker(replyTo, player, id, blocker) => parseContext(replyTo, state, player) { ctx => checkStep(ctx, Step.declareBlockers) {
            state.potentialBlockers(state.nextPlayer).get(blocker) match {
              case Some(_) =>
                state.combatZone.get(id) match {
                  case Some(_) => Effect.persist(triggersHandler(state, List(BlockerDeclared(id, blocker)))).thenReply(replyTo)(state => StatusReply.Success(state))
                  case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Attacker not found"))
                }
              case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error("Card not found"))
            }
          }}

          // TODO: Apparently there is a round of priority after declare attacker/blockers
          // TODO: Need to find a way to do that round of priority each step
          case Next(replyTo, player) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) {
            persistEvents(ctx) { Try {
              if state.stack.nonEmpty then
                throw new RuntimeException("Cannot go to next phase until the stack is empty")
              else
                List(
                  MovedToStep(state.currentStep.next()),
                  PriorityPassed(state.activePlayer)
                )
            }}
          }}
          case Resolve(replyTo, player) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) {
            persistEvents(ctx) {
              Try {
                // TODO: Should stop/print a message when a spell need interaction
                state.listCardsFromZone(Stack).toList.reverse match {
                  case Nil => throw new RuntimeException("Nothing to resolve. You can go to next phase")
                  case (id, spell) :: tail => spell.buildEffects(id, ctx) ++ (tail match {
                    case Nil => List(PriorityPassed(state.activePlayer))
                    case (_, nextSpell: Spell[_]) :: _ if nextSpell.controller == state.priority => List(PriorityPassed(state.nextPriority))
                    case _ => List()
                  })
                }
              }
            }
          }}

          case Discard(replyTo, player, id) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) { checkStep(ctx, Step.cleanup) {
            persistEvents(ctx) { Try {
              state.players(player).hand.get(id) match {
                case Some(_) =>
                  state.listCardsFromZone(Hand(player)) match {
                    case hand if hand.size > MAX_HAND_SIZE => List(Discarded(id, player))
                    case hand if hand.size <= MAX_HAND_SIZE => throw new RuntimeException("Hand size does not exceed maximum, you can EndTurn")
                  }
                case None => throw new RuntimeException("Card not found")
              }
            }}
          }}}
          case EndTurn(replyTo, player) => parseContext(replyTo, state, player) { ctx => checkPriority(ctx) { checkStep(ctx, Step.cleanup) {
            persistEvents(ctx) { Try {
              state.listCardsFromZone(Hand(player)) match {
                case hand if hand.size <= MAX_HAND_SIZE => List(TurnEnded, MovedToStep(Step.unTap), PriorityPassed(state.nextPlayer))
                case hand if hand.size > MAX_HAND_SIZE => throw new RuntimeException("Maximum hand size exceeded, discard down to 7")
              }
            }}
          }}}

          case _ => Effect.noReply
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
            val shuffled = ListMap(order.zip(state.listCardsFromZone(Library(player))).sortBy(_._1).map(_._2): _*)
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
          case Stacked(id, player, args) =>
            state.getCard(id) match {
              case None => state
              case Some(result) => result match {
                case (_, UnPlayed(_: Land, _)) => state
                case (zone, _) => state.moveCard(id, player, zone, Stack, args)
              }
            }
          case StackedAbility(id, abilityId, player, args) =>
            state.getCard(id) match {
              case None => state
              case Some(result) => result._2.card.activatedAbilities.get(abilityId) match {
                case Some(ability) =>
                  val abilityTokenId = s"$id$abilityId".toInt * 1000 // TODO: 1541000
                  val abilityTokenName = s"${result._2.card.name} - Ability $abilityId"
                  state.createToken(abilityTokenId, Stack, Spell(AbilityToken(abilityTokenName, ability), player, player, args))
                case None => state
              }
            }

          case LandPlayed(id, player) =>
            state.getCard(id) match {
              case None => state
              case Some(result) => result match {
                case (_, UnPlayed(_: Land, _)) =>
                  state.modifyPlayer(player, p => p.copy(landsToPlay = p.landsToPlay - 1))
                case (_, _) => state
              }
            }

          case EnteredTheBattlefield(id) =>
            state.getCard(id) match {
              case None => state
              case Some(result) => result match {
                case (zone, s) if s.card.isInstanceOf[PermanentCard] => state.moveCard(id, s.owner, zone, Battlefield)
                case (_, _) => state
              }
            }

          // TODO: Should move instances to the combat zone
          case AttackerDeclared(id) =>
            state.battleField.get(id) match {
              // TODO
              case Some(permanent) => state.focus(_.combatZone).modify(_ + (id -> CombatZoneEntry(permanent, state.nextPlayer)))
              case None => state
            }

            // TODO: Naming !
          case BlockerDeclared(id, blocker) =>
            // TODO
            (state.combatZone.get(id), state.battleField.get(blocker)) match {
              case (Some(_), Some(card)) => state.focus(_.combatZone.index(id).blockers).modify(_ + (blocker -> card))
              case _ => state
            }

          case Drawn(amount, player) =>
            val topXCards = state.listCardsFromZone(Library(player)).take(amount)
            state.moveCards(topXCards.keys.toList, player, Library(player), Hand(player))

          case DamageDealt(id, amount) =>
            id match {
              case cardId: CardId           => state.modifyCardFromZone(cardId, Battlefield, _.takeDamage(amount))
              case player: PlayerId         => state.modifyPlayer(player, _.takeDamage(amount))
            }
          case Discarded(id, player)        => state.moveCard(id, player, Hand(player), Graveyard(state.getCardOwner(id).getOrElse(player)))
          case Destroyed(id, player)        => state.moveCard(id, player, Battlefield, Graveyard(state.getCardOwner(id).getOrElse(player)))
          case PutIntoGraveyard(id, player) => state.moveCard(id, player, Stack, Graveyard(state.getCardOwner(id).getOrElse(player)))
          case Tapped(id)                   => state.modifyCardFromZone(id, Battlefield, _.tap)
          case Untapped                     => state.modifyAllCardsFromZone(Battlefield, _.unTap)

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