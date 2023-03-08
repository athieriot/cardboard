import './TurnPhases.css'
import {useEffect} from "react";
import {Badge} from "flowbite-react"
import {CheckIcon, ClockIcon} from "@heroicons/react/20/solid";

export enum Step {
    unTap = 'unTap',
    upKeep = 'upKeep',
    draw = 'draw',
    preCombatMain = 'preCombatMain',
    beginningOfCombat = 'beginningOfCombat',
    declareAttackers = 'declareAttackers',
    declareBlockers = 'declareBlockers',
    combatDamage = 'combatDamage',
    endOfCombat = 'endOfCombat',
    postCombatMain = 'postCombatMain',
    end = 'end',
    cleanup = 'cleanup',
}

interface Props {
    currentStep: keyof typeof Step
}

interface TurnProps {
    currentStep: keyof typeof Step
    step: keyof typeof Step
}

const TurnPhases = ({ currentStep }: Props) => {
    useEffect(() => {
        console.log(currentStep)
    }, [currentStep])

    return <div className='turn_phase_content'>
        {(Object.keys(Step) as (keyof typeof Step)[]).map((step) => (
            <Badge key={step} color={currentStep == step ? "success" : "light"}>{step}</Badge>
        ))}
    </div>
}

export default TurnPhases