package cards

// TODO: Need to define steps
enum Phase {
  case unTap, upKeep, draw, preCombatMain, beginningOfCombat, declareAttackers, declareBlockers, combatDamage, endOfCombat, postCombatMain, end, cleanup
}

enum Steps {
  case unTap, upKeep, draw
}
