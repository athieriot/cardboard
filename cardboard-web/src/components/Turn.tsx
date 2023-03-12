import {Button} from "flowbite-react";
import useCommandHandler, {GameAction} from "../game/useCommandHandler";
import {PlayerId} from "../types/type";

interface Props {
    currentPlayer?: PlayerId
}

const Turn = ({ currentPlayer }: Props) => {
    const { sendCommand } = useCommandHandler()

    return <Button pill={true} size="xs" onClick={() => sendCommand({ name: GameAction.next, player: currentPlayer })}>
        Next
    </Button>
}

export default Turn