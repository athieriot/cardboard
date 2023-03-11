import {Card} from "../types/type";
import {useQuery} from "react-query";
import axios from "axios";

interface Props {
    card: Card
    onHover: (url: string) => void
}

const Player = ({ card, onHover }: Props) => {
    const { data, isLoading } = useQuery(['card', card], () =>
        axios.get(
            `https://api.scryfall.com/cards/${card.set}/${card.numberInSet}`
        ).then((res) => res.data))

    if (isLoading) return <></>

    return <img className="w-24" src={data.image_uris.png} onMouseOver={() => onHover(data.image_uris.png)} />
}

export default Player