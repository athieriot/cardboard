package helpers

import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import game.*
import game.EngineStandardGameTest.eventSourcedTestKit
import game.mana.*
import game.cards.*
import game.cards.types.*
import game.mechanics.*
import utest.assert

object GameChecks {

  def playLandCheck(id: CardId, player: PlayerId, cardsInHand: Int)(implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Action, Event, State]) = {
    val result = eventSourcedTestKit.runCommand[StatusReply[State]](PlayLand(_, player, id, List()))
    assert(result.reply.isSuccess)
    assert(result.events.diff(List(LandPlayed(id, player), EnteredTheBattlefield(id))).isEmpty)
    assert(result.stateOfType[BoardState].listCardsFromZone(Battlefield).exists(_._1 == id))
    assert(result.stateOfType[BoardState].listCardsFromZone(Hand(player)).size == cardsInHand)
  }

  def tapForManaCheck(id: CardId, abilityId: Int, player: PlayerId, color: Color, amount: Int)(implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Action, Event, State]) = {
    val result = eventSourcedTestKit.runCommand[StatusReply[State]](Activate(_, player, id, abilityId, List()))
    assert(result.reply.isSuccess)
    assert(result.events.diff(List(Tapped(id), ManaAdded(Map(color -> amount), player))).isEmpty)
    assert(result.stateOfType[BoardState].players(player).manaPool.pool(color) == amount)
    assert(result.stateOfType[BoardState].getCardFromZone(id, Battlefield).get.status == Status.Tapped)
  }

  def castPermanentCheck(id: CardId, player: PlayerId, nextPlayer: PlayerId, manaCost: ManaCost, cardsInHand: Int)(implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Action, Event, State]) = {
    val result = eventSourcedTestKit.runCommand[StatusReply[State]](c => Cast(c, player, id, List()))
    assert(result.reply.isSuccess)
    assert(result.events.diff(List(ManaPaid(manaCost, player), Stacked(id, player, List()), PriorityPassed(nextPlayer))).isEmpty)
    assert(result.stateOfType[BoardState].listCardsFromZone(Stack).exists(_._1 == id))
    assert(result.stateOfType[BoardState].listCardsFromZone(Hand(player)).size == cardsInHand)
  }

  def resolvePermanentCheck(id: CardId, player: PlayerId, nextPlayer: PlayerId, extraEvents: Event*)(implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Action, Event, State]) = {
    val result = eventSourcedTestKit.runCommand[StatusReply[State]](Resolve(_, player))
    assert(result.reply.isSuccess)
    assert(result.events.diff(List(EnteredTheBattlefield(id), PriorityPassed(nextPlayer))).isEmpty)
    assert(result.stateOfType[BoardState].listCardsFromZone(Stack).isEmpty)
    assert(result.stateOfType[BoardState].listCardsFromZone(Battlefield).exists(_._1 == id))
  }

  def declareAttackerCheck(id: CardId, player: PlayerId)(implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Action, Event, State]) = {
    val result = eventSourcedTestKit.runCommand[StatusReply[State]](DeclareAttacker(_, player, id))
    assert(result.reply.isSuccess)
    assert(result.stateOfType[BoardState].attackers.exists(_._1 == id))
  }

  def moveToStepCheck(player: PlayerId, step: Step, extraEvents: Event*)(implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Action, Event, State]) = {
    val result = eventSourcedTestKit.runCommand[StatusReply[State]](Next(_, player))
    assert(result.reply.isSuccess)
    assert(result.events.diff(List(MovedToStep(step), PriorityPassed(player)) ++ extraEvents.toList).isEmpty)
    assert(result.stateOfType[BoardState].currentStep == step)
  }
}
