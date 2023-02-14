package game

import cards.*
import game.*

sealed trait Event

// TODO: Mulligan
final case class Created(die: Int, players: Map[String, Deck]) extends Event

final case class Moved(phase: Phase) extends Event

final case class Played(target: CardId, player: String) extends Event

final case class Drawn(count: Int, player: String) extends Event
final case class Shuffled(order: List[Int], player: String) extends Event
final case class Discarded(target: Option[CardId], player: String) extends Event

final case class Tapped(target: CardId, player: String) extends Event