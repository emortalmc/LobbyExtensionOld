package dev.emortal.lobby

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.config.GameListing
import dev.emortal.immortal.event.GameDestroyEvent
import dev.emortal.immortal.event.PlayerJoinGameEvent
import dev.emortal.immortal.event.PlayerLeaveGameEvent
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGame
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.lobby.commands.*
import dev.emortal.lobby.games.LobbyGame
import dev.emortal.lobby.inventories.MusicPlayerInventory
import dev.emortal.lobby.util.showFirework
import dev.emortal.nbstom.NBS
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.network.packet.client.play.ClientPongPacket
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.command.register
import world.cepi.kstom.command.unregister
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.clone
import java.nio.file.Path
import java.time.Duration
import java.util.*

class LobbyExtension : Extension() {

    companion object {
        val occupiedSeats = mutableSetOf<Point>()
        val armourStandSeatMap = hashMapOf<Entity, Point>()
        val playerMusicInvMap = hashMapOf<Player, Inventory>()

        val npcs = mutableMapOf<String, PacketNPC>()

        lateinit var lobbyInstance: InstanceContainer
    }

    override fun initialize() {
        lobbyInstance = Manager.instance.createInstanceContainer()
        lobbyInstance.chunkLoader = AnvilLoader("lobby")
        lobbyInstance.setTag(GameManager.doNotUnregisterTag, 1)

        Manager.connection.shutdownText = Component.text()
            .append("<gradient:light_purple:gold><bold>EmortalMC ".asMini())
            .append(Component.text("has shutdown. Possible downtime?", NamedTextColor.RED))
            .append(Component.text("\ndiscord.gg/TZyuMSha96", NamedTextColor.DARK_GRAY))
            .build()

        GameManager.registerGame<LobbyGame>(
            eventNode,
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
                showScoreboard = false
            )
        )

        ImmortalExtension.gameListingConfig.gameListings.entries.forEach {
            val hologramLines = it.value.npcTitles.map(String::asMini).toMutableList()

            hologramLines.add(Component.text("0 online", NamedTextColor.GRAY))

            npcs[it.key] = PacketNPC(
                it.value.npcPosition,
                hologramLines,
                it.key,
                if (it.value.npcSkinValue.isNotEmpty() && it.value.npcSkinSignature.isNotEmpty()) PlayerSkin(it.value.npcSkinValue, it.value.npcSkinSignature) else null
            )
        }

        MinecraftServer.setBrandName("§6Minestom ${MinecraftServer.VERSION_NAME}§r")

        eventNode.listenOnly<PlayerJoinGameEvent> {
            val games = GameManager.gameMap["lobby"] ?: return@listenOnly
            games.forEach {
                val lobbyGame = it as LobbyGame
                lobbyGame.refreshHolo(getGame())
            }
        }
        eventNode.listenOnly<PlayerLeaveGameEvent> {
            val games = GameManager.gameMap["lobby"] ?: return@listenOnly
            games.forEach {
                val lobbyGame = it as LobbyGame
                lobbyGame.refreshHolo(getGame())
            }
        }
        eventNode.listenOnly<GameDestroyEvent> {
            val games = GameManager.gameMap["lobby"] ?: return@listenOnly
            games.forEach {
                val lobbyGame = it as LobbyGame
                lobbyGame.refreshHolo(getGame())
            }
        }

        eventNode.listenOnly<PlayerLoginEvent> {
            val game = GameManager.findOrCreateGame(player, "lobby")
            setSpawningInstance(game.instance)
            player.respawnPoint = game.spawnPosition

            player.scheduleNextTick {
                player.joinGame(game)
            }

            playerMusicInvMap[player] = MusicPlayerInventory.inventory.clone()
        }

        val susNBS = NBS(Path.of("./nbs/sus.nbs"))
        eventNode.listenOnly<PlayerChatEvent> {
            if (message.lowercase().contains("sus")) {
                NBS.stopPlaying(player)
                DiscCommand.stopPlayingTaskMap[player]?.cancel()
                DiscCommand.stopPlayingTaskMap.remove(player)

                NBS.play(susNBS, player)
            }
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {

                if (player.username == "brayjamin") {
                    NBS.play(susNBS, player)
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

                player.refreshLatency(0)
                refreshTablist()
            }
        }
        eventNode.listenOnly<PlayerDisconnectEvent> {
            refreshTablist()
            playerMusicInvMap.remove(player)
        }


        MusicPlayerInventory.init()
        DiscCommand.register()
        DiscordCommand.register()
        RulesCommand.register()
        SpawnCommand.register()
        SitCommand.register()
        VersionCommand.register()
        MountCommand.register()

        DiscCommand.refreshSongs()

        logger.info("[LobbyExtension] Initialized!")
    }

    override fun terminate() {
        DiscCommand.unregister()
        DiscordCommand.unregister()
        RulesCommand.unregister()
        SpawnCommand.unregister()
        SitCommand.unregister()
        VersionCommand.unregister()
        MountCommand.unregister()

        logger.info("[LobbyExtension] Terminated!")
    }

    fun refreshTablist() {
        Manager.connection.onlinePlayers.forEach {
            it.sendPlayerListHeaderAndFooter(
                Component.text()
                    .append(Component.text("┌${" ".repeat(50)}", NamedTextColor.GOLD))
                    .append(Component.text("┐ ", NamedTextColor.LIGHT_PURPLE))
                    .append("\n<gradient:gold:light_purple><bold>EmortalMC".asMini())
                    .append(Component.text("\n", NamedTextColor.GRAY)),
                Component.text()
                    .append(Component.text("\n ", NamedTextColor.GRAY))
                    .append(Component.text("${Manager.connection.onlinePlayers.size} online", NamedTextColor.GRAY))
                    .append(Component.text("\n└${" ".repeat(50)}", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("┘ ", NamedTextColor.GOLD))
            )
        }
    }

}