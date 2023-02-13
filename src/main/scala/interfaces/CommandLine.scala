package interfaces

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import cards.{Card, forest}
import game.*

import scala.concurrent.duration.*
import scala.util.{Failure, Success}

object CommandLine {
  trait Status

  case object Initiate extends Status
  case object Prepare extends Status
  case class Ready(activePlayer: String) extends Status
  case object Terminate extends Status

  def apply(instance: Behavior[Action]): Behavior[Status] =
    Behaviors.setup[CommandLine.Status] { context =>
      implicit val timeout: Timeout = 3.seconds

      val cardboard = context.spawn(instance, "game")

      Behaviors.receiveMessage[CommandLine.Status] {
        case Terminate => Behaviors.stopped
        case Initiate =>
          context.askWithStatus(cardboard, Recover.apply) {
            case Success(game.EmptyState) => println("Game not ready"); Prepare
            case Success(state: game.InProgressState) => render(state); Ready(state.activePlayer)
            case Failure(StatusReply.ErrorMessage(text)) => println(text); Terminate
            case Success(state) => println(s"Wrong state $state"); Terminate
            case Failure(_) => println("An error occurred"); Terminate
          }
          Behaviors.same

        case Prepare =>
          val standardDeck: List[Card] = (1 to 40).map(_ => forest).toList

          print("Player One ?")
          val playerOne = scala.io.StdIn.readLine()
          print("Player Two ?")
          val playerTwo = scala.io.StdIn.readLine()

          context.askWithStatus(cardboard, ref => New(ref, Map(playerOne -> standardDeck, playerTwo -> standardDeck))) {
            case Success(state: game.InProgressState) => render(state); Ready(state.activePlayer)
            case Failure(StatusReply.ErrorMessage(text)) => println(text); Terminate
            case Success(state) => println(s"Wrong state $state"); Terminate
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
              case "mulligan" :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Mulligan(ref, activePlayer))
              case "discard" :: count :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Discard(ref, activePlayer, Some(count.toInt)))
              case _ => println("I don't understand your action"); context.self ! Ready(activePlayer); None
            }

            actionOpt.foreach { action =>
              println(s"$input")
              context.askWithStatus(cardboard, action) {
                case Success(state: game.InProgressState) => render(state); Ready(state.activePlayer)
                case Failure(StatusReply.ErrorMessage(text)) => println(text); Ready(activePlayer)
                case Success(state) => println(s"Wrong state $state"); Terminate
                case Failure(_) => println("An error occurred"); Ready(activePlayer)
              }
            }

            // TODO: Render state in cli

            Behaviors.same
      }
    }

  def render(state: InProgressState): Unit = {
    println(s"Active Player: ${state.activePlayer}")
    state.players.foreach { case (i, playerState) =>
      println(s"Player: $i - Life: ${playerState.life}")
      println(s"Library: ${playerState.library.length}")
      println(s"Graveyard: ${playerState.graveyard.toList.length}")
      println(s"Hand: ${playerState.hand.map(p => s"${p._2.name}").mkString(", ")}")
      println("\n")
    }
  }
}