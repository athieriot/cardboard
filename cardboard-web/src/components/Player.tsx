import {Zone} from "../types/type";
import React from "react";
import {FireIcon} from "@heroicons/react/20/solid";

interface Props {
    name?: string
    activePlayer?: string
    library?: Zone
}

const Player = ({ activePlayer, name, library }: Props) => {

    return (
        <>
            <div><b>{name}</b></div>
            Library: {library?.length}
            {activePlayer === name && <FireIcon className="h-5 w-5 text-red-600" />}
        </>
    )
}

export default Player