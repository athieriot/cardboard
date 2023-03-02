package cards

import cards.mana.*
import cards.types.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.*
import monocle.syntax.all.*
import cards.types.*
import game.mechanics.*

import java.net.URL
import scala.util.Try

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Land], name = "land"),
    new JsonSubTypes.Type(value = classOf[Creature], name = "creature"),
    new JsonSubTypes.Type(value = classOf[Instant], name = "instant"),
    new JsonSubTypes.Type(value = classOf[Token], name = "token"),
    new JsonSubTypes.Type(value = classOf[Artifact], name = "artifact"),
  )
)
abstract class Card {
  def name: String
  def subTypes: List[String]
  def color: Color
  def cost: Cost
  def set: MagicSet
  def numberInSet: Int

  // TODO: That should probably be a list
  def conditions(ctx: Context): Try[Unit]
  // TODO: Would love to have the type of CardState being the same as the current instance
  def effects(id: CardId, ctx: Context, cardState: CardState[Card]): List[Event]

  def text: String = ""
  // TODO: Part of permanent ?
  // TODO: Maybe all those should be on the CardState classes
  def keywordAbilities: List[KeywordAbilities] = List.empty
  def activatedAbilities: Map[Int, ActivatedAbility] = Map.empty
  // TODO: I don't really like having functions to end up serialized
  def triggeredAbilities: Map[Int, TriggeredAbility] = Map.empty

  def isLand: Boolean     = isInstanceOf[Land] || subTypes.exists(_.contains("Land"))
  def isCreature: Boolean = isInstanceOf[Creature] || subTypes.exists(_.contains("Creature"))
  def isToken: Boolean = isInstanceOf[Token] || subTypes.exists(_.contains("Token"))
  def isArtifact: Boolean = isInstanceOf[Artifact] || subTypes.exists(_.contains("Artifact"))

  def preview: URL = {
    new URL(s"https://scryfall.com/card/${set.code}/$numberInSet/${name.replace(" ", "-").replace("'", "").toLowerCase}")
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[AbilityToken], name = "ability"),
  )
)
trait Token extends Card

abstract class PermanentCard extends Card {
  val basePower: Option[Int]
  val baseToughness: Option[Int]
}
