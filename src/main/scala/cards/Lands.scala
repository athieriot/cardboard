package cards

import cards.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import monocle.syntax.all.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[BasicLand], name = "basicLand"),
  )
)
trait LandType extends Card

case class BasicLand(
  name: String,
  colorProduced: Color
) extends LandType {
  def activatedAbilities: Map[String, Ability] = {
    Map(
      "tap" -> Ability(Tapping, (state, player) => state.focus(_.players.index(player).manaPool.index(colorProduced)).modify(_ + 1))
    )
  }
}

// TODO: Should it generate commands/events ?
// TODO: Should use Pattern Matching
// TODO: Card names in color ?
val forest = BasicLand("Forest", Color.green)
val mountain = BasicLand("Mountain", Color.red)
val swamp = BasicLand("Swamp", Color.black)
val plains = BasicLand("Plains", Color.white)
val island = BasicLand("Island", Color.blue)
val wastes = BasicLand("Wastes", Color.none)

val basicLands = List(forest, mountain, swamp, plains, island)