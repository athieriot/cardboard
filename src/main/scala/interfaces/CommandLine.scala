package interfaces

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import game.{Action, Draw, State, Tap}

import scala.concurrent.duration.*
import scala.util.{Failure, Success}

object CommandLine {
  trait Status
  case object Ready extends Status
  case object Terminate extends Status

  def apply(instance: Behavior[Action]): Behavior[Status] =
    Behaviors.setup[CommandLine.Status] { context =>
      implicit val timeout: Timeout = 3.seconds

      val cardboard = context.spawn(instance, "game")

      Behaviors.receiveMessage[CommandLine.Status] {
        case Terminate => Behaviors.stopped
        case Ready =>
          print("> ")
          val input = scala.io.StdIn.readLine()

          if input == "!!!" then
            context.self ! Terminate
            Behaviors.same
          else
            // TODO: Match text to commands
            // TODO: Print a list of known actions
            val actionOpt = input.toLowerCase.split(" ").toList match {
              case "tap" :: id :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Tap(ref, 0, id))
              case "draw" :: count :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Draw(ref, 0, count.toInt))
              case _ => println("I don't understand your action"); None
            }

            actionOpt.map { action =>
              println(s"$input")
              context.askWithStatus(cardboard, action) {
                case Success(state: game.State) => state.render(state); Ready
                case Failure(StatusReply.ErrorMessage(text)) => println(text); Ready
                case Failure(_) => println("An error occurred"); Ready
              }
            }

            // TODO: Set Active Player by name
            // TODO: Render state in cli

            Behaviors.same
      }
    }
}