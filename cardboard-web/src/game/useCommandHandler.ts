import {DragEndEvent} from "@dnd-kit/core/dist/types";
import {useMutation} from "react-query";
import axios from "axios";
import {CardId, PlayerId} from "../types/type";

export enum GameAction {
    play = 'play',
    next = 'next'
}

interface Command {
    name: GameAction
    player?: PlayerId
    id?: CardId|PlayerId
}

const useCommandHandler = (currentPlayer?: PlayerId) => {
    const mutation = useMutation((command: Command) => {
        return axios.post('/command', command)
    })

    const dragHandler = (event: DragEndEvent) => {
        if (event?.over?.id === 'land-1' || event?.over?.id === 'land-2') {
            mutation.mutate({ name: GameAction.play, player: currentPlayer, id: event.active.id })
        }
    }

    return {
        dragHandler,
        sendCommand: mutation.mutate
    }
}

export default useCommandHandler