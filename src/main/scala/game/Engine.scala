package game

import akka.actor.typed.Behavior
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import game.*

import java.util.UUID

object Engine {

  private val commandHandler: (Battleground, Command) => ReplyEffect[Event, Battleground] = { (battleGround, command) =>
    command match {
      case Tap(replyTo, name) =>
        battleGround.lands.find(_._2.name == name) match {
          case Some(land) => Effect.persist(Tapped(land._1)).thenReply(replyTo)(_ => StatusReply.Success(s"$name tapped"))
          case None => Effect.none.thenReply(replyTo)(_ => StatusReply.Error(s"No $name found"))
        }
    }
  }

  private val eventHandler: (Battleground, Event) => Battleground = { (battleGround, event) =>
    event match {
      case Tapped(id) => battleGround.copy(lands = battleGround.lands.updatedWith(id)(_.map {
        case l@Land(_, _) => l.copy(state = State.Tapped)
        case x => x
      }))
    }
  }

  def apply(id: UUID, cards: Map[Int, Card]): Behavior[Command] =
    EventSourcedBehavior.withEnforcedReplies[Command, Event, Battleground](
      persistenceId = PersistenceId.ofUniqueId(id.toString),
      emptyState = Battleground(cards, 0),
      commandHandler = commandHandler,
      eventHandler = eventHandler,
    )
}