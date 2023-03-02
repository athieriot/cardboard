package game.mana

import game.cards.*
import game.cards.types.*
import monocle.syntax.all.*

import scala.annotation.targetName
import scala.util.{Failure, Success, Try}

// TODO: Phyrexian Mana !
enum Color {
  case red, green, black, white, blue, colorless
}
object Color {
  def colorFrom(c: Char): Option[Color] = c match {
    case 'G' => Some(Color.green)
    case 'B' => Some(Color.black)
    case 'W' => Some(Color.white)
    case 'R' => Some(Color.red)
    case 'U' => Some(Color.blue)
    case 'L' => Some(Color.colorless)
    case _ => None
  }
}

case class ManaPool(pool: Map[Color, Int] = Color.values.map((_, 0)).toMap) {
  @targetName("add")
  def +(mana: (Color, Int)): ManaPool = this.focus(_.pool.index(mana._1)).modify(_ + mana._2)

  @targetName("adds")
  def ++(manas: Map[Color, Int]): ManaPool = manas.foldLeft(this)(_+_)

  // TODO: Validate valid ManaCost
  @targetName("minusText")
  def -(cost: ManaCost): Try[ManaPool] = Try {
    cost.text.foldRight(this) { (c, manaPool) =>
      manaPool - (Color.colorFrom(c), if c.isDigit then c.asDigit else 1) match {
        case Success(value) => value
        case Failure(ex) => throw ex
      }
    }
  }

  @targetName("minus")
  def -(mana: (Option[Color], Int)): Try[ManaPool] = Try {
    (1 to mana._2).foldLeft(this) { case (manaPool, _) =>
      mana._1 match {
        case Some(color) if manaPool.pool.getOrElse(color, 0) > 0 => manaPool.focus(_.pool.index(color)).modify(_ - 1)
        case Some(_) => throw new RuntimeException("No enough mana in your mana pool")
        case None =>
          manaPool.pool.find(_._2 > 0) match {
            case Some((color, _)) => manaPool.focus(_.pool.index(color)).modify(_ - 1)
            case None => throw new RuntimeException("No enough mana in your mana pool")
          }
      }
    }
  }
}

object ManaPool {
  def empty(): ManaPool = ManaPool()
}