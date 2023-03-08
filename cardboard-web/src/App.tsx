import {useEffect, useState} from 'react'
import './App.css'
import CardPreview from './components/CardPreview'
import TurnPhases from './components/TurnPhases'
import useGameState from "./useGameState";

function App() {
  const { currentStep, readyState} = useGameState()

  return (
    <>
      <div className="zone Turn"></div>
      <div className="zone Phases">
          <TurnPhases currentStep={currentStep} />
      </div>
      <div className="zone Hand-1"></div>
      <div className="zone Player-1"></div>
      <div className="zone Lands-1"></div>
      <div className="zone Permanents-1"></div>
      <div className="zone Stack---Attack"></div>
      <div className="zone Permanents-2"></div>
      <div className="zone Lands-2"></div>
      <div className="zone Hands-2"></div>
      <div className="zone Player-2"></div>
      <div className="zone Library-2"></div>
      <div className="zone Library-1"></div>
      <div className="zone Preview">
        <CardPreview />
      </div>
    </>
  )
}

export default App
