package cards

import game.*

enum Step {
  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup

  def next(): Step = Step.values.sliding(2).find(_.head == this).map(_.last)
    .getOrElse(throw new RuntimeException("Cannot skip end step, please use EndStep Action"))

  def isMain: Boolean = this == preCombatMain || this == postCombatMain

  // TODO: Implement more Turn Based actions
  // TODO: Should have Before/After triggers for times when there are multiple steps of the same type
  // TODO: Should be handle in a sort of Event handler
  def turnBasedActions(state: BoardState, player: PlayerId): List[Event] = MovedToStep(this) +: { this match {
    case Step.unTap => List(TurnEnded, Untapped)
    case Step.draw => List(Drawn(1, player))
    case Step.declareBlockers => if state.combatZone.isEmpty then List(MovedToStep(Step.endOfCombat)) else List(PriorityPassed(state.nextPriority))
    // TODO: This might require a bit of an explanation
    case Step.combatDamage =>
      state.combatZone.flatMap { case (attackerId, CombatZoneEntry(attacker, target, blockers)) => blockers match {
        case m if m.isEmpty => if attacker.power > 0 then List(DamageDealt(target, attacker.power)) else List()
        case _ =>
          val (events, _, _) = blockers.foldLeft((List.empty[Event], attacker.power, attacker.toughness)) { case (acc@(events, powerLeft, toughnessLeft), (blockerId, blocker)) =>
            if toughnessLeft > 0 && powerLeft > 0 then
              val damageAssigned = powerLeft.min(blocker.toughness)
              (events ++ List(DamageDealt(attackerId, blocker.power), DamageDealt(blockerId, damageAssigned)), powerLeft - damageAssigned, toughnessLeft - blocker.power)
            else acc
          }
          events
          // TODO: Trample => ++ (if powerLeft > 0 then List(DamageDealt(target, powerLeft)) else List())
      }}.toList
    case Step.endOfCombat => List(CombatEnded)
    case _ => List()
  }}
}

enum Phase(steps: List[Step]) {
  case beginning extends Phase(List(Step.unTap, Step.upKeep, Step.draw))
  case firstMain extends Phase(List(Step.preCombatMain))
  case combat extends Phase(List(Step.beginningOfCombat, Step.declareAttackers, Step.declareBlockers, Step.combatDamage, Step.endOfCombat))
  case secondMain extends Phase(List(Step.postCombatMain))
  case end extends Phase(List(Step.end, Step.cleanup))
}