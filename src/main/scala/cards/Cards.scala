package cards

import cards.mana.*
import cards.types.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*
import cards.types.*

import java.net.URL
import scala.util.Try

case class Deck(cards: List[Card], sideBoard: List[Card] = List.empty) {

  private val MIN_DECK_SIZE = 60

  def isValid: Boolean = cards.length >= MIN_DECK_SIZE
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
  val set: String // TODO: Could be an enum
  val numberInSet: Int
  
  def activatedAbilities: Map[Int, Ability]
  def checkConditions(state: BoardState, player: PlayerId): Try[Unit]
  
  def preview: URL = {
    new URL(s"https://scryfall.com/card/$set/$numberInSet/${name.replace("-", "").toLowerCase}")
  }
}
