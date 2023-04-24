package dev.emortal.lobby

import dev.emortal.immortal.Immortal
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.JedisStorage
import dev.emortal.immortal.util.showFirework
import dev.emortal.lobby.commands.*
import dev.emortal.lobby.config.GameListingConfig
import dev.emortal.lobby.games.LobbyExtensionGame
import dev.emortal.lobby.inventories.GameSelectorGUI
import dev.emortal.nbstom.NBS
import dev.emortal.nbstom.commands.LoopCommand
import dev.emortal.nbstom.commands.MusicCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.biomes.Biome
import net.minestom.server.world.biomes.BiomeEffects
import redis.clients.jedis.JedisPubSub
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    Immortal.initAsServer()

    LobbyExtensionMain.gameListingConfig = ConfigHelper.initConfigFile(LobbyExtensionMain.gameListingPath, GameListingConfig())
    LobbyExtensionMain.gameSelectorGUI = GameSelectorGUI()

    // SNOWY_PLAINS
    LobbyExtensionMain.lobbyBiome = Biome.builder()
        .category(Biome.Category.PLAINS)
        .scale(0.05F)
        .depth(0.125F)
        .name(NamespaceID.from("minecraft:snowy_plains"))
        .temperature(0.05F)
        .downfall(0.3F)
        .precipitation(Biome.Precipitation.SNOW)
        .effects(
            BiomeEffects.builder()
                .fogColor(12638463)
                .waterColor(4159204)
                .waterFogColor(329011)
                .skyColor(8364543)
                .grassColorModifier(BiomeEffects.GrassColorModifier.NONE)
                .build()
        )
        .build()
    MinecraftServer.getBiomeManager().addBiome(LobbyExtensionMain.lobbyBiome)

    GameManager.registerGame<LobbyExtensionGame>(
        "lobby",
        Component.text("Lobby", NamedTextColor.GREEN),
        showsInSlashPlay = false
    )

    val visibleEntries = LobbyExtensionMain.gameListingConfig.gameListings.entries.filter { it.value.npcVisible }
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

        LobbyExtensionMain.npcs.add(PacketNPC(
            circleCenter.add(Pos(x, 0.0, -z)).withLookAt(lookingPos),
            hologramLines,
            it.key,
            if (it.value.npcSkinValue.isNotEmpty() && it.value.npcSkinSignature.isNotEmpty()) PlayerSkin(it.value.npcSkinValue, it.value.npcSkinSignature) else null,
            EntityType.fromNamespaceId(it.value.npcEntityType)
        ))
    }

    val eventNode = MinecraftServer.getGlobalEventHandler()
    eventNode.addListener(PlayerSpawnEvent::class.java) { e ->
        val player = e.player

        if (e.isFirstSpawn) {

            if (player.username == "brayjamin") {
                player.sendMessage("sus")
            }

            if (player.displayName != null) player.sendMessage(
                Component.text()
                    .append(Component.text("Welcome, ", NamedTextColor.GRAY))
                    .append(player.displayName!!)
                    .append(Component.text(", to ", NamedTextColor.GRAY))
                    .append(miniMessage.deserialize("<bold><gradient:gold:light_purple>EmortalMC"))
//                        .append(Component.newline())
//                        .append("There are currently ")
            )

            player.scheduler().buildTask {

                player.showFirework(
                    player.instance!!,
                    player.position.add(0.0, 1.0, 0.0),
                    mutableListOf(
                        FireworkEffect(
                            false,
                            false,
                            FireworkEffectType.LARGE_BALL,
                            mutableListOf(Color(NamedTextColor.LIGHT_PURPLE)),
                            mutableListOf(Color(NamedTextColor.GOLD))
                        )
                    )
                )
            }.delay(TaskSchedule.millis(500)).schedule()
        }
    }

    val jedisScope = CoroutineScope(Dispatchers.IO)
    jedisScope.launch {
        val registerGamePubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                val args = message.split(" ")
                val gameName = args[0]

                LobbyExtensionMain.playerCountCache[gameName] = 0
                LobbyExtensionMain.gameSelectorGUI.refreshPlayers(gameName, 0)
                val games = GameManager.getGames("lobby")!!
                games.forEach {
                    val lobbyGame = it as LobbyExtensionGame
                    lobbyGame.refreshHolo(gameName, 0)
                }
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

                LobbyExtensionMain.playerCountCache[gameName] = playerCount
                LobbyExtensionMain.gameSelectorGUI.refreshPlayers(gameName, playerCount)
                val games = GameManager.getGames("lobby")!!

                games.forEach {
                    val lobbyGame = it as LobbyExtensionGame
                    lobbyGame.refreshHolo(gameName, playerCount)
                }
            }
        }
        JedisStorage.jedis?.subscribe(playerCountPubSub, "playercount")
    }

    val cm = MinecraftServer.getCommandManager()

    cm.register(SpawnCommand)
    cm.register(SitCommand)
    cm.register(MountCommand)
    cm.register(FireworkCommand)
    cm.register(StackCommand)

    cm.register(StartOccurrence)
    cm.register(BlanksCommand)

    cm.register(MusicCommand())
    cm.register(LoopCommand())
}

class LobbyExtensionMain  {

    companion object {
        val playerCountCache = ConcurrentHashMap<String, Int>()
        val npcs = CopyOnWriteArrayList<PacketNPC>()

        lateinit var gameListingConfig: GameListingConfig
        val gameListingPath = Path.of("./gameListings.json")

        lateinit var gameSelectorGUI: GameSelectorGUI

        lateinit var lobbyBiome: Biome

        val buttonPresses = AtomicLong(0)
    }

}