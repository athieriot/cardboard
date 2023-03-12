import { useState} from 'react'
import './App.css'
import CardPreview from './components/CardPreview'
import Phases from './components/Phases'
import useGameState from "./game/useGameState";
import Player from "./components/Player";
import classNames from "classnames";
import {Button} from "flowbite-react";
import Hand from "./components/Hand";
import Zone from "./components/Battlefield";
import {DndContext} from "@dnd-kit/core";
import useCommandHandler from "./game/useCommandHandler";
import Battlefield from "./components/Battlefield";
import Turn from "./components/Turn";

function App() {
  const { state, readyState} = useGameState()
  const [preview, setPreview] = useState("")
  const { dragHandler } = useCommandHandler(state.activePlayer)

  const player1 = Object.keys(state.libraries).at(0)
  const player2 = Object.keys(state.libraries).at(1)

  return (
    <DndContext onDragEnd={dragHandler}>
      <div className="zone Turn">
          <Turn currentStep={state.currentStep} activePlayer={state.activePlayer} />
      </div>
      <div className="zone Phases">
          <Phases currentStep={state.currentStep} />
      </div>

      <div className={classNames("zone Player-1")}>
            <Player activePlayer={state.activePlayer}  name={player1} library={Object.values(state.libraries).at(0)} />
        </div>
      <div className="zone Hand-1 grid-cols-7 grid">
          <Hand hand={Object.values(state.hands).at(0)} onHover={(url) => setPreview(url)} />
      </div>

      <div className="zone Lands-1">
          <Battlefield zoneId="land-1" cards={state.battlefield[player1]} onHover={(url) => setPreview(url)}/>
      </div>
      <div className="zone Permanents-1"></div>

      <div className="zone Stack---Attack"></div>

      <div className="zone Permanents-2"></div>
      <div className="zone Lands-2">
          <Battlefield zoneId="land-2" cards={state.battlefield[player2]} onHover={(url) => setPreview(url)} />
      </div>

      <div className="zone Hands-2 grid-cols-7 grid">
          <Hand hand={Object.values(state.hands).at(1)} onHover={(url) => setPreview(url)} />
      </div>

      <div className={classNames("zone Player-2")}>
          <Player activePlayer={state.activePlayer} name={player2} library={Object.values(state.libraries).at(1)} />
      </div>
      <div className="zone Preview flex items-center">
        <CardPreview preview={preview} />
      </div>
    </DndContext>
  )
}

export default App
