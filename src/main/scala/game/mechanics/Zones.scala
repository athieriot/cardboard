package game.mechanics

import cards.{Card, *}
import cards.types.*
import game.*
import monocle.AppliedOptional
import monocle.syntax.all.*

import scala.annotation.unused

type ZoneLens[A <: CardState[Card]] = AppliedOptional[BoardState, Map[CardId, A]]
type CardLens[A <: CardState[Card]] = AppliedOptional[BoardState, A]
sealed trait Zone[A <: CardState[Card]] {
  def focusIn(id: CardId, state: BoardState): CardLens[A]
  def focusIn(state: BoardState): ZoneLens[A]
  def convert[From <: CardState[Card]](by: PlayerId, from: From, args: List[Arg[_]]): A
}
case object Stack extends Zone[Spell[Card]] {
  def focusIn(id: CardId, state: BoardState): CardLens[Spell[Card]] = state.focus(_.stack.index(id))
  def focusIn(state: BoardState): ZoneLens[Spell[Card]] = state.focus(_.stack)
  def convert[From <: CardState[Card]](by: PlayerId, from: From, args: List[Arg[_]]): Spell[Card] = from match {
    case UnPlayed(_: Land, _) => throw new RuntimeException("Lands don't go on the Stack")
    case UnPlayed(c, owner) => Spell(c, owner, by, args)
    case _: Permanent[_] => throw new RuntimeException("Cannot cast a spell from the battlefield")
    case s: Spell[_] => s.asInstanceOf[Spell[Card]]
  }
}
case object Battlefield extends Zone[Permanent[PermanentCard]] {
  def focusIn(id: CardId, state: BoardState): CardLens[Permanent[PermanentCard]] = state.focus(_.battleField.index(id))
  def focusIn(state: BoardState): ZoneLens[Permanent[PermanentCard]] = state.focus(_.battleField)
  def convert[From <: CardState[Card]](by: PlayerId, from: From, args: List[Arg[_]]): Permanent[PermanentCard] = from match {
    case UnPlayed(c: PermanentCard, owner) => Permanent(c, owner, by, args = args)
    case UnPlayed(_, _) => throw new RuntimeException("Cannot convert a non PermanentCard to Permanent")
    case Spell(c: PermanentCard, owner, controller, spellArgs) => Permanent(c, owner, controller, args = spellArgs) 
    case Spell(_, _, _, _) => throw new RuntimeException("Cannot convert a non PermanentCard to Permanent")
    case s: Permanent[_] => s.asInstanceOf[Permanent[PermanentCard]]
  }
}

abstract class PlayerZone(player: PlayerId) extends Zone[UnPlayed[Card]] {
  def isOf(player: PlayerId): Boolean = this.player == player
  def convert[From <: CardState[Card]](by: PlayerId, from: From, args: List[Arg[_]]): UnPlayed[Card] = from match {
    case s: Spell[_] => UnPlayed(s.card, s.owner)
    case s: Permanent[_] => UnPlayed(s.card, s.owner)
    case s: UnPlayed[_] => s.asInstanceOf[UnPlayed[Card]]
  }
}
case class Hand(player: PlayerId) extends PlayerZone(player) {
  def focusIn(id: CardId, state: BoardState): CardLens[UnPlayed[Card]] = state.focus(_.players.index(player).hand.index(id))
  def focusIn(state: BoardState): ZoneLens[UnPlayed[Card]] = state.focus(_.players.index(player).hand)
}
case class Graveyard(player: PlayerId) extends PlayerZone(player) {
  def focusIn(id: CardId, state: BoardState): CardLens[UnPlayed[Card]] = state.focus(_.players.index(player).graveyard.index(id))
  def focusIn(state: BoardState): ZoneLens[UnPlayed[Card]] = state.focus(_.players.index(player).graveyard)
}
case class Library(player: PlayerId) extends PlayerZone(player) {
  def focusIn(id: CardId, state: BoardState): CardLens[UnPlayed[Card]] = state.focus(_.players.index(player).library.index(id))
  def focusIn(state: BoardState): ZoneLens[UnPlayed[Card]] = state.focus(_.players.index(player).library)
}
case class Exile(player: PlayerId) extends PlayerZone(player) {
  def focusIn(id: CardId, state: BoardState): CardLens[UnPlayed[Card]] = state.focus(_.players.index(player).exile.index(id))
  def focusIn(state: BoardState): ZoneLens[UnPlayed[Card]] = state.focus(_.players.index(player).exile)
}