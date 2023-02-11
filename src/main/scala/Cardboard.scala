import akka.actor.typed.ActorSystem
import game.*
import interfaces.CommandLine

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@main def init(): Unit = {
  val instance = Engine(
    UUID.randomUUID(),
    Map(1 -> Land("forest", State.Tapped))
  )
  val system = ActorSystem(CommandLine(instance), "command-line")

  system ! CommandLine.Ready

  Await.ready(system.whenTerminated, Duration.Inf)
}