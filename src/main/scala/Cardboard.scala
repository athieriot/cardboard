import akka.actor.typed.ActorSystem
import cards.*
import game.*
import interfaces.CommandLine

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@main def init(args: String*): Unit = {
  val id = args.headOption.map(UUID.fromString).getOrElse(UUID.randomUUID())
  val system = ActorSystem(CommandLine(game.Engine(id)), "command-line")

  println(s"Initiate Game: $id")

  system ! CommandLine.Initiate

  Await.ready(system.whenTerminated, Duration.Inf)
}