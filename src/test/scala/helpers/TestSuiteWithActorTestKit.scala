package helpers

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ActorTestKitBase, SerializationTestKit, TestProbe}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.actor.{DeadLetter, Dropped, UnhandledMessage}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import utest.PlatformShims.EnableReflectiveInstantiation
import utest.framework.Formatter
import utest.{TestSuite, TestSuiteVersionSpecific, Tests, framework}

import scala.concurrent.{ExecutionContext, Future}

abstract class TestSuiteWithActorTestKit(testKit: ActorTestKit) extends TestSuite {

  /**
   * Config loaded from `application-test.conf` if that exists, otherwise
   * using default configuration from the reference.conf resources that ship with the Akka libraries.
   * The application.conf of your project is not used in this case.
   */
  def this() = this(ActorTestKit(ActorTestKitBase.testNameFromCallStack()))

  /**
   * Use a custom [[akka.actor.typed.ActorSystem]] for the actor system.
   */
  def this(system: ActorSystem[_]) = this(ActorTestKit(system))

  /**
   * Use a custom config for the actor system.
   */
  def this(config: String) =
    this(ActorTestKit(ActorTestKitBase.testNameFromCallStack(), ConfigFactory.parseString(config)))

  /**
   * Use a custom config for the actor system.
   */
  def this(config: Config) = this(ActorTestKit(ActorTestKitBase.testNameFromCallStack(), config))

  /**
   * Use a custom config for the actor system, and a custom [[akka.actor.testkit.typed.TestKitSettings]].
   */
  def this(config: Config, settings: TestKitSettings) =
    this(ActorTestKit(ActorTestKitBase.testNameFromCallStack(), config, settings))

//  override def afterAll(): Unit = testKit.shutdownTestKit()

  // delegates of the TestKit api for minimum fuss

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  implicit def system: ActorSystem[Nothing] = testKit.system

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  implicit def testKitSettings: TestKitSettings = testKit.testKitSettings

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  implicit def timeout: Timeout = testKit.timeout

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def spawn[T](behavior: Behavior[T]): ActorRef[T] = testKit.spawn(behavior)

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def spawn[T](behavior: Behavior[T], name: String): ActorRef[T] = testKit.spawn(behavior, name)

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def spawn[T](behavior: Behavior[T], props: Props): ActorRef[T] = testKit.spawn(behavior, props)

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def spawn[T](behavior: Behavior[T], name: String, props: Props): ActorRef[T] = testKit.spawn(behavior, name, props)

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def createTestProbe[M](): TestProbe[M] = testKit.createTestProbe[M]()

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def createTestProbe[M](name: String): TestProbe[M] = testKit.createTestProbe(name)

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def createDroppedMessageProbe(): TestProbe[Dropped] = testKit.createDroppedMessageProbe()

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def createDeadLetterProbe(): TestProbe[DeadLetter] = testKit.createDeadLetterProbe()

  /**
   * See corresponding method on [[ActorTestKit]]
   */
  def createUnhandledMessageProbe(): TestProbe[UnhandledMessage] = testKit.createUnhandledMessageProbe()

  /**
   * Additional testing utilities for serialization.
   */
  def serializationTestKit: SerializationTestKit = testKit.serializationTestKit

  override def utestAfterAll(): Unit =
    super.utestAfterAll()
    testKit.shutdownTestKit()
}