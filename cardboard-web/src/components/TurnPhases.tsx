import './TurnPhases.css'

enum Step {
    Untap = 'unTap',
    Upkeep = 'upKeep',
    Draw = 'draw',
    PreCambatMain = 'preCombatMain',
    BeginningOfCombat = 'beginningOfCombat',
    DeclareAttackers = 'declareAttackers',
    DeclareBlockers = 'declareBlockers',
    CombatDamage = 'combatDamage',
    EndOfCombat = 'endOfCombat',
    PostCombatMain = 'postCombatMain',
    End = 'end',
    Cleanup = 'cleanup',
}

const TurnPhases = () => {
    return <div className='turn_phase_content'>
        {((Object.keys(Step) as (keyof typeof Step)[]).map((step) => (
            <span key={step}>{step}</span>
        )))}
    </div>
}

export default TurnPhases