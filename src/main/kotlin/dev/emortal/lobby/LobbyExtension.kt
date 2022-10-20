package dev.emortal.lobby

import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.JedisStorage.jedis
import dev.emortal.lobby.commands.*
import dev.emortal.lobby.config.GameListingConfig
import dev.emortal.lobby.games.LobbyExtensionGame
import dev.emortal.lobby.inventories.GameSelectorGUI
import dev.emortal.lobby.util.showFirework
import dev.emortal.nbstom.NBS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.IChunkLoader
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import org.tinylog.kotlin.Logger
import redis.clients.jedis.JedisPubSub
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.command.register
import world.cepi.kstom.command.unregister
import world.cepi.kstom.event.listenOnly
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LobbyExtension : Extension() {

    companion object {
        val npcs = CopyOnWriteArrayList<PacketNPC>()

        lateinit var gameListingConfig: GameListingConfig
        val gameListingPath = Path.of("./gameListings.json")

        lateinit var gameSelectorGUI: GameSelectorGUI

        val playerCountCache = ConcurrentHashMap<String, Int>()

        lateinit var sharedLoader: IChunkLoader
    }

    override fun initialize() {
        gameListingConfig = ConfigHelper.initConfigFile(gameListingPath, GameListingConfig())
        gameSelectorGUI = GameSelectorGUI()

        Logger.info("Preloading lobby world")
        sharedLoader = AnvilLoader("./lobby")

        GameManager.registerGame<LobbyExtensionGame>(
            "lobby",
            Component.text("Lobby", NamedTextColor.GREEN),
            showsInSlashPlay = false,
            canSpectate = false,
            WhenToRegisterEvents.IMMEDIATELY,
            GameOptions(
                maxPlayers = 50,
                minPlayers = 0,
                countdownSeconds = 0,
                canJoinDuringGame = true,
                showScoreboard = false,
                allowsSpectators = false
            )
        )

        gameListingConfig.gameListings.entries.forEachIndexed { i, it ->
            val hologramLines = it.value.npcTitles.map(String::asMini).toMutableList()

            hologramLines.add(Component.empty())

            val angle = i * (PI / (gameListingConfig.gameListings.size - 1))
            val circleSize = 3.7
            val circleCenter = Pos(0.5, 69.0, -29.0)
            val lookingPos = Pos(0.5, 69.0, -27.5)
            val x = cos(angle) * circleSize
            val z = sin(angle) * circleSize / 1.5

            npcs.add(PacketNPC(
                circleCenter.add(Pos(x, 0.0, -z)).withLookAt(lookingPos),
                hologramLines,
                it.key,
                if (it.value.npcSkinValue.isNotEmpty() && it.value.npcSkinSignature.isNotEmpty()) PlayerSkin(it.value.npcSkinValue, it.value.npcSkinSignature) else null,
                EntityType.fromNamespaceId(it.value.npcEntityType)
            ))
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val registerGamePubSub = object : JedisPubSub() {
                override fun onMessage(channel: String, message: String) {
                    val args = message.split(" ")
                    val gameName = args[0]

                    playerCountCache[gameName] = 0
                    gameSelectorGUI.refreshPlayers(gameName, 0)
                    val games = GameManager.gameMap["lobby"]!!.values
                    games.forEach {
                        val lobbyGame = it as LobbyExtensionGame
                        lobbyGame.refreshHolo(gameName, 0)
                    }
                }
            }
            jedis?.subscribe(registerGamePubSub, "registergame")

            val playerCountPubSub = object : JedisPubSub() {
                override fun onMessage(channel: String, message: String) {
                    val args = message.split(" ")
                    val gameName = args[0]
                    val playerCount = args[1].toInt()

                    playerCountCache[gameName] = playerCount
                    gameSelectorGUI.refreshPlayers(gameName, playerCount)
                    val games = GameManager.gameMap["lobby"]!!.values

                    games.forEach {
                        val lobbyGame = it as LobbyExtensionGame
                        lobbyGame.refreshHolo(gameName, playerCount)
                    }
                }
            }
            jedis?.subscribe(playerCountPubSub, "playercount")
        }
        


        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {

                if (player.username == "brayjamin") {
                    player.sendMessage("sus")
                }

                if (player.displayName != null) player.sendMessage(
                    Component.text()
                        .append(Component.text("Welcome, ", NamedTextColor.GRAY))
                        .append(player.displayName!!)
                        .append(Component.text(", to ", NamedTextColor.GRAY))
                        .append("<bold><gradient:gold:light_purple>EmortalMC".asMini())
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
                }.delay(Duration.ofMillis(500)).schedule()
            }
        }

        SpawnCommand.register()
        SitCommand.register()
        MountCommand.register()
        FireworkCommand.register()

        StartOccurrence.register()
        BlanksCommand.register()

        NBS.registerCommands()

        logger.info("[LobbyExtension] Initialized!")
    }

    override fun terminate() {
        SpawnCommand.unregister()
        SitCommand.unregister()
        MountCommand.unregister()
        FireworkCommand.unregister()

        StartOccurrence.unregister()
        BlanksCommand.unregister()

        logger.info("[LobbyExtension] Terminated!")
    }

}