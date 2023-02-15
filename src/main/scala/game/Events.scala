package game

import cards.*
import game.*

sealed trait Event

// TODO: Mulligan
final case class Created(die: Int, players: Map[String, Deck]) extends Event

final case class Moved(phase: Phase) extends Event
case object ManaPoolEmptied extends Event
final case class ManaAdded(mana: Map[Color, Int], player: PlayerId) extends Event

final case class LandPlayed(target: CardId, player: PlayerId) extends Event

final case class Drawn(amount: Int, player: PlayerId) extends Event
final case class Shuffled(order: List[Int], player: PlayerId) extends Event
final case class Discarded(target: CardId, player: PlayerId) extends Event

final case class Tapped(target: CardId) extends Event