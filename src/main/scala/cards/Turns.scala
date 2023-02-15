package cards

import game.*

enum Phase {
  def next(): Phase = Phase.values.sliding(2).find(_.head == this).map(_.last).getOrElse(unTap)
  
  // TODO: Implement Turn Based checks ?
  // TODO: Implement more Turn Based actions
  def turnBasedActions(player: PlayerId): List[Event] = List(ManaPoolEmptied) ++ { this match {
    case Phase.unTap => List(Untapped, PlayerSwap)
    case Phase.draw => List(Drawn(1, player))
    case _ => List()
  }}
  
  def isMain: Boolean = this == preCombatMain || this == postCombatMain

  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup
}
