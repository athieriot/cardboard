package cards

import cards.mana.*
import cards.types.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*
import cards.types.*
import game.mechanics.*

import java.net.URL
import scala.util.Try

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Land], name = "land"),
    new JsonSubTypes.Type(value = classOf[Creature], name = "creature"),
    new JsonSubTypes.Type(value = classOf[Instant], name = "instant"),
  )
)
abstract class Card {
  val name: String
  val subTypes: List[String]
  val color: Color
  // Some cards have no mana cost (Ex: Suspend)
  val cost: CastingCost
  val set: MagicSet
  val numberInSet: Int

  def checkCastingConditions(ctx: Context): Try[Unit] =
    Try(if !cost.canPay(ctx.state, ctx.player) then
      throw new RuntimeException("Cannot pay the cost"))

  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event]

  // TODO: Maybe all those should be on the CardState classes
  def keywordAbilities: List[KeywordAbilities] = List.empty
  def activatedAbilities: Map[Int, Ability] = Map.empty

  def isLand: Boolean     = isInstanceOf[Land] || subTypes.contains("Land") || subTypes.contains("Legendary Land")
  def isCreature: Boolean = isInstanceOf[Creature] || subTypes.contains("Creature") || subTypes.contains("Legendary Creature")

  def preview: URL = {
    new URL(s"https://scryfall.com/card/${set.code}/$numberInSet/${name.replace(" ", "-").replace("'", "").toLowerCase}")
  }
}

abstract class PermanentCard extends Card {
  val basePower: Option[Int]
  val baseToughness: Option[Int]
}
