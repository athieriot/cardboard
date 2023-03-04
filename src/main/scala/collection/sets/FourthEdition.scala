package collection.sets

import collection.fourthEdition.*
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.cards.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    // Colorless
    new JsonSubTypes.Type(value = classOf[AnkhOfMishra], name = "ankhOfMishra"),
    // Green
    new JsonSubTypes.Type(value = classOf[LlanowarElf], name = "llanowarElf"),
    new JsonSubTypes.Type(value = classOf[WarMammoth], name = "warMammoth"),
    // Blue
    new JsonSubTypes.Type(value = classOf[ProdigalSorcerer], name = "prodigalSorcerer"),
    new JsonSubTypes.Type(value = classOf[Counterspell], name = "counterSpell"),
    // Red
    new JsonSubTypes.Type(value = classOf[MonssGoblinRaiders], name = "monssGoblinRaiders"),
    // Land
    new JsonSubTypes.Type(value = classOf[Forest], name = "forest"),
    new JsonSubTypes.Type(value = classOf[Island], name = "island"),
    new JsonSubTypes.Type(value = classOf[Mountain], name = "mountain"),
  )
)
trait FourthEdition extends Card {
  override val set: String = "4ed"
}