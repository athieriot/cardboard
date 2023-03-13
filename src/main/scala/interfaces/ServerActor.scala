package interfaces

import scala.concurrent.duration.*
import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.{Directives, Route}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.typed.EventEnvelope
import akka.serialization.{Serialization, SerializationExtension}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import game.*
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class WebCommand(name: String, player: PlayerId, id: Option[CardId], args: Option[List[String]])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val webCommandFormat: RootJsonFormat[WebCommand] = jsonFormat4(WebCommand.apply)
}

object ServerActor extends Directives with JsonSupport {

  sealed trait ServerStatus
  private final case class StartFailed(cause: Throwable) extends ServerStatus
  private final case class Started(binding: ServerBinding) extends ServerStatus
  private case object Stop extends ServerStatus

  def apply(host: String, port: Int, id: UUID): Behavior[ServerStatus] = Behaviors.setup { ctx =>

    implicit val system: ActorSystem[Nothing] = ctx.system
    implicit val mat: Materializer = Materializer(system)

    val serialization: Serialization = SerializationExtension(system)

    val readJournal: JdbcReadJournal =
      PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

    implicit val timeout: Timeout = 3.seconds
    val cardboard = ctx.spawn(game.Engine(id), "game")

    def webSocketRoute(id: UUID): Route = {
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

    def httpRoute(cardBoard: ActorRef[Action]): Route = {

      import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
      import akka.actor.typed.scaladsl.AskPattern.Askable

      // TODO: Maybe should have a command to request the state of a card ?
      path("command") {
        post {
          entity(as[WebCommand]) { cmd =>
            val done: Future[State] = cmd match {
              // TODO: Args
              case WebCommand("play", player, Some(id), _) =>
                cardBoard.askWithStatus(replyTo => PlayLand(replyTo, player, id, List()))
              case WebCommand("next", player, _, _) =>
                cardBoard.askWithStatus(replyTo => Next(replyTo, player))
              case WebCommand("resolve", player, _, _) =>
                cardBoard.askWithStatus(replyTo => Resolve(replyTo, player))
              case WebCommand("end", player, _, _) =>
                cardBoard.askWithStatus(replyTo => EndTurn(replyTo, player))
              case _ => Future.failed(new RuntimeException("Command not found"))
            }

            onComplete(done) {
              case Success(_) => complete("Command sent")
              case Failure(error) => complete(StatusCodes.BadRequest -> error.getLocalizedMessage)
            }
          }
        }
      }
    }

    def messageOutSource(id: UUID): Source[TextMessage, NotUsed] = {
      readJournal.eventsByPersistenceId(id.toString, 0L, Long.MaxValue)
        .map(e => {
          e.copy(event = e.event match {
            // TODO: Now we can use the normal GameCreated object !!
            case GameCreated(die, step, players) =>
              GameCreatedSimplified(die, step, players.view.mapValues(d => d.map(c => (c._1, SimpleCard(
                c._2.name,
                c._2.set,
                c._2.numberInSet,
                c._2.activatedAbilities.map(a => (a._1, a._2.text)).toList
              )))).toMap)
            case other: Event => other
          })
        })
        .map(e => EventEnvelope[Event](e.offset, e.persistenceId, e.sequenceNr, e.event.asInstanceOf[Event], e.timestamp, e.event.getClass.getSimpleName, 0))
        .map(e => new String(serialization.serialize(e).get, StandardCharsets.UTF_8))
        .map(e => TextMessage(e))
    }

    def running(binding: ServerBinding): Behavior[ServerStatus] =
      Behaviors.receiveMessagePartial[ServerStatus] {
        case Stop =>
          ctx.log.info(
            "Stopping server http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          Behaviors.stopped
      }.receiveSignal {
        case (_, PostStop) =>
          binding.unbind()
          Behaviors.same
      }

    def starting(wasStopped: Boolean): Behaviors.Receive[ServerStatus] =
      Behaviors.receiveMessage[ServerStatus] {
        case StartFailed(cause) =>
          throw new RuntimeException("Server failed to start", cause)
        case Started(binding) =>
          ctx.log.info(
            "Server online at http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          if (wasStopped) ctx.self ! Stop
          running(binding)
        case Stop =>
          // we got a stop message but haven't completed starting yet,
          // we cannot stop until starting has completed
          starting(wasStopped = true)
      }

    val serverBinding: Future[Http.ServerBinding] =
      Http().newServerAt(host, port).bind(cors() {
        httpRoute(cardboard) ~ webSocketRoute(id)
      })
    ctx.pipeToSelf(serverBinding) {
      case Success(binding) => Started(binding)
      case Failure(ex) => StartFailed(ex)
    }

    starting(wasStopped = false)
  }
}
