import akka.actor.typed.ActorSystem
import cards.*
import game.*
import interfaces.CommandLine

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@main def init(): Unit = {
  val instance = game.Engine(
    UUID.randomUUID(),
    Map("Jack" -> List(forest), "Jill" -> List(forest))
  )
  val system = ActorSystem(CommandLine(instance), "command-line")

  system ! CommandLine.Initiate

  Await.ready(system.whenTerminated, Duration.Inf)
}