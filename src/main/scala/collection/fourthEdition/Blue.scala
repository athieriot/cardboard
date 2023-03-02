package collection.fourthEdition

import collection.sets.FourthEdition
import game.cards.*
import game.cards.types.*
import game.*
import game.mana.*
import game.mechanics.*

import scala.util.{Success, Try}

class ProdigalSorcerer(val numberInSet: Int = 94) extends Creature with FourthEdition {
  val name: String = "Prodigal Sorcerer"
  val subTypes: List[String] = List("Creature", "Human Wizard")
  val color: Color = Color.blue
  val cost: Cost = ManaCost("2U")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)

  override def activatedAbilities: Map[Int, ActivatedAbility] = Map(
    1 -> ActivatedAbility(new Tap(), "Prodigal Sorcerer deals 1 damage to any target",
      effects = (_: CardId, ctx: Context, cardState: CardState[Card]) =>
        cardState match {
          case Spell(_, _, _, args) =>
            Args.retrieveTarget(args) match {
              case Success(playerId: PlayerId) => ctx.state.players.get(playerId).map(_ => List(DamageDealt(playerId, 1))).getOrElse(List())
              case Success(cardId: CardId) => ctx.state.getCardFromZone(cardId, Battlefield).map(_ => List(DamageDealt(cardId, 1))).getOrElse(List())
              case _ => List()
            }
          case _ => List()
        },
      conditions = (ctx: Context) => Try {
        Args.retrieveTarget(ctx.args).flatMap(target => Try {
          target match {
            case playerId: PlayerId => ctx.state.players.getOrElse(playerId, throw new RuntimeException("Invalid Target player"))
            case cardId: CardId => ctx.state.getCardFromZone(cardId, Battlefield).getOrElse(throw new RuntimeException("Target invalid, it has to be in the Battlefield"))
          }
        })
      }
    )
  )
}

class Counterspell(val numberInSet: Int = 65) extends Instant with FourthEdition {
  val name: String = "Counterspell"
  val subTypes: List[String] = List("Instant")
  val color: Color = Color.blue
  val cost: Cost = ManaCost("UU")

  override def text: String = "Counter target spell"

  override def conditions(ctx: Context): Try[Unit] = super.conditions(ctx).flatMap { _ =>
    Args.retrieveTarget(ctx.args).flatMap(target => Try { target match {
      case _: PlayerId => throw new RuntimeException("Target invalid, cannot be a player")
      case cardId: CardId =>
        ctx.state.getCardFromZone(cardId, Stack).filterNot(_.card.isToken).getOrElse(throw new RuntimeException("Target invalid, it has to be a card on the Stack"))
    }})
  }

  override def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = super.effects(id, ctx, cardState) ++ (
    cardState match {
      case Spell(_, _, _, args) =>
        Args.retrieveTarget(args) match {
          case Success(cardId: CardId) =>
            ctx.state.getCardFromZone(cardId, Stack).filterNot(_.card.isToken).map(_ => List(PutIntoGraveyard(cardId, ctx.player))).getOrElse(List())
          case _ => List()
        }
      case _ => List()
    }
    )
}