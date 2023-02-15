package cards

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*

import java.net.URL

enum Color {
  case red, green, black, white, blue, none
}

case class Deck(cards: List[Card], sideBoard: List[Card] = List.empty) {

  private val MIN_DECK_SIZE = 40

  def isValid: Boolean = cards.length >= MIN_DECK_SIZE
}

trait Cost {
  def canPay(target: Instance): Boolean
  def pay(target: CardId, player: PlayerId): List[Event]
}
case object Tap extends Cost {
  def canPay(target: Instance): Boolean = target.status == Status.Untapped
  def pay(target: CardId, player: PlayerId): List[Event] = List(Tapped(target))
}
case class ManaCost(text: String) extends Cost {
  def canPay(target: Instance): Boolean = ???
  def pay(target: CardId, player: PlayerId): List[Event] = ???
}


case class Ability(cost: Cost, effect: (InProgressState, String) => List[Event])




enum Type {
  case artifact, creature, enchantment, instant, land, planesWalker, sorcery
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[LandType], name = "landType"),
  )
)
trait Card {
  val name: String
  val subTypes: List[String]
  val color: Color
  val manaCost: Option[ManaCost]
  val preview: URL
  def activatedAbilities: Map[Int, Ability]
}

val standardDeck: Deck = Deck((1 to 8).flatMap(_ => basicLands).toList)
