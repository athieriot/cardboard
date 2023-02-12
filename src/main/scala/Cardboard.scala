import akka.actor.typed.ActorSystem
import cards.*
import game.*
import interfaces.CommandLine

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@main def init(): Unit = {
  val instance = Engine(
    UUID.randomUUID(),
    Map(0 -> List(forest), 1 -> List(forest))
  )
  val system = ActorSystem(CommandLine(instance), "command-line")

  system ! CommandLine.Ready

  Await.ready(system.whenTerminated, Duration.Inf)
}