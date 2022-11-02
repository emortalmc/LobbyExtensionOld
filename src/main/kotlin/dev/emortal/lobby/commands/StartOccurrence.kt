package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.lobby.games.LobbyExtensionGame
import dev.emortal.lobby.occurrences.ChatOccurrence
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player
import java.time.Duration

object StartOccurrence : Command("startoccurrence") {

    init {
        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
                player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            if (player.username != "emortaldev") return@setDefaultExecutor

            val lobbyGame = (player.game ?: return@setDefaultExecutor) as? LobbyExtensionGame ?: return@setDefaultExecutor
            val occurrence = ChatOccurrence()
            occurrence.start(lobbyGame)

            lobbyGame.occurrenceStopTask = lobbyGame.instance!!.scheduler().buildTask {
                lobbyGame.currentOccurrence?.stop(lobbyGame)
            }.delay(Duration.ofSeconds(40)).schedule()
        }
    }

}