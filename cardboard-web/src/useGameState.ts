import useWebSocket from 'react-use-websocket'
import { useState } from 'react'
import { Step } from './components/TurnPhases'

interface Envelope<T> {
    offset: { value: number }
    persistenceId: string
    sequenceNr: number
    timestamp: number
    event: T
    entityType: string
}
interface GameCreated {
    die: number
    step: Step
    createdAt: string
}

const useGameState = () => {
    const [currentStep, setCurrentStep] = useState<Step>(Step.unTap)

    const { readyState } = useWebSocket('ws://localhost:8080/connect', {
        onMessage: (event) => {
            const data = JSON.parse(event.data)
            switch (data.entityType) {
                case 'GameCreatedSimplified': setCurrentStep((data.event as GameCreated).step)
            }
        }
    })

    return { readyState, currentStep }
}

export default useGameState