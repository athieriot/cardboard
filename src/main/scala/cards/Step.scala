package cards

import game.*

enum Step {
  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup

  // TODO: Should stop if end of turn + EndTurn Command
  def next(): Step = Step.values.sliding(2).find(_.head == this).map(_.last)
    .getOrElse(throw new RuntimeException("Cannot skip end step, please use EndStep Action"))

  def isMain: Boolean = this == preCombatMain || this == postCombatMain

  // TODO: Implement Turn Based checks ?
  // TODO: Implement more Turn Based actions
  // TODO: Check conditions for End step, like hand size ?
  // TODO: Should have Before/After triggers for times when there are multiple steps of the same type
  def turnBasedActions(state: BoardState, player: PlayerId): List[Event] = commonTurnBasedActions(state) ++ { this match {
    case Step.unTap => List(TurnEnded, Untapped)
    case Step.draw => List(Drawn(1, player))
    case Step.cleanup => List(TurnStateCleaned)
    case _ => List()
  }}

  private def commonTurnBasedActions(state: BoardState): List[Event] =
    List(Moved(this), ManaPoolEmptied, PriorityPassed(state.playersTurn))
}

enum Phase(steps: List[Step]) {
  case beginning extends Phase(List(Step.unTap, Step.upKeep, Step.draw))
  case firstMain extends Phase(List(Step.preCombatMain))
  case combat extends Phase(List(Step.beginningOfCombat, Step.declareAttackers, Step.declareBlockers, Step.combatDamage, Step.endOfCombat))
  case secondMain extends Phase(List(Step.postCombatMain))
  case end extends Phase(List(Step.end, Step.cleanup))
}