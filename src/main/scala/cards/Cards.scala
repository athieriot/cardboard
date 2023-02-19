package cards

import cards.mana.*
import cards.types.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*
import cards.types.*

import java.net.URL
import scala.util.Try

enum Status {
  case Tapped, Untapped
}

case class Spell[T <: Card](
  card: T,
  owner: String,
  controller: String,
)

case class Permanent[T <: PermanentCard](
  card: T,
  owner: String,
  controller: String,
  status: Status = Status.Untapped,
  firstTurn: Boolean = true
) {
  // TODO: Check for Haste
  def hasSummoningSickness: Boolean = card.isCreature && firstTurn

  def tap: Permanent[T] = {
    this.copy(status = Status.Tapped)
  }

  def unTap: Permanent[T] = {
    this.copy(status = Status.Untapped)
  }
}

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
  // TODO: Some cards have no mana cost (Ex: Suspend)
  val cost: CastingCost
  val set: MagicSet
  val numberInSet: Int

  def activatedAbilities: Map[Int, Ability]
  def checkConditions(state: BoardState, player: PlayerId): Try[Unit]

  // TODO: Or has toughness ?
  def isCreature: Boolean = {
    isInstanceOf[Creature] || subTypes.contains("Creature") || subTypes.contains("Legendary Creature")
  }

  def preview: URL = {
    new URL(s"https://scryfall.com/card/${set.code}/$numberInSet/${name.replace("-", "").toLowerCase}")
  }
}

abstract class PermanentCard extends Card {
  val basePowerToughness: Option[(Int, Int)]
}
