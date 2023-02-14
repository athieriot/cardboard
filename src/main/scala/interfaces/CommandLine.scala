package interfaces

import akka.actor.typed.{ActorRef, Behavior, RecipientRef}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.pattern.StatusReply
import akka.util.Timeout
import cards.*
import game.*

import scala.concurrent.duration.*
import scala.reflect.ClassTag
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
          SendAction(context, cardboard, Recover.apply, {
            case game.EmptyState => println("No existing game found"); Prepare
            case state: game.InProgressState => render(state); Ready(state.playersTurn)
            case state => println(s"Wrong state $state"); Terminate
          })

        case Prepare =>
          print("Player One: "); val playerOne = scala.io.StdIn.readLine()
          print("Player Two: "); val playerTwo = scala.io.StdIn.readLine()

          val players = Map(
            playerOne -> standardDeck,
            playerTwo -> standardDeck
          )

          SendAction(context, cardboard, ref => New(ref, players), {
            case state: game.InProgressState => render(state); Ready(state.playersTurn)
            case state => println(s"Wrong state $state"); Terminate
          })

        case Ready(activePlayer) =>
          print(s"|$activePlayer|> ")
          val input = scala.io.StdIn.readLine()

          if input == "!!!" then
            context.self ! Terminate
            Behaviors.same
          else
            // TODO: Print a list of known actions
            // TODO: Jline ?
            val actionOpt = input.toLowerCase.split(" ").toList match {
              case "tap" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Tap(ref, activePlayer, target.toInt))
              case "play" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Play(ref, activePlayer, target.toInt))
              case "draw" :: count :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Draw(ref, activePlayer, count.toInt))
              case "discard" :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Discard(ref, activePlayer, None))
              case "discard" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Discard(ref, activePlayer, Some(target.toInt)))
              case _ => println("I don't understand your action"); context.self ! Ready(activePlayer); None
            }

            actionOpt.foreach { action =>
              println(s"$input")
              SendAction(context, cardboard, action, {
                case state: game.InProgressState => render(state); Ready(state.playersTurn)
                case state => println(s"Wrong state $state"); Terminate
              })
            }

            Behaviors.same
      }
    }

   private def SendAction[Command, State](
    context: ActorContext[CommandLine.Status],
    target: ActorRef[Command],
    request: ActorRef[StatusReply[State]] => Command,
    onSuccess: State => CommandLine.Status)(implicit responseTimeout: Timeout, classTag: ClassTag[State]
   ): Behavior[CommandLine.Status] = {
     context.askWithStatus(target, request) {
       case Success(state) => onSuccess(state)
       case Failure(StatusReply.ErrorMessage(text)) => println(text); Terminate
       case Failure(_) => println("An error occurred"); Terminate
     }
     Behaviors.same
  }

  private def render(state: InProgressState): Unit = {
    println("\n")
    val playerOne = state.players.head
    val playerTwo = state.players.last
    renderPlayer(state, playerOne._1, playerOne._2)

    // TODO: Display summoning sickness
    // TODO: Show each types (Creatures, Artefacts, Enchantments, Planeswalkers) on a different line
    println(s"🌳Lands: ${state.battleField.filter(_._2.owner == playerOne._1).map(p => s"${p._2.card.name}[${p._1}][${if p._2.status == Status.Untapped then " " else "T"}]").mkString(", ")}")
    println("|------------------")
    println(s"🌳Lands: ${state.battleField.filter(_._2.owner == playerTwo._1).map(p => s"${p._2.card.name}[${p._1}][${if p._2.status == Status.Untapped then " " else "T"}]").mkString(", ")}")

    renderPlayer(state, playerTwo._1, playerTwo._2)

    println(s"| Phase: ${Phase.values.map(phase => if state.phase == phase then s"🌙$phase" else phase).mkString(", ")}")
    println("|------------------")
    println("\n")
  }

  private def renderPlayer(state: InProgressState, name: String, playerState: PlayerSide): Unit = {
    val active = if state.playersTurn == name then "⭐ " else ""
    val priority = if state.priority == name then " 🟢" else ""

    println("|------------------")
    println(s"| Player: $active$name$priority - Life: ${playerState.life}")
    println(s"| 📚Library: ${playerState.library.size}")
    println(s"| 🪦Graveyard (${playerState.graveyard.size}): ${playerState.graveyard.map(p => s"${p._2.name}[${p._1}]").mkString(", ")}")
    println(s"| ✋ Hand (${playerState.hand.size}): ${playerState.hand.map(p => s"${p._2.name}[${p._1}]").mkString(", ")}")
    // TODO: Use color for mana
    println(s"| 🪄Mana: ${playerState.manaPool.map(m => s"${m._1} = ${m._2}").mkString(" / ")}")
    println("|------------------")
  }
}