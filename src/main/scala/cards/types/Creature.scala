package cards.types

import cards.{CardState, *}
import cards.mana.{ManaCost, *}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.{Context, *}
import game.mechanics.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.{Success, Try}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[LlanowarElf], name = "llanowarElf"),
    new JsonSubTypes.Type(value = classOf[ProdigalSorcerer], name = "prodigalSorcerer"),
    new JsonSubTypes.Type(value = classOf[MonssGoblinRaiders], name = "monssGoblinRaiders"),
    new JsonSubTypes.Type(value = classOf[WarMammoth], name = "warMammoth"),
  )
)
sealed abstract class Creature extends PermanentCard {
  // TODO: I think it has to be a list of Conditions
  // TODO: Split catching condition to be able to override some of them
  override def conditions(ctx: Context): Try[Unit] = Try {
    if ctx.state.activePlayer != ctx.player then
      throw new RuntimeException("You can only play creatures during your turn")
    else if !ctx.state.currentStep.isMain then
      throw new RuntimeException("You can only play creatures during a main phase")
    else if ctx.state.stack.nonEmpty then
      throw new RuntimeException("You can only play creatures when the stack is empty")
  }
  
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(EnteredTheBattlefield(id))
}

// Green
class LlanowarElf(val set: MagicSet, val numberInSet: Int) extends Creature {
  val name: String = "Llanowar Elf"
  val subTypes: List[String] = List("Creature", "Elf Druid")
  val color: Color = Color.green
  val cost: Cost = ManaCost("G")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)

  override def activatedAbilities: Map[Int, ActivatedAbility] = Map(
    1 -> ActivatedAbility(new Tap(), "Add one green mana", (_, ctx: Context, _) => List(ManaAdded(Map(Color.green -> 1), ctx.player)), manaAbility = true)
  )
}

class WarMammoth(val set: MagicSet, val numberInSet: Int) extends Creature {
  val name: String = "War Mammoth"
  val subTypes: List[String] = List("Creature", "Elephant")
  val color: Color = Color.green
  val cost: Cost = ManaCost("3G")
  val basePower: Option[Int] = Some(3)
  val baseToughness: Option[Int] = Some(3)

  override def keywordAbilities: List[KeywordAbilities] = List(KeywordAbilities.trample)
}

// Blue
class ProdigalSorcerer(val set: MagicSet, val numberInSet: Int) extends Creature {
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

// Red
class MonssGoblinRaiders(val set: MagicSet, val numberInSet: Int) extends Creature {
  val name: String = "Mons's Goblin Raiders"
  val subTypes: List[String] = List("Creature", "Goblin")
  val color: Color = Color.red
  val cost: Cost = ManaCost("R")
  val basePower: Option[Int] = Some(1)
  val baseToughness: Option[Int] = Some(1)
}