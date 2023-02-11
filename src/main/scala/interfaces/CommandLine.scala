package interfaces

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import game.{Command, Tap}

import scala.concurrent.duration.*
import scala.util.{Failure, Success}

object CommandLine {
  trait Status
  case object Ready extends Status
  case object Terminate extends Status

  def apply(instance: Behavior[Command]): Behavior[Status] =
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
            println(s"Tap $input")
            context.askWithStatus(cardboard, ref => Tap(ref, input)) {
              case Success(text) => println(text); Ready
              case Failure(StatusReply.ErrorMessage(text)) => println(text); Ready
              case Failure(_) => println("An error occurred"); Ready
            }
            Behaviors.same
      }
    }
}