package cards

import cards.mana.*
import cards.types.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.mechanics.{AbilityToken, *}

import java.net.URL
import scala.util.Try

enum Status {
  case Tapped, Untapped
}

sealed abstract class CardState[+T] { val card: T; val owner: String; val controller: String }
case class UnPlayed[T <: Card](card: T, owner: String, controller: String) extends CardState[T] {

  def payCost(id: CardId, ctx: Context): Try[List[Event]] = card.cost.pay(id, ctx, this)
  // TODO: Effect should return the list also ?
  // TODO: That way we could have Ready Made effects
  def checkConditions(ctx: Context): Try[Unit] = card.conditions(ctx)
}

case class Spell[T <: Card](
  card: T,
  owner: String,
  controller: String,
  // TODO: Should be only Option[Target]
  args: List[Arg[_]] = List.empty
) extends CardState[T] {

  def buildEffects(id: CardId, ctx: Context): List[Event] = card.effects(id, ctx, this)
}

case class Permanent[T <: PermanentCard](
  card: T,
  owner: String,
  controller: String,
  status: Status = Status.Untapped,
  firstTurn: Boolean = true,
  damages: Int = 0,
  attacking: Option[TargetId] = None,
  bolocking: Option[CardId] = None,
  args: List[Arg[_]] = List.empty
) extends CardState[T] {
  def hasSummoningSickness: Boolean = card.isCreature && !card.keywordAbilities.contains(KeywordAbilities.haste) && firstTurn
  def dispatchTriggers(event: Event): List[(Int, TargetArg)] =
    card.triggeredAbilities.flatMap { case (id, ta) => ta.triggers(event).map((id, _)) }.toList

  def power: Int = card.basePower.getOrElse(0)
  def toughness: Int = card.baseToughness.map(_ - damages).getOrElse(0)

  def tap: Permanent[T] = this.copy(status = Status.Tapped)
  def unTap: Permanent[T] = this.copy(status = Status.Untapped)

  def takeDamage(amount: Int): Permanent[T] = this.copy(damages = damages + amount)
}