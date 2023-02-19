package cards.types

import cards.*
import cards.mana.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.Try

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[BasicLand], name = "basicLand"),
  )
)
sealed abstract class Land extends PermanentCard {
  def checkConditions(state: BoardState, player: PlayerId): Try[Unit] = Try {
    if state.currentPlayer != player then
      throw new RuntimeException("You can only play lands during your turn")
    else if !state.currentStep.isMain then
      throw new RuntimeException("You can only play lands during a main phase")
    else if state.stack.nonEmpty then
      throw new RuntimeException("You can only play lands when the stack is empty")
    else if state.players(player).landsToPlay <= 0 then
      throw new RuntimeException("You already played a land this turn")
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Forest], name = "forest"),
//    new JsonSubTypes.Type(value = classOf[Mountain], name = "mountain"),
//    new JsonSubTypes.Type(value = classOf[Island], name = "island"),
//    new JsonSubTypes.Type(value = classOf[Swamp], name = "swamp"),
//    new JsonSubTypes.Type(value = classOf[Plains], name = "plains"),
  )
)
sealed abstract class BasicLand extends Land {
  val color: Color = Color.none
  
  def colorProduced: Color
  
  def activatedAbilities: Map[Int, Ability] = Map(
    1 -> Ability(Tap, s"Add one ${colorProduced.toString} mana", (_, player) => List(ManaAdded(Map(colorProduced -> 1), player)))
  )
}

class Forest(val set: String, val numberInSet: Int) extends BasicLand {
  val name: String = "Forest"
  val subTypes: List[String] = List("Basic Land", "Forest")
  val cost: CastingCost = ManaCost("G")
  val colorProduced: Color = Color.green
}