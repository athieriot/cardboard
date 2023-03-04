package game.cards

import game.cards.types.*
import game.cards

case class Deck(cards: List[Card], sideBoard: List[Card] = List.empty) {
  private val MIN_DECK_SIZE = 60

  def isValid: Boolean = cards.length >= MIN_DECK_SIZE
}

val mountain = collection.fourthEdition.Mountain()
val monssGoblinRaiders = collection.fourthEdition.MonssGoblinRaiders()
val greenDeck: Deck =
  cards.Deck(
    (1 to 30).map(_ => collection.fourthEdition.Forest()).toList
      ++ (1 to 15).map(_ => collection.fourthEdition.LlanowarElf())
      ++ (1 to 15).map(_ => collection.fourthEdition.WarMammoth())
  )

val blueDeck: Deck =
  cards.Deck(
    (1 to 24).map(_ => collection.fourthEdition.Island()).toList
      ++ (1 to 12).map(_ => collection.fourthEdition.AnkhOfMishra())
      ++ (1 to 12).map(_ => collection.fourthEdition.Counterspell())
      ++ (1 to 12).map(_ => collection.fourthEdition.ProdigalSorcerer())
  )