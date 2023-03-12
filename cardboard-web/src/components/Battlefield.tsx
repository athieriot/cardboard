import {useDroppable} from "@dnd-kit/core";
import classNames from "classnames";
import {Zone} from "../types/type";
import Card from "./Card";

interface Props {
    zoneId: string
    cards: Zone
    onHover: (url: string) => void
}

const Battlefield = ({ zoneId, cards, onHover }: Props) => {
    const {isOver, setNodeRef} = useDroppable({
        id: zoneId,
    });

    return <div ref={setNodeRef} className={classNames({'overlay': isOver})}>
        {zoneId}
        {(cards || []).filter(kv => (kv || [])[1]).map((kv) => {
            const [id, card] = kv || []

            if (!card || !id) return <></>

            return <Card key={id} cardId={id} card={card} onHover={onHover} />
        })}
    </div>
}

export default Battlefield