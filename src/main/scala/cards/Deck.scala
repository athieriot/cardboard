package cards

import cards.MagicSet.*
import cards.types.*

case class Deck(cards: List[Card], sideBoard: List[Card] = List.empty) {
  private val MIN_DECK_SIZE = 60

  def isValid: Boolean = cards.length >= MIN_DECK_SIZE
}

enum MagicSet(val code: String) {
  case FourthEdition extends MagicSet("4ed")
}

val mountain = Mountain(FourthEdition, 373)
val monssGoblinRaiders = MonssGoblinRaiders(FourthEdition, 213)
val greenDeck: Deck =
  Deck(
    (1 to 30).map(_ => Forest(FourthEdition, 376)).toList
      ++ (1 to 15).map(_ => LlanowarElf(FourthEdition, 261))
      ++ (1 to 15).map(_ => WarMammoth(FourthEdition, 286))
  )

val blueDeck: Deck =
  Deck(
    (1 to 24).map(_ => Island(FourthEdition, 367)).toList
      ++ (1 to 12).map(_ => AnkhOfMishra(FourthEdition, 294))
      ++ (1 to 12).map(_ => Counterspell(FourthEdition, 65))
      ++ (1 to 12).map(_ => ProdigalSorcerer(FourthEdition, 94))
  )