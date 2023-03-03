package game

import akka.Done
import akka.pattern.StatusReply
import akka.persistence.testkit.{PersistenceTestKitPlugin, PersistenceTestKitSnapshotPlugin}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings
import akka.persistence.typed.PersistenceId
import com.typesafe.config.ConfigFactory
import game.cards.*
import game.cards.types.*
import helpers.TestSuiteWithActorTestKit
import utest.*

import java.util.UUID

object GameSetupTest extends TestSuiteWithActorTestKit(EventSourcedBehaviorTestKit.config) {
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[Action, Event, State](
      system,
      game.Engine(UUID.randomUUID()),
      SerializationSettings.disabled
    )

  override def utestBeforeEach(path: Seq[String]): Unit = {
    super.utestBeforeEach(path: Seq[String])
    eventSourcedTestKit.clear()
  }

  val testingDeck: Deck = Deck(
    (1 to 30).map(_ => collection.fourthEdition.Forest()).toList
      ++ (1 to 30).map(_ => collection.fourthEdition.LlanowarElf())
  )

  val tests: Tests = Tests {
    test("Game") {
      val result = eventSourcedTestKit.runCommand[StatusReply[State]](c => Recover(c))
      assert(result.reply == StatusReply.Success(EmptyState))
      assert(result.hasNoEvents)
    }
  }
}