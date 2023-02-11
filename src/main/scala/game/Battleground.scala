package game

enum State:
  case Tapped, Untapped

trait Card {
  val name: String
  val state: State
}

case class Land(name: String, state: State) extends Card

case class Battleground(lands: Map[Int, Card],
                        manaPool: Int)