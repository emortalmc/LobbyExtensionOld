package dev.emortal.lobby.occurrences

import net.minestom.server.timer.Task

sealed class Occurrence {

    companion object {
        var currentOccurrence: Occurrence? = null
        var occurrenceStopTask: Task? = null
    }
    val taskList = mutableListOf<Task>()

    fun start() {
        currentOccurrence = this@Occurrence

        started()
    }

    abstract fun started()

    fun stop() {
        taskList.forEach {
            it.cancel()
        }
        taskList.clear()
        occurrenceStopTask?.cancel()
        currentOccurrence = null
        occurrenceStopTask = null

        stopped()
    }

    abstract fun stopped()

}