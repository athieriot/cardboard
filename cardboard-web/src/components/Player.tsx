import {Library} from "../types/type";
import React from "react";
import {FireIcon} from "@heroicons/react/20/solid";

interface Props {
    name?: string
    currentPlayer?: string
    library?: Library
}

const Player = ({ currentPlayer, name, library }: Props) => {

    return (
        <>
            <div><b>{name}</b></div>
            Library: {library?.length}
            {currentPlayer === name && <FireIcon className="h-5 w-5 text-red-600" />}
        </>
    )
}

export default Player