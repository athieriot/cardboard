package cards

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*

case class Deck(cards: List[Card]) {

  private val MIN_DECK_SIZE = 40

  def isValid: Boolean = cards.length >= MIN_DECK_SIZE
}

enum Color {
  case red, green, black, white, blue
}

enum Type {
  case land
}

trait Cost

case object Tapping extends Cost
case class ManaCost() extends Cost

case class Ability(cost: Cost, effect: (InProgressState, String) => InProgressState)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Land], name = "land"),
  )
)
trait Card {
  val name: String
  def activatedAbilities: Map[String, Ability]
}

case class Land(
  name: String,
) extends Card {
  def activatedAbilities: Map[String, Ability] = {
    Map(
      "tap" -> Ability(Tapping, (state, player) => state.focus(_.players.index(player).manaPool.index(Color.red)).modify(_ + 1))
    )
  }
}

// TODO: Should it generate commands/events ?
// TODO: Should use Pattern Matching
val forest = Land("Forest")
