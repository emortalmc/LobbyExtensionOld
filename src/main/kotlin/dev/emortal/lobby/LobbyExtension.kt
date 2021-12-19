package dev.emortal.lobby

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.lobby.commands.*
import dev.emortal.lobby.config.ConfigurationHelper
import dev.emortal.lobby.config.GameListing
import dev.emortal.lobby.config.GameListingConfig
import dev.emortal.lobby.games.LobbyGame
import dev.emortal.lobby.inventories.MusicPlayerInventory
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.AreaEffectCloudMeta
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.inventory.Inventory
import net.minestom.server.timer.Task
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

        val chatHologramMap = hashMapOf<Player, Pair<Entity, Task>>()

        val SPAWN_POINT = Pos(0.5, 65.0, 0.5, 180f, 0f)

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
                gameListingConfig.gameListings[it.value.gameName] = GameListing(it.value.gameName)
            }
        }

        ConfigurationHelper.writeObjectToPath(gameListingsPath, gameListingConfig)

        GameManager.registerGame<LobbyGame>(
            eventNode,
            "lobby",
            Component.empty(),
            true,
            WhenToRegisterEvents.IMMEDIATELY,
            GameOptions(
                maxPlayers = 50,
                minPlayers = 0,
                countdownSeconds = 0,
                canJoinDuringGame = true,
                showScoreboard = false
            )
        )

        eventNode.listenOnly<PlayerLoginEvent> {
            val game = player.joinGameOrNew("lobby")
            setSpawningInstance(game.instance)
            player.respawnPoint = SPAWN_POINT

            playerMusicInvMap[player] = MusicPlayerInventory.inventory.clone()
        }

        eventNode.listenOnly<PlayerChatEvent> {
            chatHologramMap[player]?.first?.remove()
            chatHologramMap[player]?.second?.cancel()

            val entity = Entity(EntityType.AREA_EFFECT_CLOUD)

            val meta = entity.entityMeta as AreaEffectCloudMeta

            val playerName = player.displayName ?: Component.text(player.username)

            meta.radius = 0f
            meta.isHasNoGravity = true
            meta.customName = playerName.append(Component.text(": ")).append(Component.text(if (message.length > 20) message.take(17) + "..." else message))
            meta.isCustomNameVisible = true

            player.addPassenger(entity)

            val task = Manager.scheduler.buildTask {
                entity.remove()
                chatHologramMap.remove(player)
            }.delay(Duration.ofSeconds(5)).schedule()

            chatHologramMap[player] = Pair(entity, task)


        }

        MusicPlayerInventory.init()
        DiscCommand.register()
        PerformanceCommand.register()
        PingCommand.register()
        RulesCommand.register()
        SpawnCommand.register()
        SitCommand.register()
        VersionCommand.register()

        logger.info("[LobbyExtension] Initialized!")
    }

    override fun terminate() {
        DiscCommand.unregister()
        PerformanceCommand.unregister()
        PingCommand.unregister()
        RulesCommand.unregister()
        SpawnCommand.unregister()
        SitCommand.unregister()
        VersionCommand.unregister()

        logger.info("[LobbyExtension] Terminated!")
    }

}