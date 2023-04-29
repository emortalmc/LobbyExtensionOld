package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.lobby.LobbyMain
import dev.emortal.lobby.occurrences.ChatOccurrence
import dev.emortal.lobby.occurrences.Occurrence
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player
import net.minestom.server.timer.TaskSchedule

object StartOccurrence : Command("startoccurrence") {

    init {
        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
                player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            if (player.username != "emortaldev") return@setDefaultExecutor

            val occurrence = ChatOccurrence()
            occurrence.start()

            Occurrence.occurrenceStopTask = LobbyMain.instance.scheduler().buildTask {
                Occurrence.currentOccurrence?.stop()
            }.delay(TaskSchedule.seconds(40)).schedule()
        }
    }

}