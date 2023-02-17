package interfaces

import akka.actor.typed.{ActorRef, Behavior, RecipientRef}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.pattern.StatusReply
import akka.util.Timeout
import cards.*
import cards.mana.*
import cards.types.LandType
import game.*
import org.jline.builtins.Completers.TreeCompleter.node
import org.jline.builtins.Completers.{OptDesc, OptionCompleter, TreeCompleter}
import org.jline.reader.{LineReader, LineReaderBuilder}
import org.jline.reader.impl.completer.{ArgumentCompleter, EnumCompleter, StringsCompleter}
import org.jline.terminal.{Terminal, TerminalBuilder}

import scala.concurrent.duration.*
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object CommandLine {

  trait Status

  case object Initiate extends Status
  case object Prepare extends Status
  case class Ready(activePlayer: PlayerId) extends Status
  case object Terminate extends Status

  private val terminal: Terminal = TerminalBuilder.terminal
  private var lineReader: LineReader = LineReaderBuilder.builder().terminal(terminal).build()

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
          val inputs = input.toLowerCase.split(" ").toList

          if inputs.head == "exit" then
            context.self ! Terminate
            Behaviors.same
          else
            val actionOpt = inputs match {
              case "read" :: _ :: Nil => Some(Recover.apply)
              case "play" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => PlayLand(ref, activePlayer, readIdFromArg(target)))
              case "cast" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Cast(ref, activePlayer, readIdFromArg(target)))
              case "use" :: target :: abilityId :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Use(ref, activePlayer, readIdFromArg(target), abilityId.toInt))
              case "pass" :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Pass(ref, activePlayer, None))
              case "pass" :: times :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Pass(ref, activePlayer, Some(times.toInt)))
              case "discard" :: target :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Discard(ref, activePlayer, readIdFromArg(target)))
              case _ => println("I don't understand your action"); context.self ! Ready(activePlayer); None
            }

            actionOpt.foreach { action =>
              println(s"$input")
              SendAction(context, cardboard, action, {
                case state: game.InProgressState if inputs.head == "read" => renderCard(state, readIdFromArg(inputs.tail.head)); Ready(state.playersTurn)
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

  private def refreshAutoComplete(state: InProgressState): Unit = {
    val collection: Map[CardId, Card] = state.battleField.view.mapValues(_.card).toMap ++ state.stack.view.mapValues(_.card).toMap
      ++ state.players.flatMap(player => player._2.hand ++ player._2.graveyard ++ player._2.exile).view

    lineReader = LineReaderBuilder.builder()
      .terminal(terminal)
      .completer(new TreeCompleter(
        node("read", node(collection.map(c => renderArg(c._1, c._2)).toList: _*)),
        node("play", node(state.players(state.priority).hand.filter(_._2.isInstanceOf[LandType]).map(c => renderArg(c._1, c._2)).toList: _*)),
        node("cast", node(state.players(state.priority).hand.filterNot(_._2.isInstanceOf[LandType]).map(c => renderArg(c._1, c._2)).toList: _*)),
        node("use", node(state.battleField.filter(_._2.owner == state.priority).map(c => renderArg(c._1, c._2.card)).toList: _*)),
        node("discard", node(state.players(state.priority).hand.map(c => renderArg(c._1, c._2)).toList: _*)),
        node("pass"),
        node("exit"),
      ))
      .build()
  }

  private def render(state: InProgressState): Unit = {
    refreshAutoComplete(state)

    println("\n")
    val playerOne = state.players.head
    val playerTwo = state.players.last
    renderPlayer(state, playerOne._1, playerOne._2)

    // TODO: Display summoning sickness
    // TODO: Show each types (Creatures, Artefacts, Enchantments, Planeswalkers) on a different line
    println(s"| ✋ Hand (${playerOne._2.hand.size}): ${playerOne._2.hand.map(p => renderName(p._1, p._2)).mkString(", ")}")
    println("|------------------")
    println(s"| 🌳Lands: ${state.battleField.filter(_._2.owner == playerOne._1).map(p => s"${renderName(p._1, p._2.card)}[${if p._2.status == Status.Untapped then " " else "T"}]").mkString(", ")}")
    println("\n")
    println(s"| 🌳Lands: ${state.battleField.filter(_._2.owner == playerTwo._1).map(p => s"${renderName(p._1, p._2.card)}[${if p._2.status == Status.Untapped then " " else "T"}]").mkString(", ")}")
    println("|------------------")
    println(s"| ✋ Hand (${playerTwo._2.hand.size}): ${playerTwo._2.hand.map(p => renderName(p._1, p._2)).mkString(", ")}")

    renderPlayer(state, playerTwo._1, playerTwo._2)

    println(s"| Phase: ${Phases.values.map(phase => if state.phase == phase then s"🌙$phase" else phase).mkString(", ")}")
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

  def renderArg(id: CardId, card: Card): String = s"${card.name.replace(" ", "_")}[$id]"
  def readIdFromArg(name: String): CardId = name.split("[\\[\\]]").last.toInt
  def renderName(id: CardId, card: Card): String = s"${terminalColor(card.color, card.name)}[$id]"

  def renderCard(state: InProgressState, target: CardId): Unit = {
    val collection: Map[CardId, Card] = state.battleField.view.mapValues(_.card).toMap ++ state.stack.view.mapValues(_.card).toMap
      ++ state.players.flatMap(player => player._2.hand ++ player._2.graveyard ++ player._2.exile).view

    collection.get(target) match {
      case None => println("Card not found")
      case Some(card) =>
        println(s"|Name: ${card.name}")
        println(s"|Subtype: ${card.subTypes.mkString(" - ")}")
        println(s"|Color: ${terminalColor(card.color, card.color.toString)}")
        println(s"|Cost: ${card.cost.toString}")
        println(s"|Preview: ${card.preview}")
        print("|Abilities:\n")
        card.activatedAbilities.foreach { (i, ability) =>
          println(s"|\t[$i]: [${ability.cost.toString}], ${ability.text}")
        }
    }
  }

  private def terminalColor(c: Color, text: String): String = c match {
    case Color.red => s"${Console.RED}$text${Console.RESET}"
    case Color.green => s"${Console.GREEN}$text${Console.RESET}"
    case Color.white => s"${Console.WHITE}$text${Console.RESET}"
    case Color.black => s"${Console.BLACK}$text${Console.RESET}"
    case Color.blue => s"${Console.BLUE}$text${Console.RESET}"
    case Color.none => s"${Console.YELLOW}$text${Console.RESET}"
  }
}