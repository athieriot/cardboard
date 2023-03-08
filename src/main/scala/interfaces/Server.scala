package interfaces

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directives, Route}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.typed.EventEnvelope
import akka.serialization.{Serialization, SerializationExtension}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import collection.sets.{FourthEdition, PortalThreeKingdoms}
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.cards.*
import interfaces.CommandLine.lineReader
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

case class WebCommand(name: String, player: PlayerId, id: CardId, args: List[String])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val webCommandFormat: RootJsonFormat[WebCommand] = jsonFormat4(WebCommand.apply)
}

class Server(actorSystem: ActorSystem[Nothing]) extends Directives with JsonSupport {
  implicit val system: ActorSystem[Nothing] = actorSystem
  implicit val ec: ExecutionContextExecutor = system.executionContext
  implicit val mat: Materializer = Materializer(system)

  private val serialization: Serialization = SerializationExtension(system)

  private val readJournal: JdbcReadJournal =
    PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

  def start(id: UUID): Unit = {
    val cardboard = actorSystem.systemActorOf(game.Engine(id), "game")

    val bindingFuture = Http()
      .newServerAt("localhost", 8080)
      .bind(cors() { webSocketRoute(id) ~ httpRoute(cardboard) })

    println(s"Server now online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  private def webSocketRoute(id: UUID): Route = {
    import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling.*

      path("connect") {
        extractWebSocketUpgrade { upgrade =>
          complete(upgrade.handleMessagesWithSinkSource(
            Sink.ignore,
            messageOutSource(id)
          ))
        }
      }
  }

  private def httpRoute(cardBoard: ActorRef[Action]): Route = {
    path("command") {
      post {
        entity(as[WebCommand]) {
          // TODO: Args
          case WebCommand("play", player, id, _) =>
            // TODO: Error Handling
            cardBoard ! PlayLand(system.ignoreRef, player, id, List())
            complete("Command sent")
          case _ => complete(StatusCodes.InternalServerError -> "Command not found")
        }
      }
    }
  }

  private def messageOutSource(id: UUID): Source[TextMessage, NotUsed] = {
    readJournal.eventsByPersistenceId(id.toString, 0L, Long.MaxValue)
      .map(e => { e.copy(event = e.event match {
        case GameCreated(die, step, players, createdAt) =>
          GameCreatedSimplified(die, step, players.view.mapValues(d => d.cards.map(c => SimpleCard(c.name, c.set, c.numberInSet))).toMap, createdAt)
        case other: Event => other
      })})
      .map(e => EventEnvelope[Event](e.offset, e.persistenceId, e.sequenceNr, e.event.asInstanceOf[Event], e.timestamp, e.event.getClass.getSimpleName, 0))
      .map(e => new String(serialization.serialize(e).get, StandardCharsets.UTF_8))
      .map(e => TextMessage(e))
  }
}
