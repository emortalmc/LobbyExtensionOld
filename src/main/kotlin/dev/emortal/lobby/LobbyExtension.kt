package dev.emortal.lobby

import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.RedisStorage.redisson
import dev.emortal.lobby.commands.*
import dev.emortal.lobby.config.GameListingConfig
import dev.emortal.lobby.games.LobbyGame
import dev.emortal.lobby.inventories.GameSelectorGUI
import dev.emortal.lobby.inventories.MusicPlayerInventory
import dev.emortal.lobby.util.showFirework
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.color.Color
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.clone
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class LobbyExtension : Extension() {

    companion object {
        val playerMusicInvMap = ConcurrentHashMap<Player, Inventory>()

        val npcs = ConcurrentHashMap<String, PacketNPC>()

        lateinit var gameListingConfig: GameListingConfig
        val gameListingPath = Path.of("./gameListings.json")

        lateinit var gameSelectorGUI: GameSelectorGUI

        val playerCountCache = ConcurrentHashMap<String, Int>()
    }

    override fun initialize() {
        gameListingConfig = ConfigHelper.initConfigFile(gameListingPath, GameListingConfig())
        gameSelectorGUI = GameSelectorGUI()

        GameManager.registerGame<LobbyGame>(
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

        gameListingConfig.gameListings.entries.forEach {
            val hologramLines = it.value.npcTitles.map(String::asMini).toMutableList()

            hologramLines.add(Component.text("0 online", NamedTextColor.GRAY))

            npcs[it.key] = PacketNPC(
                it.value.npcPosition,
                hologramLines,
                it.key,
                if (it.value.npcSkinValue.isNotEmpty() && it.value.npcSkinSignature.isNotEmpty()) PlayerSkin(it.value.npcSkinValue, it.value.npcSkinSignature) else null,
                EntityType.fromNamespaceId(it.value.npcEntityType)
            )
        }

        redisson?.getTopic("playercount")?.addListenerAsync(String::class.java) { channel, msg ->
            val args = msg.split(" ")
            val gameName = args[0]
            val playerCount = args[1].toInt()

            playerCountCache[gameName] = playerCount
            gameSelectorGUI.refreshPlayers(gameName, playerCount)

            val games = GameManager.gameMap["lobby"]!!
            games.forEach {
                val lobbyGame = it as LobbyGame
                lobbyGame.refreshHolo(gameName, playerCount)
            }
        }

        eventNode.listenOnly<PlayerLoginEvent> {
            playerMusicInvMap[player] = MusicPlayerInventory.inventory.clone()
        }

        /*val susNBS = NBS(Path.of("./nbs/sus.nbs"))
        eventNode.listenOnly<PlayerChatEvent> {
            if (message.lowercase().contains("sus")) {
                NBS.stopPlaying(player)
                DiscCommand.stopPlayingTaskMap[player]?.cancel()
                DiscCommand.stopPlayingTaskMap.remove(player)

                NBS.play(susNBS, player)
            }
        }*/

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {

                if (player.username == "brayjamin") {
                    //NBS.play(susNBS, player)
                    player.sendMessage("sus")
                }

                if (player.displayName != null) player.sendMessage(
                    Component.text()
                        .append(Component.text("Welcome, ", NamedTextColor.GRAY))
                        .append(player.displayName!!)
                        .append(Component.text(", to ", NamedTextColor.GRAY))
                        .append("<bold><gradient:gold:light_purple>EmortalMC".asMini())
                )

                Manager.scheduler.buildTask {

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

        eventNode.listenOnly<PlayerDisconnectEvent> {
            playerMusicInvMap.remove(player)
        }

        MusicPlayerInventory.init()
        DiscCommand.register()
        SpawnCommand.register()
        SitCommand.register()
        MountCommand.register()
        FireworkCommand.register()
        LoopCommand.register()

        DiscCommand.refreshSongs()

        StartOccurrence.register()
        BlanksCommand.register()

        logger.info("[LobbyExtension] Initialized!")
    }

    override fun terminate() {
        DiscCommand.unregister()
        SpawnCommand.unregister()
        SitCommand.unregister()
        MountCommand.unregister()
        FireworkCommand.unregister()
        LoopCommand.unregister()

        StartOccurrence.unregister()
        BlanksCommand.unregister()

        logger.info("[LobbyExtension] Terminated!")
    }

}