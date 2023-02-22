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

// TODO: Should cards in the library be a sort of instance too ? With ownership
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
  firstTurn: Boolean = true,
  damages: Int = 0,
) {
  def hasSummoningSickness: Boolean = card.isCreature && !card.keywordAbilities.contains(KeywordAbilities.haste) && firstTurn

  def power: Int = card.basePower.getOrElse(0)
  def toughness: Int = card.baseToughness.map(_ - damages).getOrElse(0)

  def tap: Permanent[T] = {
    this.copy(status = Status.Tapped)
  }

  def unTap: Permanent[T] = {
    this.copy(status = Status.Untapped)
  }
}

enum KeywordAbilities {
  case haste
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Land], name = "land"),
    new JsonSubTypes.Type(value = classOf[Creature], name = "creature"),
    new JsonSubTypes.Type(value = classOf[Instant], name = "instant"),
  )
)
// TODO: I think Cards should have an effect
abstract class Card {
  val name: String
  val subTypes: List[String]
  val color: Color
  // Some cards have no mana cost (Ex: Suspend)
  val cost: CastingCost
  val set: MagicSet
  val numberInSet: Int

  def keywordAbilities: List[KeywordAbilities] = List()
  def activatedAbilities: Map[Int, Ability]
  def checkConditions(state: BoardState, player: PlayerId): Try[Unit]

  def isCreature: Boolean = {
    isInstanceOf[Creature] || subTypes.contains("Creature") || subTypes.contains("Legendary Creature")
  }

  def preview: URL = {
    new URL(s"https://scryfall.com/card/${set.code}/$numberInSet/${name.replace("-", "").toLowerCase}")
  }
}

abstract class PermanentCard extends Card {
  val basePower: Option[Int]
  val baseToughness: Option[Int]
}
