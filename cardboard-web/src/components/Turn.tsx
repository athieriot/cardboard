import {Button} from "flowbite-react";
import useCommandHandler, {GameAction} from "../game/useCommandHandler";
import {PlayerId} from "../types/type";
import {Step} from "./Phases";

interface Props {
    activePlayer?: PlayerId
    currentStep: Step
}

const Turn = ({ activePlayer, currentStep }: Props) => {
    const { sendCommand } = useCommandHandler()

    const commandNext = { name: currentStep === Step.cleanup ? GameAction.end : GameAction.next, player: activePlayer }
    const commandResolve = { name: GameAction.resolve, player: activePlayer }

    return <div className="flex flex-row">
        <Button pill={true} size="xs" onClick={() => sendCommand(commandNext)}>
            {currentStep === Step.cleanup ? "End" : "Next"}
        </Button>
        <Button pill={true} size="xs" onClick={() => sendCommand(commandResolve)}>
            Resolve
        </Button>
    </div>
}

export default Turn