package game

import game.*

sealed trait Event

final case class Tapped(name: Int) extends Event