package collection.fourthEdition

import collection.sets.FourthEdition
import game.*
import game.cards.*
import game.cards.*
import game.cards.types.*
import game.mana.*
import game.mechanics.*

import scala.util.Success

class AnkhOfMishra(val numberInSet: Int = 294) extends Artifact with FourthEdition {
  val name: String = "Ankh of Mishra"
  val subTypes: List[String] = List("Artifact")
  val color: Color = Color.colorless
  val cost: Cost = ManaCost("2")
  val basePower: Option[Int] = None
  val baseToughness: Option[Int] = None

  override val triggeredAbilities: Map[Int, TriggeredAbility] = Map(
    1 -> TriggeredAbility(
      s"Whenever a land enters the battlefield, Ankh of Mishra deals 2 damage to that land’s controller",
      effects = (_, ctx: Context, cardState: CardState[Card]) =>
        cardState match {
          case Spell(_, _, _, args) =>
            Args.retrieveTarget(args) match {
              case Success(cardId: CardId) =>
                ctx.state.getCardController(cardId).map(controller => List(DamageDealt(controller, 2))).getOrElse(List.empty)
              case _ => List()
            }
          case _ => List()
        },
      // TODO: Or register triggers instead of this ?
      triggers = {
        case LandPlayed(id, _) => Some(TargetArg(id))
        case _ => None
      }
    )
  )
}