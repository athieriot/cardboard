package cards

import game.*

enum Phases {
  def next(): Phases = Phases.values.sliding(2).find(_.head == this).map(_.last).getOrElse(unTap)

  // TODO: Implement Turn Based checks ?
  // TODO: Implement more Turn Based actions
  // TODO: Check conditions for End step, like hand size ?
  def turnBasedActions(player: PlayerId): List[Event] = List(ManaPoolEmptied) ++ { this match {
    case Phases.unTap => List(PlayerSwapped, Untapped)
    case Phases.draw => List(Drawn(1, player))
    case Phases.cleanup => List(TurnStateCleaned)
    case _ => List()
  }}

  def isMain: Boolean = this == preCombatMain || this == postCombatMain

  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup
}
