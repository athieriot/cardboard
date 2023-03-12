import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.server.Directives.*
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.typed.EventEnvelope
import akka.stream.scaladsl.{Flow, Sink, Source}
import game.*
import interfaces.{CommandLine, ServerActor}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.serialization.SerializationExtension
import akka.util.ByteString
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import game.cards.Deck

import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

@main def init(args: String*): Unit = {
  args match {
    case "console" :: args =>
      val id = args.headOption.map(UUID.fromString).getOrElse(UUID.randomUUID())
      val system = ActorSystem(CommandLine(game.Engine(id)), "command-line")

      println(s"Initiate Game: $id")

      system ! CommandLine.Initiate

      Await.ready(system.whenTerminated, Duration.Inf)
      
    case "web" :: args =>
      val id = args.headOption.map(UUID.fromString).getOrElse(UUID.randomUUID())
      val system: ActorSystem[ServerActor.ServerStatus] = ActorSystem(ServerActor("localhost", 8080, id), "server")

      println(s"Start game server: $id")

      Await.ready(system.whenTerminated, Duration.Inf)
    case Nil => println("Please choose an interface between 'web' and 'console'")
  }
}