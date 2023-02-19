package cards

import cards.MagicSet.*
import cards.types.*

case class Deck(cards: List[Card], sideBoard: List[Card] = List.empty) {

  private val MIN_DECK_SIZE = 60

  def isValid: Boolean = cards.length >= MIN_DECK_SIZE
}

enum MagicSet(val code: String) {
  case AllWillBeOne extends MagicSet("one")
  case CoreSet2019 extends MagicSet("m19")
}

val standardDeck: Deck =
  Deck(
    (1 to 30).map(_ => Forest(AllWillBeOne, 276)).toList
      ++ (1 to 30).map(_ => LlanowarElf(CoreSet2019, 314))
  )