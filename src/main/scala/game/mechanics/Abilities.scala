package game.mechanics

import game.cards.*
import game.cards.types.*
import game.*
import game.cards.{Card, CardState, MagicSet, Token}
import game.mana.{Color, Cost, NoCost}

import java.net.URL
import scala.util.Try

enum KeywordAbilities {
  case haste, trample
}

trait Ability {
  def text: String
  def effects: (CardId, Context, CardState[Card]) => List[Event]
  def manaAbility: Boolean
}

case class TriggeredAbility(
  text: String,
  effects: (CardId, Context, CardState[Card]) => List[Event],
  triggers: Event => Option[TargetArg],
  manaAbility: Boolean = false,
) extends Ability

case class ActivatedAbility(
  cost: Cost,
  text: String,
  effects: (CardId, Context, CardState[Card]) => List[Event],
  conditions: Context => Try[Unit] = _ => Try(()),
  manaAbility: Boolean = false,
) extends Ability {
  def payCost(id: CardId, ctx: Context, cardState: CardState[Card]): Try[List[Event]] = cost.pay(id, ctx, cardState)

  // TODO: Effect should return the list also ?
  // TODO: That way we could have Ready Made effects
  def checkConditions(ctx: Context): Try[Unit] = conditions(ctx)
}

case class AbilityToken(name: String, ability: Ability) extends Token {
  def color: Color = Color.colorless

  override def subTypes: List[String] = throw new RuntimeException("Ability, not a Card")
  override def set: MagicSet = throw new RuntimeException("Ability, not a Card")
  override def numberInSet: Int = throw new RuntimeException("Ability, not a Card")
  override def preview: URL = throw new RuntimeException("Ability, not a Card")

  def cost: Cost = new NoCost
  def conditions(ctx: Context): Try[Unit] = Try(new RuntimeException("Should not need to check Ability condition"))
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] =
    ability.effects(id, ctx, cardState).concat(List(PutIntoGraveyard(id, ctx.player)))
}
