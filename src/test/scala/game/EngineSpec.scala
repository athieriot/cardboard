package game

import akka.Done
import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import cards.Deck
import cards.types.{forest, llanowarElf}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class EngineSpec
  extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
    with AnyWordSpecLike
    with should.Matchers
    with BeforeAndAfterEach
    with LogCapturing {

  val testingDeck: Deck = Deck((1 to 30).map(_ => forest).toList ++ (1 to 30).map(_ => llanowarElf))

  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[Action, Event, State](
      system,
      game.Engine(UUID.randomUUID())
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "Game" must {

    "be created for one user" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[State]](New(_, Map("Jack" -> testingDeck)))
      result.reply shouldBe StatusReply.Success
      result.event shouldBe Created(0, Map("Jack" -> testingDeck))
      result.stateOfType[BoardState] shouldBe BoardState(
        "Jack",
        "Jack",
        Map("Jack" -> PlayerState(testingDeck.cards))
      )
    }

//    "handle Withdraw" in {
//      eventSourcedTestKit.runCommand[StatusReply[Done]](AccountEntity.CreateAccount(_))
//
//      val result1 = eventSourcedTestKit.runCommand[StatusReply[Done]](AccountEntity.Deposit(100, _))
//      result1.reply shouldBe StatusReply.Ack
//      result1.event shouldBe AccountEntity.Deposited(100)
//      result1.stateOfType[AccountEntity.OpenedAccount].balance shouldBe 100
//
//      val result2 = eventSourcedTestKit.runCommand[StatusReply[Done]](AccountEntity.Withdraw(10, _))
//      result2.reply shouldBe StatusReply.Ack
//      result2.event shouldBe AccountEntity.Withdrawn(10)
//      result2.stateOfType[AccountEntity.OpenedAccount].balance shouldBe 90
//    }
//
//    "reject Withdraw overdraft" in {
//      eventSourcedTestKit.runCommand[StatusReply[Done]](AccountEntity.CreateAccount(_))
//      eventSourcedTestKit.runCommand[StatusReply[Done]](AccountEntity.Deposit(100, _))
//
//      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](AccountEntity.Withdraw(110, _))
//      result.reply.isError shouldBe true
//      result.hasNoEvents shouldBe true
//    }
//
//    "handle GetBalance" in {
//      eventSourcedTestKit.runCommand[StatusReply[Done]](AccountEntity.CreateAccount(_))
//      eventSourcedTestKit.runCommand[StatusReply[Done]](AccountEntity.Deposit(100, _))
//
//      val result = eventSourcedTestKit.runCommand[AccountEntity.CurrentBalance](AccountEntity.GetBalance(_))
//      result.reply.balance shouldBe 100
//      result.hasNoEvents shouldBe true
//    }
  }
}