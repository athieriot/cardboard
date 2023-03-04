package collection.sets

import collection.fourthEdition.*
import collection.portalThreeKingdoms.MountainBandit
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import game.cards.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    // Red
    new JsonSubTypes.Type(value = classOf[MountainBandit], name = "mountainBandit"),
  )
)
trait PortalThreeKingdoms extends Card {
  override val set: String = "ptk"
}