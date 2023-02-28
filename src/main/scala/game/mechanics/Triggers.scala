package game.mechanics

import game.*
import game.Engine.eventHandler

// TODO: Priority rounds could use a "Pass" command
object Triggers {

  def triggersHandler(state: State, events: List[Event]): List[Event] = {
    events.foldLeft((state, List.empty[Event])) { case ((state, acc), event) =>
      val (newState, newEvents) = triggerHandler(state: State, event: Event)
      (newState, acc ++ newEvents)
    }._2
  }

  // TODO: Try[(State, List[Event])] so we can throw errors and also map/flatMap ?
  private def triggerHandler(state: State, event: Event): (State, List[Event]) =
    val newEvents = event match {
      case MovedToStep(step) =>
        val newState = eventHandler(state, event)
        event +: triggersHandler(newState, turnBaseActions(step, newState))
      case PriorityPassed(_) => triggersHandler(state, stateBasedActionsLoop(state)) :+ event
      case _ => List(event)
    }
    (newEvents.foldLeft(state)(eventHandler(_, _)), newEvents)

  private def stateBasedActionsLoop(state: State, check: State => List[Event] = stateBaseActionsCheck): List[Event] =
    check(state) match {
      case Nil => List.empty
      case events => events ++ stateBasedActionsLoop(events.foldLeft(state)(eventHandler(_, _)), check)
    }

  // TODO: I'm still quite scared that the state is not up to date at that point
  private def stateBaseActionsCheck(state: State): List[Event] = state match {
    case state: BoardState =>
      state.players.filter(_._2.life <= 0).map(p => GameEnded(p._1)).toList
        ++ state.battleField.filter(c => c._2.card.basePower.isDefined && c._2.toughness <= 0).map(c => Destroyed(c._1, c._2.owner)).toList
    case _ => List.empty
  }

  // TODO: Implement more Turn Based actions
  // TODO: Should have Before/After triggers for times when there are multiple steps of the same type
  private def turnBaseActions(step: Step, state: State): List[Event] = state match {
    case state: BoardState =>
      step match {
        case Step.unTap => List(Untapped)
        case Step.draw => List(Drawn(1, state.activePlayer))
        case Step.declareBlockers => if state.attackers.isEmpty then List(MovedToStep(Step.endOfCombat)) else List()
        case Step.combatDamage =>
          // TODO: This might require a bit of an explanation
          state.attackers.toList.flatMap { case (attackerId, (attacker, blockers)) => blockers match {
            case m if m.isEmpty => if attacker.power > 0 then List(DamageDealt(attacker.attacking.get, attacker.power)) else List()
            case _ =>
              val (events, _, _) = blockers.foldLeft((List.empty[Event], attacker.power, attacker.toughness)) { case (acc@(events, powerLeft, toughnessLeft), (blockerId, blocker)) =>
                if toughnessLeft > 0 && powerLeft > 0 then
                  val damageAssigned = powerLeft.min(blocker.toughness)
                  (events ++ List(DamageDealt(attackerId, blocker.power), DamageDealt(blockerId, damageAssigned)), powerLeft - damageAssigned, toughnessLeft - blocker.power)
                else acc
              }
              events
            // TODO: Trample => ++ (if powerLeft > 0 then List(DamageDealt(target, powerLeft)) else List())
          }}
        case Step.endOfCombat => List(CombatEnded)
        case _ => List()
      }
    case _ => List.empty
  }

}