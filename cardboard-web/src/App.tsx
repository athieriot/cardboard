import {useEffect, useState} from 'react'
import './App.css'
import CardPreview from './components/CardPreview'
import TurnPhases from './components/TurnPhases'
import useGameState from "./useGameState";
import {QueryClient, QueryClientProvider} from "react-query";
import Player from "./components/Player";
import Card from "./components/Card";
import classNames from "classnames";
import {Button} from "flowbite-react";
import {ArrowRightIcon} from "@heroicons/react/20/solid";

function App() {
  const queryClient = new QueryClient()
  const { currentPlayer, currentStep, libraries, readyState} = useGameState()
  const [preview, setPreview] = useState("")


  const player1 = Object.keys(libraries).at(0)
  const player2 = Object.keys(libraries).at(1)

  return (
    <QueryClientProvider client={queryClient}>
      <div className="zone Turn">
          <Button pill={true} size="xs">
              Next
          </Button>
      </div>
      <div className="zone Phases">
          <TurnPhases currentStep={currentStep} />
      </div>

      <div className="zone Hand-1 grid-cols-7 grid">
          {Object.values(libraries).at(0)?.slice(0, 7).map((kv) => {
              const [id, card] = kv || []

              if (!card) return <></>

              return <Card key={id} card={card} onHover={(url) => setPreview(url)} />
          })}
      </div>
      <div className={classNames("zone Player-1")}>
          <Player currentPlayer={currentPlayer}  name={player1} library={Object.values(libraries).at(0)} />
      </div>
      <div className="zone Lands-1"></div>
      <div className="zone Permanents-1"></div>

      <div className="zone Stack---Attack"></div>

      <div className="zone Permanents-2"></div>
      <div className="zone Lands-2"></div>
      <div className="zone Hands-2 grid-cols-7 grid">
          {Object.values(libraries).at(1)?.slice(0, 7).map((kv) => {
              const [id, card] = kv || []

              if (!card) return <></>

              return <Card key={id} card={card} onHover={(url) => setPreview(url)} />
          })}
      </div>
      <div className={classNames("zone Player-2")}>
          <Player currentPlayer={currentPlayer} name={player2} library={Object.values(libraries).at(1)} />
      </div>
      <div className="zone Preview flex items-center">
        <CardPreview preview={preview} />
      </div>
    </QueryClientProvider>
  )
}

export default App
