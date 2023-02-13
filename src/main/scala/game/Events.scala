package game

import cards.Card
import game.*

sealed trait Event

final case class Created(die: Int, decks: Map[String, List[Card]]) extends Event

final case class Drawn(count: Int, player: String) extends Event

final case class Discarded(id: Option[Int], player: String) extends Event

final case class Tapped(id: Int, player: String) extends Event