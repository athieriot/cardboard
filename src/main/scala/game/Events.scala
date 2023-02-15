package game

import cards.*
import game.*

sealed trait Event

// TODO: Mulligan
final case class Created(die: Int, players: Map[String, Deck]) extends Event

final case class Moved(phase: Phase) extends Event
case object ManaPoolEmptied extends Event

final case class LandPlayed(target: CardId, player: String) extends Event

final case class Drawn(amount: Int, player: String) extends Event
final case class Shuffled(order: List[Int], player: String) extends Event
final case class Discarded(target: CardId, player: String) extends Event

final case class Tapped(target: CardId, player: String) extends Event