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
    new JsonSubTypes.Type(value = classOf[Intervene], name = "intervene"),
  )
)
sealed abstract class Instant extends Card {
  def checkConditions(state: BoardState, player: PlayerId): Try[Unit] = Try {}
  def effects(id: CardId, state: BoardState, player: PlayerId): List[Event] = List(PutIntoGraveyard(id, player))
}

class Intervene(val set: MagicSet, val numberInSet: Int) extends Instant {
  val name: String = "Intervene"
  val subTypes: List[String] = List("Instant")
  val color: Color = Color.blue
  val cost: CastingCost = ManaCost("U")

  def activatedAbilities: Map[Int, Ability] = Map()  
}