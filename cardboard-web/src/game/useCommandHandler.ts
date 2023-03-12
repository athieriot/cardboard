import {DragEndEvent} from "@dnd-kit/core/dist/types";
import {useMutation} from "react-query";
import axios from "axios";
import {CardId, PlayerId} from "../types/type";
import toast from "react-hot-toast";

export enum GameAction {
    play = 'play',
    next = 'next',
    resolve = 'resolve',
    end = 'end'
}

interface Command {
    name: GameAction
    player?: PlayerId
    id?: CardId|PlayerId
}

const useCommandHandler = (currentPlayer?: PlayerId) => {

    const mutation = useMutation((command: Command) => {
        return axios.post('/command', command)
    }, {
        onSuccess: (data) => "",
        onError: (error) => toast.error(error.response.data)
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