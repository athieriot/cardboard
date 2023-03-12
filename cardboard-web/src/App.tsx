import { useState} from 'react'
import './App.css'
import CardPreview from './components/CardPreview'
import TurnPhases from './components/TurnPhases'
import useGameState from "./useGameState";
import Player from "./components/Player";
import Card from "./components/Card";
import classNames from "classnames";
import {Button} from "flowbite-react";
import Hand from "./components/Hand";

function App() {
  const { state, readyState} = useGameState()
  const [preview, setPreview] = useState("")

  const player1 = Object.keys(state.libraries).at(0)
  const player2 = Object.keys(state.libraries).at(1)

  return (
    <>
      <div className="zone Turn">
          <Button pill={true} size="xs">
              Next
          </Button>
      </div>
      <div className="zone Phases">
          <TurnPhases currentStep={state.currentStep} />
      </div>

      <div className="zone Hand-1 grid-cols-7 grid">

          <Hand hand={Object.values(state.hands).at(0)} onHover={(url) => setPreview(url)} />
      </div>
      <div className={classNames("zone Player-1")}>
          <Player currentPlayer={state.currentPlayer}  name={player1} library={Object.values(state.libraries).at(0)} />
      </div>
      <div className="zone Lands-1"></div>
      <div className="zone Permanents-1"></div>

      <div className="zone Stack---Attack"></div>

      <div className="zone Permanents-2"></div>
      <div className="zone Lands-2"></div>
      <div className="zone Hands-2 grid-cols-7 grid">

          <Hand hand={Object.values(state.hands).at(1)} onHover={(url) => setPreview(url)} />
      </div>
      <div className={classNames("zone Player-2")}>
          <Player currentPlayer={state.currentPlayer} name={player2} library={Object.values(state.libraries).at(1)} />
      </div>
      <div className="zone Preview flex items-center">
        <CardPreview preview={preview} />
      </div>
    </>
  )
}

export default App
