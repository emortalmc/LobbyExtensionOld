package dev.emortal.lobby

import dev.emortal.immortal.Immortal
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.lobby.LobbyMain.Companion.gameListingConfig
import dev.emortal.lobby.LobbyMain.Companion.gameListingPath
import dev.emortal.lobby.LobbyMain.Companion.gameSelectorGUI
import dev.emortal.lobby.LobbyMain.Companion.instance
import dev.emortal.lobby.config.GameListingConfig
import dev.emortal.lobby.inventories.GameSelectorGUI
import dev.emortal.lobby.modules.loadLoaders
import dev.emortal.lobby.npc.MultilineHologram
import dev.emortal.lobby.npc.PacketNPC
import dev.emortal.tnt.TNTLoader
import dev.emortal.tnt.source.FileTNTSource
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class LobbyMain  {

    companion object {
        lateinit var instance: Instance

        val npcs = mutableListOf<PacketNPC>()

        val gsonSerializer = GsonComponentSerializer.gson()

        val emortalmcGradient = MiniMessage.miniMessage().deserialize("<bold><gradient:gold:light_purple>EmortalMC")

        val holograms = ConcurrentHashMap<String, MultilineHologram>()

        lateinit var gameListingConfig: GameListingConfig
        val gameListingPath = Path.of("./gameListings.json")
        lateinit var gameSelectorGUI: GameSelectorGUI

        fun createInstance(): Instance {
            val newInstance = MinecraftServer.getInstanceManager().createInstanceContainer()
            newInstance.timeRate = 0
            newInstance.time = 0
            newInstance.timeUpdate = null
            newInstance.chunkLoader = TNTLoader(FileTNTSource(Path.of("./lobby.tnt")))
            newInstance.setTag(Tag.Boolean("doNotAutoUnloadChunk"), true)

            newInstance.enableAutoChunkLoad(false)

            val radius = 8
            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    newInstance.loadChunk(x, z)
                }
            }

            return newInstance
        }

        fun refreshHolo(gameName: String, players: Int) {
            val hologram = holograms[gameName] ?: return

            hologram.setLine(hologram.components.size - 1, Component.text("$players online", NamedTextColor.GRAY))
        }


    }

}

fun main() {
    Immortal.initAsServer(false, null)

    instance = LobbyMain.createInstance()

    gameListingConfig = ConfigHelper.initConfigFile(gameListingPath, GameListingConfig())
    gameSelectorGUI = GameSelectorGUI()

    loadLoaders()
}