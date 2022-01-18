package dev.emortal.lobby.config

import kotlinx.serialization.Serializable
import net.minestom.server.item.Material
import world.cepi.kstom.serializer.MaterialSerializer

@Serializable
class GameListing(
    val description: Array<String> = arrayOf(),
    @Serializable(with = MaterialSerializer::class)
    val item: Material = Material.GRASS_BLOCK,
    val slot: Int = 0,
    val visible: Boolean = true,

)