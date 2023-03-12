import useWebSocket from 'react-use-websocket'
import {Reducer, useReducer, useState} from 'react'
import { Step } from '../components/Phases'
import {CardId, Library, PlayerId, Zone, ZoneEntry} from "../types/type";

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
    players: Record<PlayerId, Zone>
}

interface Shuffled extends Event {
    order: number[]
    player: PlayerId
}

interface Drawn extends Event {
    amount: number
    player: PlayerId
}

interface MovedToStep extends Event {
    phase: Step
}

interface PriorityPassed extends Event {
    to: PlayerId
}

interface EnteredTheBattlefield extends Event {
    id: CardId
    player: PlayerId
}

interface State {
    activePlayer?: PlayerId
    currentPriority?: PlayerId
    currentStep: Step
    libraries: Record<string, Zone>
    hands: Record<string, Zone>
    battlefield: Record<string, Zone>
}

const initialState = {
    currentStep: Step.unTap,
    libraries: {},
    hands: {},
    battlefield: {}
}

const useGameState = () => {

    const reducer = (state: State, action: Envelope<Event>): State => {
        switch (action.entityType) {
            case 'GameCreatedSimplified':
                const game = (action.event as GameCreated)

                const currentPlayer = Object.keys(game.players).at(game.die)
                const player1 = Object.keys(game.players).at(0)
                const player2 = Object.keys(game.players).at(1)

                return {
                    ...state,
                    currentStep: game.step,
                    activePlayer: currentPlayer,
                    currentPriority: currentPlayer,
                    libraries: {
                        ...(player1 ? { [player1]: game.players[player1] } : {}),
                        ...(player2 ? { [player2]: game.players[player2] } : {})
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
            case 'EnteredTheBattlefield':
                const etb = (action.event as EnteredTheBattlefield)

                const card = (state.hands[etb.player] || []).find((e: ZoneEntry) => e.at(0) === etb.id)

                return {
                    ...state,
                    battlefield: {
                        ...state.battlefield,
                        ...{[etb.player]: (state.battlefield[etb.player] || []).concat([card])},
                    },
                    hands: {
                        ...state.hands,
                        ...{[etb.player]: (state.hands[etb.player] || []).filter((e: ZoneEntry) => e.at(0) !== etb.id)},
                   },
                }
            case 'MovedToStep':
                const mts = (action.event as MovedToStep)

                return {
                    ...state,
                    currentStep: mts.phase
                }
            case 'PriorityPassed':
                const pp = (action.event as PriorityPassed)

                return {
                    ...state,
                    currentPriority: pp.to
                }
            case 'TurnEnded$':
                const player1Name = Object.keys(state.libraries).at(0)
                const player2Name = Object.keys(state.libraries).at(1)

                return {
                    ...state,
                    activePlayer: state.activePlayer === player1Name ? player2Name : player1Name
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