package game

import game.*
import game.cards.*
import game.mana.*
import game.mechanics.*
import monocle.{AppliedLens, AppliedOptional, Lens}

import java.util.UUID
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import monocle.syntax.all.*

import java.time.Instant
import scala.annotation.targetName
import scala.collection.immutable.ListMap

type CardId = Int
type PlayerId = String
type TargetId = CardId|PlayerId

case class PlayerState(
  library: Map[CardId, UnPlayed[Card]],
  life: Int = 20,
  landsToPlay: Int = 1,
  manaPool: ManaPool = ManaPool.empty(),

  hand: Map[CardId, UnPlayed[Card]] = Map.empty,
  graveyard: Map[CardId, UnPlayed[Card]] = Map.empty,
  exile: Map[CardId, UnPlayed[Card]] = Map.empty,
  command: Option[UnPlayed[Card]] = None,
) {
  def takeDamage(amount: Int): PlayerState = this.copy(life = life - amount)
}

trait State
case object EmptyState extends State
case class EndState(loser: String) extends State
case class BoardState(
   activePlayer: String,
   priority: String,
   players: Map[String, PlayerState],
   cardsZone: Map[CardId, Zone[CardState[Card]]],

   // TODO: Stack should have it's own class with Pop/Put/Resolve
   stack: Map[CardId, Spell[Card]] = Map.empty,
   battleField: Map[CardId, Permanent[PermanentCard]] = Map.empty,
                     
   currentStep: Step = Step.preCombatMain,
   createdAt: Instant = Instant.now(),
   turnCount: Int = 1
) extends State {
  def nextPlayer: String = players.keys.sliding(2).find(_.head == activePlayer).map(_.last).getOrElse(players.keys.head)
  def nextPriority: String = players.keys.sliding(2).find(_.head == priority).map(_.last).getOrElse(players.keys.head)

  // Player methods
  def modifyPlayer(player: PlayerId, block: PlayerState => PlayerState): BoardState = this.focus(_.players.index(player)).modify(block)
  def modifyPlayers(block: PlayerState => PlayerState): BoardState =
    this.players.keys.foldLeft(this) { (state, player) => state.focus(_.players.index(player)).modify(block) }
  def focusOnManaPool(player: PlayerId): AppliedOptional[BoardState, ManaPool] = this.focus(_.players.index(player).manaPool)

  // Zone methods
  def focusOnZone[A <: CardState[Card]](zone: Zone[A]): AppliedOptional[BoardState, Map[CardId, A]] = zone.focusIn(this)
  def getCardFromZone[A <: CardState[Card]](id: CardId, zone: Zone[A]): Option[A] = zone.focusIn(this).getOption.flatMap(_.get(id))
  def listCardsFromZone[A <: CardState[Card]](zone: Zone[A]): Map[CardId, A] = zone.focusIn(this).getOption.get
  def modifyAllCardsFromZone[A <: CardState[Card]](zone: Zone[A], block: A => A): BoardState = zone.focusIn(this).modify(m => ListMap(m.view.mapValues(block).toList: _*))

  // Card methods
  def modifyCardFromZone[A <: CardState[Card]](id: CardId, zone: Zone[A], block: A => A): BoardState = zone.focusIn(id, this).modify(block)
  def getCard(id: CardId): Option[(Zone[CardState[Card]], CardState[Card])] = cardsZone.get(id).flatMap(zone => getCardFromZone(id, zone).map((zone, _)))
  def getCardOwner(id: CardId): Option[String] = getCard(id).map(_._2.owner)
  def getCardController(id: CardId): Option[String] = getCard(id).map(_._2.controller)
  // TODO: Top or Bottom ?
  def moveCards[A <: CardState[Card]](ids: List[CardId], by: PlayerId, origin: Zone[A], destination: Zone[A], args: List[Arg[_]] = List.empty): BoardState =
    ids.foldRight(this) { case (id, state) => state.moveCard(id, by, origin, destination) }
  def moveCard[A <: CardState[Card], B <: CardState[Card]](id: CardId, by: PlayerId, origin: Zone[A], destination: Zone[B], args: List[Arg[_]] = List.empty): BoardState = getCardFromZone(id, origin) match {
    case Some(Spell(_: Token, _, _, _)) => focusOnZone(origin).modify(_.removed(id))
    case Some(card) =>
      focusOnZone(destination).modify(_ + (id -> destination.convert(by, card, args)))
        .focus(_.cardsZone).modify(_.updatedWith(id)(_.map(_ => destination.asInstanceOf[Zone[CardState[Card]]])))
        .focusOnZone(origin).modify(_.removed(id))
    case None => this
  }
  def createToken[A <: CardState[Card]](id: CardId, destination: Zone[A], tokenState: A): BoardState =
    focusOnZone(destination).modify(_ + (id -> tokenState))

  // TODO: Will need to add more restrictions based on static abilities
  // Combat methods
  def attackers: Map[CardId, (Permanent[PermanentCard], Map[CardId, Permanent[PermanentCard]])] =
    listCardsFromZone(Battlefield).filter(_._2.attacking.isDefined)
      .map(kv => (kv._1, (kv._2, listCardsFromZone(Battlefield).filter(_._2.bolocking.contains(kv._1)))))
    
  def potentialAttackers(player: PlayerId): Map[CardId, Permanent[PermanentCard]] = listCardsFromZone(Battlefield)
    .filter(_._2.controller == player)
    .filter(_._2.card.isCreature)
    .filter(_._2.status == Status.Untapped)
    .filterNot(_._2.hasSummoningSickness)

  def potentialBlockers(player: PlayerId): Map[CardId, Permanent[PermanentCard]] = listCardsFromZone(Battlefield)
    .filter(_._2.controller == player)
    .filter(_._2.card.isCreature)
    .filter(_._2.status == Status.Untapped)
}