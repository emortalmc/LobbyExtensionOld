package dev.emortal.lobby.occurrences

import dev.emortal.lobby.games.LobbyExtensionGame
import net.minestom.server.timer.Task

sealed class Occurrence {

    val taskList = mutableListOf<Task>()

    fun start(game: LobbyExtensionGame) {
        game.currentOccurrence = this@Occurrence

        started(game)
    }

    abstract fun started(game: LobbyExtensionGame)

    fun stop(game: LobbyExtensionGame) {
        taskList.forEach {
            it.cancel()
        }
        taskList.clear()
        game.occurrenceStopTask?.cancel()
        game.currentOccurrence = null
        game.occurrenceStopTask = null

        stopped(game)
    }

    abstract fun stopped(game: LobbyExtensionGame)

}