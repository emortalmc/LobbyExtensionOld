package dev.emortal.lobby.config

import dev.emortal.immortal.serializer.MaterialSerializer
import dev.emortal.immortal.serializer.PositionSerializer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.item.Material

@kotlinx.serialization.Serializable
class GameListing(
    val description: Array<String> = arrayOf(),
    val itemVisible: Boolean = false,
    @kotlinx.serialization.Serializable(with = MaterialSerializer::class)
    val item: Material = Material.GRASS_BLOCK,
    val slot: Int = 0,
    val npcVisible: Boolean = false,
    @kotlinx.serialization.Serializable(with = PositionSerializer::class)
    val npcPosition: Pos = Pos.ZERO,
    val npcEntityType: String = EntityType.PLAYER.toString(),
    val npcTitles: Array<String> = arrayOf("Example text!"),
    val npcSkinValue: String = "",
    val npcSkinSignature: String = ""
)