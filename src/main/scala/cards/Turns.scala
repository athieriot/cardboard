package cards

import game.*

enum Phase {
  def next(): Phase = Phase.values.sliding(2).find(_.head == this).map(_.last).getOrElse(unTap)
  
  // TODO: Implement more Turn Based actions
  def turnBasedActions(player: PlayerId): List[Event] = List(ManaPoolEmptied) ++ { this match {
    case Phase.draw => List(Drawn(1, player))
    case _ => List()
  }}
  
  def isMain: Boolean = this == preCombatMain || this == postCombatMain

  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup
}

// TODO: Need to define steps ?
enum Steps {
  case unTap, upKeep, draw
}
