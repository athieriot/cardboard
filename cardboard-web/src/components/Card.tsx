import {Card, CardId} from "../types/type";
import {useQuery} from "react-query";
import axios from "axios";
import {useDraggable} from '@dnd-kit/core';
import {CSS} from '@dnd-kit/utilities';

interface Props {
    cardId: CardId,
    card: Card
    onHover: (url: string) => void
}

const Card = ({ cardId, card, onHover }: Props) => {
    const { data, isLoading } = useQuery(['card', card], () =>
        axios.get(
            `https://api.scryfall.com/cards/${card.set}/${card.numberInSet}`
        ).then((res) => res.data))



    const {attributes, listeners, setNodeRef, transform} = useDraggable({
        id: cardId,
    });
    const style = transform ? {
        transform: CSS.Translate.toString(transform),
    } : undefined;


    if (isLoading) return <></>

    return <img ref={setNodeRef} style={style} {...listeners} {...attributes} className="w-24 z-50" src={data.image_uris.png} onMouseOver={() => onHover(data.image_uris.png)} />
}

export default Card