package cards

import cards.mana.*
import cards.types.{Creature, LandType}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*
import cards.types.*

import java.net.URL

case class Deck(cards: List[Card], sideBoard: List[Card] = List.empty) {

  private val MIN_DECK_SIZE = 60

  def isValid: Boolean = cards.length >= MIN_DECK_SIZE
}

case class Ability(cost: AbilityCost, text: String, effect: (BoardState, String) => List[Event])

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[LandType], name = "landType"),
    new JsonSubTypes.Type(value = classOf[Creature], name = "creature"),
  )
)
trait Card {
  val name: String
  val subTypes: List[String]
  val color: Color
  // TODO: Some cards have no mana cost (Ex: Miracle)
  val cost: CastingCost
  val preview: URL
  def activatedAbilities: Map[Int, Ability]
}
