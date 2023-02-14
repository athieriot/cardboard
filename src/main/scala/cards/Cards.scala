package cards

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*

case class Deck(cards: List[Card], sideBoard: List[Card] = List.empty) {

  private val MIN_DECK_SIZE = 40

  def isValid: Boolean = cards.length >= MIN_DECK_SIZE
}

enum Color {
  case red, green, black, white, blue, none
}

enum Type {
  case artifact, creature, enchantment, instant, land, planeswalker, sorcery
}

trait Cost

case object Tapping extends Cost
case class ManaCost() extends Cost

case class Ability(cost: Cost, effect: (InProgressState, String) => InProgressState)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[LandType], name = "landType"),
  )
)
trait Card {
  val name: String
  def activatedAbilities: Map[String, Ability]
}

val standardDeck: Deck = Deck((1 to 8).flatMap(_ => basicLands).toList)
