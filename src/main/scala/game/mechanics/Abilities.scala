package game.mechanics

import cards.*
import cards.mana.*
import cards.types.*
import game.*

import java.net.URL
import scala.util.Try

enum KeywordAbilities {
  case haste
}

case class Ability(
  cost: Cost,
  text: String,
  effects: (CardId, Context, CardState[Card]) => List[Event],
  conditions: Context => Try[Unit] = _ => Try(()),
  manaAbility: Boolean = false,
) {
  def payCost(id: CardId, ctx: Context, cardState: CardState[Card]): Try[List[Event]] = cost.pay(id, ctx, cardState)

  // TODO: Effect should return the list also ?
  // TODO: That way we could have Ready Made effects
  def checkConditions(ctx: Context): Try[Unit] = conditions(ctx)
}

case class AbilityToken(name: String, ability: Ability) extends Token {
  def color: Color = Color.none

  override def subTypes: List[String] = throw new RuntimeException("Ability, not a Card")
  override def set: MagicSet = throw new RuntimeException("Ability, not a Card")
  override def numberInSet: Int = throw new RuntimeException("Ability, not a Card")
  override def preview: URL = throw new RuntimeException("Ability, not a Card")

  def cost: Cost = ability.cost
  def conditions(ctx: Context): Try[Unit] = ability.conditions(ctx)
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] =
    ability.effects(id, ctx, cardState).concat(List(PutIntoGraveyard(id, ctx.player)))
}
