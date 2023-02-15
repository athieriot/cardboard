package cards

import cards.*
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
trait LandType extends Card

case class BasicLand(
  name: String,
  subTypes: List[String],
  colorProduced: Color,
  preview: URL,
  color: Color = Color.none,
  manaCost: Option[ManaCost] = Some(ManaCost("0"))
) extends LandType {
  def activatedAbilities: Map[Int, Ability] = Map(
    1 -> Ability(Tap, (_, player) => List(ManaAdded(Map(colorProduced -> 1), player)))
  )
}

// TODO: Should use Pattern Matching
val forest = BasicLand("Forest", List("Basic Land", "Forest"), Color.green, new URL("https://scryfall.com/card/one/276/forest"))
val mountain = BasicLand("Mountain", List("Basic Land", "Mountain"), Color.red, new URL("https://scryfall.com/card/one/275/mountain"))
val swamp = BasicLand("Swamp", List("Basic Land", "Swamp"), Color.black, new URL("https://scryfall.com/card/one/274/swamp"))
val plains = BasicLand("Plains", List("Basic Land", "Plains"), Color.white, new URL("https://scryfall.com/card/one/272/plains"))
val island = BasicLand("Island", List("Basic Land", "Island"), Color.blue, new URL("https://scryfall.com/card/one/273/island"))
val wastes = BasicLand("Wastes", List("Basic Land", "Wastes"), Color.none, new URL("https://scryfall.com/card/ogw/183a/wastes"))

val basicLands = List(forest, mountain, swamp, plains, island)