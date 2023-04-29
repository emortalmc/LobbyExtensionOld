package dev.emortal.lobby.modules

import dev.emortal.immortal.util.JedisStorage
import dev.emortal.lobby.LobbyMain
import dev.emortal.lobby.npc.MultilineHologram
import dev.emortal.lobby.npc.PacketNPC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.PlayerSkin
import redis.clients.jedis.JedisPubSub
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val playerCountCache = ConcurrentHashMap<String, Int>()

fun npcLoader() {
    registerRedis()

    val eventNode = MinecraftServer.getGlobalEventHandler()

    PacketNPC.init(eventNode)

    val visibleEntries = LobbyMain.gameListingConfig.gameListings.entries.filter { it.value.npcVisible }
    val miniMessage = MiniMessage.miniMessage()
    visibleEntries.forEachIndexed { i, it ->
        val hologramLines = it.value.npcTitles.map { miniMessage.deserialize(it) }.toMutableList()

        hologramLines.add(Component.empty())

        val angle = i * (PI / (visibleEntries.size - 1))
        val circleSize = 3.7
        val circleCenter = Pos(0.5, 69.0, -29.0)
        val lookingPos = Pos(0.5, 69.0, -27.5)
        val x = cos(angle) * circleSize
        val z = sin(angle) * circleSize / 1.5

        val position = circleCenter.add(Pos(x, 0.0, -z)).withLookAt(lookingPos)
        val entityType = EntityType.fromNamespaceId(it.value.npcEntityType)

        LobbyMain.npcs.add(PacketNPC(
            position,
            hologramLines,
            it.key,
            if (it.value.npcSkinValue.isNotEmpty() && it.value.npcSkinSignature.isNotEmpty()) PlayerSkin(it.value.npcSkinValue, it.value.npcSkinSignature) else null,
            entityType
        ))

        val hologram = MultilineHologram(hologramLines.toMutableList())
        LobbyMain.holograms[it.key] = hologram
        val spawnPosition = position.add(0.0, (entityType.height() + 0.2) / 2.0, 0.0)
        LobbyMain.instance.loadChunk(spawnPosition).thenRun {
            hologram.setInstance(spawnPosition, LobbyMain.instance)

            val playerCountCache = playerCountCache[it.key]

            hologram.setLine(hologramLines.size - 1, Component.text("${playerCountCache ?: 0} online", NamedTextColor.GRAY))
        }
    }
}

private fun registerRedis() {
    val jedisScope = CoroutineScope(Dispatchers.IO)
    jedisScope.launch {
        val registerGamePubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                val args = message.split(" ")
                val gameName = args[0]

                playerCountCache[gameName] = 0
                LobbyMain.gameSelectorGUI.refreshPlayers(gameName, 0)

                LobbyMain.refreshHolo(gameName, 0)
            }
        }
        JedisStorage.jedis?.subscribe(registerGamePubSub, "registergame")
    }

    jedisScope.launch {
        val playerCountPubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                val args = message.split(" ")
                val gameName = args[0]
                val playerCount = args[1].toInt()

                playerCountCache[gameName] = playerCount
                LobbyMain.gameSelectorGUI.refreshPlayers(gameName, playerCount)

                LobbyMain.refreshHolo(gameName, playerCount)
            }
        }
        JedisStorage.jedis?.subscribe(playerCountPubSub, "playercount")
    }
}