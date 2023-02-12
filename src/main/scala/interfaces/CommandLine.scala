package interfaces

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import game.*

import scala.concurrent.duration.*
import scala.util.{Failure, Success}

object CommandLine {
  trait Status
  case object Initiate extends Status
  case class Ready(activePlayer: String) extends Status
  case object Terminate extends Status

  def apply(instance: Behavior[Action]): Behavior[Status] =
    Behaviors.setup[CommandLine.Status] { context =>
      implicit val timeout: Timeout = 3.seconds

      val cardboard = context.spawn(instance, "game")

      Behaviors.receiveMessage[CommandLine.Status] {
        case Terminate => Behaviors.stopped
        case Initiate =>
          context.askWithStatus(cardboard, Start.apply) {
            case Success(state: game.State) => state.render(state); Ready(state.activePlayer)
            case Failure(StatusReply.ErrorMessage(text)) => println(text); Terminate
            case Failure(_) => println("An error occurred"); Terminate
          }
          Behaviors.same
        case Ready(activePlayer) =>
          print(s"|$activePlayer|> ")
          val input = scala.io.StdIn.readLine()

          if input == "!!!" then
            context.self ! Terminate
            Behaviors.same
          else
            // TODO: Match text to commands
            // TODO: Print a list of known actions
            val actionOpt = input.toLowerCase.split(" ").toList match {
              case "tap" :: id :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Tap(ref, activePlayer, id))
              case "draw" :: count :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Draw(ref, activePlayer, count.toInt))
              case _ => println("I don't understand your action"); context.self ! Ready(activePlayer); None
            }

            actionOpt.foreach { action =>
              println(s"$input")
              context.askWithStatus(cardboard, action) {
                case Success(state: game.State) => state.render(state); Ready(state.activePlayer)
                case Failure(StatusReply.ErrorMessage(text)) => println(text); Ready(activePlayer)
                case Failure(_) => println("An error occurred"); Ready(activePlayer)
              }
            }

            // TODO: Render state in cli

            Behaviors.same
      }
    }
}