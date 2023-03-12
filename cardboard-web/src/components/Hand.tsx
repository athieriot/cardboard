import {Zone} from "../types/type";
import Card from "./Card";

interface Props {
    hand?: Zone,
    onHover: (url: string) => void
}

const Hand = ({ hand, onHover }: Props) => {

    return <>
        {(hand || []).filter(kv => (kv || [])[1]).map((kv) => {
            const [id, card] = kv || []

            if (!card || !id) return <></>

            return <Card key={id} cardId={id} card={card} onHover={onHover} />
        })}
    </>
}

export default Hand