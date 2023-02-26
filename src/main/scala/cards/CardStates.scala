package cards

import cards.types.{Creature, Instant, Land}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.{Arg, CardId, Context, Event}
import game.mechanics.KeywordAbilities

enum Status {
  case Tapped, Untapped
}

abstract class CardState[+T] { val card: T; val owner: String }
case class UnPlayed[T <: Card](card: T, owner: String) extends CardState[T]

case class Spell[T <: Card](
  card: T,
  owner: String,
  controller: String,
  args: List[Arg[_]] = List.empty
) extends CardState[T] {

  def effects(id: CardId, ctx: Context): List[Event] = card.effects(id, ctx, this)
}

case class Permanent[T <: PermanentCard](
  card: T,
  owner: String,
  controller: String,
  status: Status = Status.Untapped,
  firstTurn: Boolean = true,
  damages: Int = 0,
  args: List[Arg[_]] = List.empty
) extends CardState[T] {
  def hasSummoningSickness: Boolean = card.isCreature && !card.keywordAbilities.contains(KeywordAbilities.haste) && firstTurn

  def power: Int = card.basePower.getOrElse(0)
  def toughness: Int = card.baseToughness.map(_ - damages).getOrElse(0)

  def tap: Permanent[T] = this.copy(status = Status.Tapped)
  def unTap: Permanent[T] = this.copy(status = Status.Untapped)

  def takeDamage(amount: Int): Permanent[T] = this.copy(damages = damages - amount)
}