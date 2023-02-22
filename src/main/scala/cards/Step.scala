package cards

import game.*

enum Step {
  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup

  def next(): Step = Step.values.sliding(2).find(_.head == this).map(_.last)
    .getOrElse(throw new RuntimeException("Cannot skip end step, please use EndStep Action"))

  def isMain: Boolean = this == preCombatMain || this == postCombatMain
}

enum Phase(steps: List[Step]) {
  case beginning extends Phase(List(Step.unTap, Step.upKeep, Step.draw))
  case firstMain extends Phase(List(Step.preCombatMain))
  case combat extends Phase(List(Step.beginningOfCombat, Step.declareAttackers, Step.declareBlockers, Step.combatDamage, Step.endOfCombat))
  case secondMain extends Phase(List(Step.postCombatMain))
  case end extends Phase(List(Step.end, Step.cleanup))
}