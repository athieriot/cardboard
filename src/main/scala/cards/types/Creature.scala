package cards.types

import cards.*
import cards.mana.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*

import java.net.URL
import scala.util.Try

case class Creature(
  name: String,
  subTypes: List[String],
  color: Color,
  cost: CastingCost,
  preview: URL,
) extends Card {
  def activatedAbilities: Map[Int, Ability] = Map(
    1 -> Ability(Tap, (_, player) => List(ManaAdded(Map(Color.green -> 1), player)))
  )
}


val llanowarElf = Creature("Llanowar Elf", List("Creature", "Elf Druid"), Color.green, ManaCost("G"), new URL("https://scryfall.com/card/m19/314/llanowar-elves"))