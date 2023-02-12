package game

import game.*

sealed trait Event

final case class Drawn(count: Int, player: String) extends Event

final case class Tapped(id: Int, player: String) extends Event