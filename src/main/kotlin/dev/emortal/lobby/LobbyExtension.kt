package dev.emortal.lobby

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGame
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.lobby.commands.*
import dev.emortal.lobby.config.ConfigurationHelper
import dev.emortal.lobby.config.GameListing
import dev.emortal.lobby.config.GameListingConfig
import dev.emortal.lobby.games.LobbyGame
import dev.emortal.lobby.inventories.MusicPlayerInventory
import dev.emortal.lobby.util.showFirework
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.TeamBuilder
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.clone
import java.nio.file.Path
import java.time.Duration

class LobbyExtension : Extension() {

    companion object {
        val occupiedSeats = mutableSetOf<Point>()
        val armourStandSeatMap = hashMapOf<Entity, Point>()
        val playerMusicInvMap = hashMapOf<Player, Inventory>()

        lateinit var gameListingConfig: GameListingConfig
    }

    override fun initialize() {
        // epic shutdown thing
        Manager.connection.shutdownText = Component.text()
            .append("<gradient:light_purple:gold><bold>EmortalMC\n\n".asMini())
            .append(Component.text("Server is shutting down.", NamedTextColor.RED))
            .append(Component.text("\n\ndiscord.gg/TZyuMSha96", NamedTextColor.DARK_GRAY))
            .build()

        val gameListingsPath = Path.of("./gameListings.json")
        gameListingConfig = ConfigurationHelper.initConfigFile(gameListingsPath, GameListingConfig())

        GameManager.registeredGameMap.forEach {
            if (!gameListingConfig.gameListings.contains(it.value.gameName)) {
                gameListingConfig.gameListings[it.value.gameName] = GameListing()
            }
        }

        ConfigurationHelper.writeObjectToPath(gameListingsPath, gameListingConfig)

        GameManager.registerGame<LobbyGame>(
            eventNode,
            "lobby",
            Component.text("Lobby"),
            false,
            WhenToRegisterEvents.IMMEDIATELY,
            GameOptions(
                maxPlayers = 50,
                minPlayers = 0,
                countdownSeconds = 0,
                canJoinDuringGame = true,
                showScoreboard = false
            )
        )

        MinecraftServer.setBrandName("§6Minestom ${MinecraftServer.VERSION_NAME}§r")

        eventNode.listenOnly<PlayerLoginEvent> {
            val game = GameManager.findOrCreateGame(player, "lobby")
            setSpawningInstance(game.instance)
            player.respawnPoint = game.spawnPosition

            player.scheduleNextTick {
                player.joinGame(game)
            }

            playerMusicInvMap[player] = MusicPlayerInventory.inventory.clone()
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {

                player.sendMessage(
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
        PerformanceCommand.register()
        RulesCommand.register()
        SpawnCommand.register()
        SitCommand.register()
        VersionCommand.register()

        logger.info("[LobbyExtension] Initialized!")
    }

    override fun terminate() {
        DiscCommand.unregister()
        PerformanceCommand.unregister()
        RulesCommand.unregister()
        SpawnCommand.unregister()
        SitCommand.unregister()
        VersionCommand.unregister()

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
                    .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("${it.latency}ms", NamedTextColor.GREEN))
                    .append(Component.text("\n└${" ".repeat(50)}", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("┘ ", NamedTextColor.GOLD))
            )
        }
    }

}