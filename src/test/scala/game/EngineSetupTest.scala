package game

import akka.Done
import akka.pattern.StatusReply
import akka.persistence.testkit.{PersistenceTestKitPlugin, PersistenceTestKitSnapshotPlugin}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings
import akka.persistence.typed.PersistenceId
import com.typesafe.config.ConfigFactory
import game.Engine.idify
import game.cards.*
import game.cards.types.*
import game.mechanics.*
import helpers.TestSuiteWithActorTestKit

import scala.collection.immutable.ListMap
import utest.*

import java.time.Instant
import java.util.UUID
import scala.util.Random

object EngineSetupTest extends TestSuiteWithActorTestKit(EventSourcedBehaviorTestKit.config) {
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
    test("Game Setup") {

      test("Retrieve EmptyState") {
        val result = eventSourcedTestKit.runCommand[StatusReply[State]](c => Recover(c))
        result.reply ==> StatusReply.Success(EmptyState)
        assert(result.hasNoEvents)
        assert(result.state.isInstanceOf[EmptyState.type])
      }

      test("Prepare New Game for one player") {
        val player = "Jill"

        val result = eventSourcedTestKit.runCommand[StatusReply[State]](c => New(c, Map(player -> testingDeck)))
        assert(result.reply.isSuccess)
        assert(result.events.size == 3)
        assert(result.events.contains(GameCreated(0, Step.preCombatMain, Map(player -> idify(testingDeck, 0)))))
        assertMatch(result.events.find(_.isInstanceOf[Shuffled])) { case Some(Shuffled(_, "Jill")) => }
        assert(result.events.contains(Drawn(7, player)))

        assert(result.stateOfType[BoardState].players(player).life == 20)
        assert(result.stateOfType[BoardState].currentStep == Step.preCombatMain)

        val library = result.stateOfType[BoardState].listCardsFromZone(Library(player))
        assert((library.values.size - testingDeck.cards.size) == -7)
        assert(library.keys.toSeq.zip(library.keys.toSeq.sorted).count(x => x._1 == x._2) < library.size)

        val hand = result.stateOfType[BoardState].listCardsFromZone(Hand(player))
        assert(hand.size == 7)
      }

      test("Retrieve a game in Progress") {
        eventSourcedTestKit.runCommand[StatusReply[State]](c => New(c, Map("Jack" -> testingDeck, "Jill" -> testingDeck)))
        val result = eventSourcedTestKit.runCommand[StatusReply[State]](c => Recover(c))
        assert(result.reply.isSuccess)
        assert(result.state.isInstanceOf[BoardState])
      }

      test("Cannot create a game if Deck invalid") {
        val result = eventSourcedTestKit.runCommand[StatusReply[State]](c => New(c, Map("Jill" -> Deck(List()))))
        assert(result.reply.isError)
        assert(result.reply.getError.getLocalizedMessage == "Deck invalid")
      }
    }
  }
}