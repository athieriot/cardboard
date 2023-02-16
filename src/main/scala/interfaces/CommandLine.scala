package interfaces

import akka.actor.typed.{ActorRef, Behavior, RecipientRef}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.pattern.StatusReply
import akka.util.Timeout
import cards.*
import cards.mana.*
import game.*
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.{EnumCompleter, StringsCompleter}
import org.jline.terminal.TerminalBuilder

import scala.concurrent.duration.*
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object CommandLine {

  trait Status

  case object Initiate extends Status
  case object Prepare extends Status
  case class Ready(activePlayer: PlayerId) extends Status
  case object Terminate extends Status

  def apply(instance: Behavior[Action]): Behavior[Status] =
    Behaviors.setup[CommandLine.Status] { context =>
      implicit val timeout: Timeout = 3.seconds

      val lineReader = LineReaderBuilder.builder()
        .terminal(TerminalBuilder.terminal)
        .completer(new StringsCompleter("play", "use", "pass", "discard", "exit"))
        .build()

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
          val playerOne = lineReader.readLine("Player One: ")
          val playerTwo = lineReader.readLine("Player Two: ")

          val players = Map(
            playerOne -> standardDeck,
            playerTwo -> standardDeck
          )

          SendAction(context, cardboard, ref => New(ref, players), {
            case state: game.InProgressState => render(state); Ready(state.playersTurn)
            case state => println(s"Wrong state $state"); Terminate
          })

        case Ready(activePlayer) =>
          val input = lineReader.readLine(s"|$activePlayer|> ")

          if input == "exit" then
            context.self ! Terminate
            Behaviors.same
          else
            // TODO: Jline ?
            val actionOpt = input.toLowerCase.split(" ").toList match {
              case "play" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => PlayLand(ref, activePlayer, target.toInt))
              case "cast" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Cast(ref, activePlayer, target.toInt))
              case "use" :: target :: abilityId :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Use(ref, activePlayer, target.toInt, abilityId.toInt))
              case "pass" :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Pass(ref, activePlayer))
              case "discard" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Discard(ref, activePlayer, target.toInt))
              case _ => println("I don't understand your action"); context.self ! Ready(activePlayer); None
            }

            actionOpt.foreach { action =>
              println(s"$input")
              SendAction(context, cardboard, action, {
                case state: game.InProgressState => render(state); Ready(state.playersTurn)
                case state => println(s"Wrong state $state"); Terminate
              }, {
                text => println(text); Ready(activePlayer)
              })
            }

            Behaviors.same
      }
    }

  private def SendAction[Command, State](
    context: ActorContext[CommandLine.Status],
    target: ActorRef[Command],
    request: ActorRef[StatusReply[State]] => Command,
    onSuccess: State => CommandLine.Status,
    onError: String => CommandLine.Status = {message => println(s"An error occurred ${message}"); Terminate},
  )(implicit responseTimeout: Timeout, classTag: ClassTag[State]): Behavior[CommandLine.Status] = {
    context.askWithStatus(target, request) {
      case Success(state) => onSuccess(state)
      case Failure(StatusReply.ErrorMessage(text)) => onError(text)
      case Failure(ex) => onError(ex.getLocalizedMessage)
    }
    Behaviors.same
  }

  // TODO: Add links to Scryfall
  private def render(state: InProgressState): Unit = {
    println("\n")
    val playerOne = state.players.head
    val playerTwo = state.players.last
    renderPlayer(state, playerOne._1, playerOne._2)

    // TODO: Display summoning sickness
    // TODO: Show each types (Creatures, Artefacts, Enchantments, Planeswalkers) on a different line
    println(s"| ✋ Hand (${playerOne._2.hand.size}): ${playerOne._2.hand.map(p => renderName(p._1, p._2)).mkString(", ")}")
    println(s"| 🌳Lands: ${state.battleField.filter(_._2.owner == playerOne._1).map(p => s"${renderName(p._1, p._2.card)}[${if p._2.status == Status.Untapped then " " else "T"}]").mkString(", ")}")
    println("|------------------")
    println(s"| 🌳Lands: ${state.battleField.filter(_._2.owner == playerTwo._1).map(p => s"${renderName(p._1, p._2.card)}[${if p._2.status == Status.Untapped then " " else "T"}]").mkString(", ")}")
    println(s"| ✋ Hand (${playerTwo._2.hand.size}): ${playerTwo._2.hand.map(p => renderName(p._1, p._2)).mkString(", ")}")

    renderPlayer(state, playerTwo._1, playerTwo._2)

    println(s"| Phase: ${Phase.values.map(phase => if state.phase == phase then s"🌙$phase" else phase).mkString(", ")}")
    println("|------------------")
    println("\n")
  }

  private def renderPlayer(state: InProgressState, name: String, playerState: PlayerState): Unit = {
    val active = if state.playersTurn == name then "⭐ " else ""
    val priority = if state.priority == name then " 🟢" else ""

    println("|------------------")
    println(s"| Player: $active$name$priority - Life: ${playerState.life}")
    println(s"| 📚Library: ${playerState.library.size}")
    println(s"| 🪦Graveyard (${playerState.graveyard.size}): ${playerState.graveyard.map(p => renderName(p._1, p._2)).mkString(", ")}")
    println(s"| 🪄Mana: ${playerState.manaPool.pool.map(m => terminalColor(m._1, s"${m._1} (${m._2})")).mkString(" / ")}")
    println("|------------------")
  }

  def renderName(id: CardId, card: Card): String = s"${terminalColor(card.color, card.name)}[$id]"

  private def terminalColor(c: Color, text: String): String = c match {
    case Color.red => s"${Console.RED}$text${Console.RESET}"
    case Color.green => s"${Console.GREEN}$text${Console.RESET}"
    case Color.white => s"${Console.WHITE}$text${Console.RESET}"
    case Color.black => s"${Console.BLACK}$text${Console.RESET}"
    case Color.blue => s"${Console.BLUE}$text${Console.RESET}"
    case Color.none => s"${Console.YELLOW}$text${Console.RESET}"
  }
}