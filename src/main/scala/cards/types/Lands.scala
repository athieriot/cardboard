package cards.types

import cards.*
import cards.mana.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import game.mechanics.*
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
  override def conditions(ctx: Context): Try[Unit] = Try {
    if ctx.state.activePlayer != ctx.player then
      throw new RuntimeException("You can only play lands during your turn")
    else if !ctx.state.currentStep.isMain then
      throw new RuntimeException("You can only play lands during a main phase")
    else if ctx.state.stack.nonEmpty then
      throw new RuntimeException("You can only play lands when the stack is empty")
    else if ctx.state.players(ctx.player).landsToPlay <= 0 then
      throw new RuntimeException("You already played a land this turn")
  }
  
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event] = List(LandPlayed(id, ctx.player), EnteredTheBattlefield(id))
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Forest], name = "forest"),
    new JsonSubTypes.Type(value = classOf[Island], name = "island"),
    new JsonSubTypes.Type(value = classOf[Mountain], name = "mountain"),
//    new JsonSubTypes.Type(value = classOf[Swamp], name = "swamp"),
//    new JsonSubTypes.Type(value = classOf[Plains], name = "plains"),
  )
)
sealed abstract class BasicLand extends Land {
  val color: Color = Color.none
  
  def colorProduced: Color
  
  override def activatedAbilities: Map[Int, Ability] = Map(
    1 -> Ability(new Tap(), s"Add one ${colorProduced.toString} mana", (_, ctx: Context, _) => List(ManaAdded(Map(colorProduced -> 1), ctx.player)), manaAbility = true)
  )
}

class Forest(val set: MagicSet, val numberInSet: Int) extends BasicLand {
  val name: String = "Forest"
  val subTypes: List[String] = List("Basic Land", "Forest")
  val cost: Cost = ManaCost("0")
  val colorProduced: Color = Color.green
  val basePower: Option[Int] = None
  val baseToughness: Option[Int] = None
}

class Island(val set: MagicSet, val numberInSet: Int) extends BasicLand {
  val name: String = "Island"
  val subTypes: List[String] = List("Basic Land", "Island")
  val cost: Cost = ManaCost("0")
  val colorProduced: Color = Color.blue
  val basePower: Option[Int] = None
  val baseToughness: Option[Int] = None
}

class Mountain(val set: MagicSet, val numberInSet: Int) extends BasicLand {
  val name: String = "Mountain"
  val subTypes: List[String] = List("Basic Land", "Mountain")
  val cost: Cost = ManaCost("0")
  val colorProduced: Color = Color.red
  val basePower: Option[Int] = None
  val baseToughness: Option[Int] = None
}