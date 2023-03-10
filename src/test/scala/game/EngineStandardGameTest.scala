package game

import akka.Done
import akka.pattern.StatusReply
import akka.persistence.testkit.{PersistenceTestKitPlugin, PersistenceTestKitSnapshotPlugin}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings
import akka.persistence.typed.PersistenceId
import com.typesafe.config.ConfigFactory
import game.Engine.idify
import game.EngineSetupTest.eventSourcedTestKit
import game.cards.*
import game.cards.types.*
import game.mana.{Color, ManaCost}
import game.mechanics.*
import helpers.GameChecks.*
import helpers.TestSuiteWithActorTestKit

import scala.collection.immutable.ListMap
import utest.*

import java.time.Instant
import java.util.UUID

object EngineStandardGameTest extends TestSuiteWithActorTestKit(EventSourcedBehaviorTestKit.config) {
  implicit private val eventSourcedTestKit: EventSourcedBehaviorTestKit[Action, Event, State] =
    EventSourcedBehaviorTestKit[Action, Event, State](
      system,
      game.Engine(UUID.randomUUID()),
      SerializationSettings.disabled
    )

  override def utestBeforeEach(path: Seq[String]): Unit = {
    super.utestBeforeEach(path: Seq[String])
    eventSourcedTestKit.clear()
  }

  private val players = Map(
    "Jack" -> Deck((1 to 30).flatMap(_ => List(
      collection.fourthEdition.Mountain(),
      collection.portalThreeKingdoms.MountainBandit(),
    )).toList),
    "Jill" -> blueDeck
  )

  val tests: Tests = Tests {
    eventSourcedTestKit.initialize(List(GameCreated(0, Step.preCombatMain, idify(players)), DamageDealt("Jill", 19), Drawn(7, "Jack"), Drawn(7, "Jill")):_*)

    test("Standard Game") {

      // TODO: Counter a spell
      // TODO: Tim Ping (For Ability on the stack)
      // TODO: AnkhOfMishra (For Triggered Ability)
      // TODO: Trample (Also for blockers)
      // TODO: End of Turn With Discard + New Turn With Untap
      test("Win through damage") {
        playLandCheck(0, "Jack", 6)
        tapForManaCheck(0, 1, "Jack", Color.red, 1)
        castPermanentCheck(1, "Jack", "Jill", ManaCost("R"), 5)
        resolvePermanentCheck(1, "Jill", "Jack")

        moveToStepCheck("Jack", Step.beginningOfCombat)
        moveToStepCheck("Jack", Step.declareAttackers)

        // Attack with Haste
        declareAttackerCheck(1, "Jack")
        moveToStepCheck("Jack", Step.declareBlockers)

        val result = eventSourcedTestKit.runCommand[StatusReply[State]](Next(_, "Jack"))
        assert(result.reply.isSuccess)
        assert(result.events == List(MovedToStep(Step.combatDamage), DamageDealt("Jill", 1), GameEnded("Jill"), PriorityPassed("Jack")))
        assert(result.stateOfType[EndState].loser == "Jill")
      }
    }
  }
}
