import useWebSocket from 'react-use-websocket'
import {Reducer, useReducer, useState} from 'react'
import { Step } from './components/TurnPhases'
import {Library, PlayerId, Zone} from "./types/type";

interface Envelope<T> {
    offset: { value: number }
    persistenceId: string
    sequenceNr: number
    timestamp: number
    event: T
    entityType: string
}

interface Event { }

interface GameCreated extends Event {
    die: number
    step: Step
    players: Record<string, Zone>
}

interface Shuffled extends Event {
    order: number[]
    player: String
}

interface Drawn extends Event {
    amount: number
    player: String
}


interface State {
    currentPlayer?: PlayerId
    currentPriority?: PlayerId
    currentStep: Step
    libraries: Record<string, Zone>
    hands: Record<string, Zone>
}

const initialState = {
    currentStep: Step.unTap,
    libraries: {},
    hands: {}
}


const useGameState = () => {

    const reducer = (state: State, action: Envelope<Event>): State => {
        switch (action.entityType) {
            case 'GameCreatedSimplified':
                const game = (action.event as GameCreated)

                const currentPlayer = Object.keys(game.players).at(game.die)
                const player1 = Object.keys(game.players).at(0)
                const player2 = Object.keys(game.players).at(1)

                const library1 = player1 ? { [player1]: game.players[player1] } : {}
                const library2 = player2 ? { [player2]: game.players[player2] } : {}

                return {
                    ...state,
                    currentStep: game.step,
                    currentPlayer: currentPlayer,
                    currentPriority: currentPlayer,
                    libraries: {
                        ...library1,
                        ...library2
                    },
                }
            case 'Shuffled':
                const shuffled = (action.event as Shuffled)

                const shuffledDeck = shuffled.order.map(function(e, i) {
                    // @ts-ignore
                    return [e, state.libraries[shuffled.player][i]];
                }).sort((a, b) => a[0] - b[0])
                    .map((e) => e[1])
                    .filter(Boolean);

                return {
                    ...state,
                    libraries: {
                        ...state.libraries,
                        ...{[shuffled.player]: shuffledDeck}
                    }
                }
            case 'Drawn':
                const drawn = (action.event as Drawn)

                const cards = state.libraries[drawn.player]?.slice(0, drawn.amount)

                return {
                    ...state,
                    libraries: {
                        ...state.libraries,
                        ...{[drawn.player]: state.libraries[drawn.player]?.slice(drawn.amount)}
                    },
                    hands: {
                        ...state.hands,
                        ...{[drawn.player]: (state.hands[drawn.player] || []).concat(cards)}
                    }
                }
        }

        return state
    }


    const [state, dispatch] = useReducer<Reducer<State, Envelope<Event>>>(reducer, initialState)

    const { readyState } = useWebSocket('ws://localhost:8080/connect', {
        onMessage: (event) => dispatch(JSON.parse(event.data))
    })

    return { readyState, state }
}

export default useGameState