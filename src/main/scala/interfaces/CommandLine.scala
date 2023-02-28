package interfaces

import akka.actor.typed.{ActorRef, Behavior, RecipientRef}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.pattern.StatusReply
import akka.util.Timeout
import cards.*
import cards.mana.*
import cards.types.*
import game.*
import game.mechanics.*
import interfaces.CommandLine.renderName
import org.jline.builtins.Completers.TreeCompleter.node
import org.jline.builtins.Completers.{OptDesc, OptionCompleter, TreeCompleter}
import org.jline.reader.{LineReader, LineReaderBuilder}
import org.jline.reader.impl.completer.{ArgumentCompleter, EnumCompleter, StringsCompleter}
import org.jline.terminal.{Terminal, TerminalBuilder}

import scala.annotation.unused
import scala.concurrent.duration.*
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object CommandLine {

  trait Status

  case object Initiate extends Status
  case object Prepare extends Status
  case class Ready(priority: PlayerId) extends Status
  case object Terminate extends Status

  private val terminal: Terminal = TerminalBuilder.terminal
  private var lineReader: LineReader = LineReaderBuilder.builder().terminal(terminal).build()
  
  private def parseArg(x: String): Option[Arg[_]] = {
    x.replaceAll("-", "").split("=").toList match {
      case "target" :: y :: Nil => Some(TargetArg(y.toIntOption.getOrElse(y)))
      case _ => None
    }
  }
  
  def apply(instance: Behavior[Action]): Behavior[Status] =
    Behaviors.setup[CommandLine.Status] { context =>
      implicit val timeout: Timeout = 3.seconds

      val cardboard = context.spawn(instance, "game")

      Behaviors.receiveMessage[CommandLine.Status] {
        case Terminate => Behaviors.stopped
        case Initiate =>
          SendAction(context, cardboard, Recover.apply, {
            case game.EmptyState => println("No existing game found"); Prepare
            case game.EndState(loser) => println(s"Game already ended. $loser lost"); Terminate
            case state: game.BoardState => render(state); Ready(state.priority)
          })

        case Prepare =>
          val playerOne = lineReader.readLine("Player One: ")
          val playerTwo = lineReader.readLine("Player Two: ")

          val players = Map(
            playerOne -> greenDeck,
            playerTwo -> blueDeck
          )

          SendAction(context, cardboard, ref => New(ref, players), {
            case state: game.BoardState => render(state); Ready(state.priority)
            case state => println(s"Wrong state $state"); Terminate
          })

        case Ready(priority) =>
          val input = lineReader.readLine(s"|$priority|> ")
          val inputs = input.split(" ").toList

          if inputs.head == "exit" then
            context.self ! Terminate
            Behaviors.same
          else
            // TODO: "cmd+n" for next ?
            // TODO: Skip only in cli: automate phase/resolution if not exceptions
            val actionOpt = inputs.head.toLowerCase +: inputs.tail match {
              case "read" :: _ :: Nil => Some(Recover.apply)
              case "play" :: id :: args => Some((ref: ActorRef[StatusReply[State]]) => PlayLand(ref, priority, readIdFromArg(id), args.flatMap(parseArg)))
              case "cast" :: id :: args => Some((ref: ActorRef[StatusReply[State]]) => Cast(ref, priority, readIdFromArg(id), args.flatMap(parseArg)))
              case "attack" :: id :: Nil => Some((ref: ActorRef[StatusReply[State]]) => DeclareAttacker(ref, priority, readIdFromArg(id)))
              case "block" :: id :: block :: Nil => Some((ref: ActorRef[StatusReply[State]]) => DeclareBlocker(ref, priority, readIdFromArg(id), readIdFromArg(block)))
              case "activate" :: id :: abilityId :: args => Some((ref: ActorRef[StatusReply[State]]) => Activate(ref, priority, readIdFromArg(id), abilityId.toInt, args.flatMap(parseArg)))
              case "n" :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Next(ref, priority))
              case "next" :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Next(ref, priority))
              case "resolve" :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Resolve(ref, priority))
              case "end" :: Nil => Some((ref: ActorRef[StatusReply[State]]) => EndTurn(ref, priority))
              case "discard" :: id :: Nil => Some((ref: ActorRef[StatusReply[State]]) => Discard(ref, priority, readIdFromArg(id)))
              case _ => println("I don't understand your action"); context.self ! Ready(priority); None
            }

            actionOpt.foreach { action =>
              println(s"$input")
              SendAction(context, cardboard, action, {
                case state: game.BoardState if inputs.head == "read" => renderCard(state, readIdFromArg(inputs.tail.head)); Ready(state.priority)
                case state: game.BoardState => render(state); Ready(state.priority)
                case game.EndState(loser) => println(s"Good game ! But $loser did not win"); Terminate
                case state => println(s"Wrong state $state"); Terminate
              }, {
                text => println(text); Ready(priority)
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
    onError: String => CommandLine.Status = {message => println(s"An error occurred $message"); Terminate},
  )(implicit responseTimeout: Timeout, @unused classTag: ClassTag[State]): Behavior[CommandLine.Status] = {
    context.askWithStatus(target, request) {
      case Success(state) => onSuccess(state)
      case Failure(StatusReply.ErrorMessage(text)) => onError(text)
      case Failure(ex) => onError(ex.getLocalizedMessage)
    }
    Behaviors.same
  }

  private def buildCollection(state: BoardState): Map[CardId, CardState[Card]] =
    state.listCardsFromZone(Battlefield) ++ state.listCardsFromZone(Stack)
      ++ state.players.flatMap(player => state.listCardsFromZone(Hand(player._1)) ++ state.listCardsFromZone(Graveyard(player._1)) ++ state.listCardsFromZone(Exile(player._1)))

  private def refreshAutoComplete(state: BoardState): Unit = {

    val collection = buildCollection(state)
    lineReader = LineReaderBuilder.builder()
      .terminal(terminal)
      .completer(new TreeCompleter(
        node("read", node(collection.map(c => renderArg(c._1, c._2.card)).toList: _*)),
        if !state.listCardsFromZone(Hand(state.priority)).exists(_._2.card.isLand) then node("play") else node("play", node(state.listCardsFromZone(Hand(state.priority)).filter(_._2.card.isLand).map(c => renderArg(c._1, c._2.card)).toList: _*)),
        if state.listCardsFromZone(Hand(state.priority)).exists(_._2.card.isLand) then node("cast") else node("cast", node(state.listCardsFromZone(Hand(state.priority)).filterNot(_._2.card.isLand).map(c => renderArg(c._1, c._2.card)).toList: _*)),
        if state.potentialAttackers(state.priority).isEmpty then node("attack") else node("attack", node(state.potentialAttackers(state.priority).map(c => renderArg(c._1, c._2.card)).toList: _*)),
        // TODO: TreeCompleter for blockers
        // TODO: ArgCompleter for arg targets
        if !state.battleField.exists(_._2.owner == state.priority) then node("activate") else node("activate", node(state.battleField.filter(_._2.owner == state.priority).map(c => renderArg(c._1, c._2.card)).toList: _*)),
        node("discard", node(state.players(state.priority).hand.map(c => renderArg(c._1, c._2.card)).toList: _*)),
        node("next"),
        node("end"),
        node("exit"),
      ))
      .build()
  }

  private def render(state: BoardState): Unit = {
    refreshAutoComplete(state)

    println("\n")
    val playerOne = state.players.head
    val playerTwo = state.players.last
    renderPlayer(state, playerOne._1, playerOne._2)

    // TODO: Show each types (Artefacts, Enchantments, Planeswalkers) on a different line
    println(s"| ✋ Hand (${playerOne._2.hand.size}): ${playerOne._2.hand.map(p => renderName(p._1, p._2.card)).mkString(", ")}")
    println("|------------------")
    println(s"| 🌳 Lands: ${state.battleField.filter(_._2.controller == playerOne._1).filter(_._2.card.isLand).map(p => s"${renderName(p._1, p._2.card)}${renderStatus(p._2)}").mkString(", ")}")
    println(s"| 🦁 Creatures: ${state.battleField.filter(_._2.controller == playerOne._1).filter(_._2.card.isCreature).map(p => s"${renderName(p._1, p._2.card)}${renderStatus(p._2)}${renderSummoningSickness(p._2)}").mkString(", ")}")
    println("|")
    if state.stack.nonEmpty then println(s"| 🎴Stack: ${state.stack.map(p => renderName(p._1, p._2.card)).mkString(", ")}")
    if state.attackers.nonEmpty then println(s"| ⚠ Attack: ${state.attackers.map(p => s"${renderName(p._1, p._2._1.card)}->${p._2._2.map(b => renderName(b._1, b._2.card)).mkString(", ")}->${p._2._1.attacking.get}").mkString(", ")}")
    println("|")
    println(s"| 🦁 Creatures: ${state.battleField.filter(_._2.controller == playerTwo._1).filter(_._2.card.isCreature).map(p => s"${renderName(p._1, p._2.card)}${renderStatus(p._2)}${renderSummoningSickness(p._2)}").mkString(", ")}")
    println(s"| 🌳 Lands: ${state.battleField.filter(_._2.controller == playerTwo._1).filter(_._2.card.isLand).map(p => s"${renderName(p._1, p._2.card)}${renderStatus(p._2)}").mkString(", ")}")
    println("|------------------")
    println(s"| ✋ Hand (${playerTwo._2.hand.size}): ${playerTwo._2.hand.map(p => renderName(p._1, p._2.card)).mkString(", ")}")

    renderPlayer(state, playerTwo._1, playerTwo._2)

    println(s"| Phase: ${Step.values.map(phase => if state.currentStep == phase then s"🌙$phase" else phase).mkString(", ")}")
    println("|------------------")
    println("\n")
  }

  private def renderPlayer(state: BoardState, name: String, playerState: PlayerState): Unit = {
    val active = if state.activePlayer == name then "⭐ " else ""
    val coloredName = if state.priority == name then s"${Console.RED_B}$name${Console.RESET}" else name
    val priority = if state.priority == name then " 🟢" else ""

    println("|------------------")
    println(s"| Player: $active$coloredName$priority - Life: ${playerState.life}")
    println(s"| 📚 Library: ${playerState.library.size}")
    println(s"| 🪦  Graveyard (${playerState.graveyard.size}): ${playerState.graveyard.map(p => renderName(p._1, p._2.card)).mkString(", ")}")
    println(s"| 🪄  Mana: ${playerState.manaPool.pool.map(m => terminalColor(m._1, s"${m._1} (${m._2})")).mkString(" / ")}")
    println("|------------------")
  }

  private def renderArg(id: CardId, card: Card): String = s"${card.name.replace(" ", "_")}[$id]"
  private def readIdFromArg(name: String): CardId = name.split("[\\[\\]]").last.toInt
  private def renderName(id: CardId, card: Card): String = s"${terminalColor(card.color, card.name)}[$id]"
  private def renderStatus(permanent: Permanent[PermanentCard]): String = if permanent.status == Status.Tapped then "[T]" else ""
  private def renderSummoningSickness(permanent: Permanent[PermanentCard]): String = if permanent.card.isCreature && permanent.hasSummoningSickness then s"[S]${renderPowerToughness(permanent)}" else renderPowerToughness(permanent)
  private def renderPowerToughness(permanent: Permanent[PermanentCard]): String = if permanent.card.isCreature then s"(${permanent.power}/${permanent.toughness})" else ""

  private def renderCard(state: BoardState, id: CardId): Unit = {

    buildCollection(state).get(id) match {
      case None => println("Card not found")
      case Some(card) =>
        println(s"|Name: ${card.card.name}")
        println(s"|Subtype: ${card.card.subTypes.mkString(" - ")}")
        println(s"|Color: ${terminalColor(card.card.color, card.card.color.toString)}")
        println(s"|Cost: ${card.card.cost.toString}")
        println(s"|Text: ${card.card.text}")
        println(s"|Keywords: ${card.card.keywordAbilities.map(_.toString).mkString(", ")}")
        println(s"|Preview: ${card.card.preview}")
        print("|Abilities:\n")
        card.card.activatedAbilities.foreach { (i, ability) =>
          println(s"|\t[$i]: [${ability.cost.toString}], ${ability.text}")
        }

        card.card match {
          case permanent: PermanentCard => s"(${permanent.basePower.getOrElse(0)}/${permanent.baseToughness.getOrElse(0)})"
          case _ =>
        }
    }
  }

  private def terminalColor(c: Color, text: String): String = c match {
    case Color.red => s"${Console.RED}$text${Console.RESET}"
    case Color.green => s"${Console.GREEN}$text${Console.RESET}"
    case Color.white => s"${Console.WHITE}${Console.BLACK_B}$text${Console.RESET}"
    case Color.black => s"${Console.BLACK}${Console.WHITE_B}$text${Console.RESET}"
    case Color.blue => s"${Console.BLUE}$text${Console.RESET}"
    case Color.none => s"${Console.YELLOW}$text${Console.RESET}"
  }
}