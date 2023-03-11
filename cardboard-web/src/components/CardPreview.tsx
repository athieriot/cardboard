interface Props {
    preview: string
}

const defaultImage = "https://gamepedia.cursecdn.com/mtgsalvation_gamepedia/f/f8/Magic_card_back.jpg"

const CardPreview = ({ preview }: Props) => {
    return <img className="h-max" src={preview || defaultImage} height="100%" width="100%"/>
}

export default CardPreview