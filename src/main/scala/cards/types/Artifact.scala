package cards.types

import cards.mana.*
import cards.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.mechanics.*
import game.*

import scala.util.{Success, Try}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[AnkhOfMishra], name = "ankhOfMishra"),
  )
)
sealed abstract class Artifact extends PermanentCard {
  override def conditions(ctx: Context): Try[Unit] = Try {
    if ctx.state.activePlayer != ctx.player then
      throw new RuntimeException("You can only play artifacts during your turn")
    else if !ctx.state.currentStep.isMain then
      throw new RuntimeException("You can only play artifacts during a main phase")
    else if ctx.state.stack.nonEmpty then
      throw new RuntimeException("You can only play artifacts when the stack is empty")
  }

  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(EnteredTheBattlefield(id))
}

class AnkhOfMishra(val set: MagicSet, val numberInSet: Int) extends Artifact {
  val name: String = "Ankh of Mishra"
  val subTypes: List[String] = List("Artifact")
  val color: Color = Color.none
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