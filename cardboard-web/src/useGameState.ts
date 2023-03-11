import useWebSocket from 'react-use-websocket'
import { useState } from 'react'
import { Step } from './components/TurnPhases'
import {Library} from "./types/type";

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
    players: Record<string, Library>
}

const useGameState = () => {
    const [currentPlayer, setCurrentPlayer] = useState<string|undefined>("")
    const [currentPriority, setCurrentPriority] = useState<string|undefined>("")
    const [currentStep, setCurrentStep] = useState<Step>(Step.unTap)
    const [libraries, setLibraries] = useState<Record<string, Library>>({})

    const [playerState, setPlayerState] = useState()

    const { readyState } = useWebSocket('ws://localhost:8080/connect', {
        onMessage: (event) => {
            const data = JSON.parse(event.data)
            switch (data.entityType) {
                case 'GameCreatedSimplified':
                    const game = (data.event as GameCreated)
                    setCurrentStep(game.step)
                    for (const player in game.players) {
                        setLibraries((prev) => ({ ...prev, ...{[player]: game.players[player]} }))
                    }
                    setCurrentPlayer(Object.keys(game.players).at(game.die))
                    setCurrentPriority(Object.keys(game.players).at(game.die))
            }
        }
    })

    return { readyState, currentStep, currentPlayer, libraries }
}

export default useGameState