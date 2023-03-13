import {Card, CardId} from "../types/type";
import {useQuery} from "react-query";
import axios from "axios";
import {useDraggable} from '@dnd-kit/core';
import {CSS} from '@dnd-kit/utilities';
import {Button, ListGroup, Modal, Tooltip} from "flowbite-react";
import {useState} from "react";

interface Props {
    cardId: CardId,
    card: Card
    onHover: (url: string) => void
}

const AbilityModal = ({ card, show, onClose }: {card: Card, show: boolean, onClose: () => void}) => {
    return <Modal
        show={show}
        size="sm"
        onClose={onClose}
    >
        <Modal.Header>
            Abilities
        </Modal.Header>
        <Modal.Body>
            <ListGroup>
                {(card.abilities || []).map(a => {
                    const [i, text] = a

                    return <ListGroup.Item key={i}>
                        <Button>{i}: {text}</Button>
                    </ListGroup.Item>
                })}
            </ListGroup>
        </Modal.Body>
    </Modal>
}

const Card = ({ cardId, card, onHover }: Props) => {
    const [showModal, setShowModal] = useState(false)

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

    return <Tooltip content={
        <ListGroup>
            {(card.abilities || []).map(a => {
                const [i, text] = a

                return <ListGroup.Item key={i}>
                    {text}
                </ListGroup.Item>
            })}
        </ListGroup>
    }>
        <img ref={setNodeRef} style={style} {...listeners} {...attributes} className="w-20 z-50" src={data.image_uris.png} onMouseOver={() => onHover(data.image_uris.png)} />
        <AbilityModal card={card} show={showModal} onClose={() => setShowModal(false)} />
    </Tooltip>
}

export default Card