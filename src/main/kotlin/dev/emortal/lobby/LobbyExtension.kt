package dev.emortal.lobby

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.game.GameOptions
import dev.emortal.lobby.commands.DiscCommand
import dev.emortal.lobby.commands.PingCommand
import dev.emortal.lobby.commands.SitCommand
import dev.emortal.lobby.commands.SpawnCommand
import dev.emortal.lobby.games.LobbyGame
import dev.emortal.lobby.inventories.MusicPlayerInventory
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.inventory.Inventory
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.clone

class LobbyExtension : Extension() {

    companion object {
        val occupiedSeats = mutableSetOf<Point>()
        val armourStandSeatMap = HashMap<Entity, Point>()
        val playerMusicInvMap = HashMap<Player, Inventory>()

        val SPAWN_POINT = Pos(0.5, 65.0, 0.5, 180f, 0f)
    }

    override fun initialize() {
        eventNode.listenOnly<PlayerLoginEvent> {
            val game = player.joinGameOrNew<LobbyGame>()
            setSpawningInstance(game.instance)
            player.respawnPoint = SPAWN_POINT

            playerMusicInvMap[player] = MusicPlayerInventory.inventory.clone()
        }


        GameManager.registerGame<LobbyGame>(
            eventNode,
            "lobby",
            Component.empty(),
            true,
            GameOptions(
                maxPlayers = 50,
                minPlayers = 0,
                countdownSeconds = 0,
                canJoinDuringGame = true,
                showScoreboard = false
            )
        )

        MusicPlayerInventory.init()
        DiscCommand.register()
        PingCommand.register()
        SpawnCommand.register()
        SitCommand.register()

        logger.info("[LobbyExtension] Initialized!")
    }

    override fun terminate() {
        DiscCommand.unregister()
        PingCommand.unregister()
        SpawnCommand.unregister()
        SitCommand.unregister()

        logger.info("[LobbyExtension] Terminated!")
    }

}