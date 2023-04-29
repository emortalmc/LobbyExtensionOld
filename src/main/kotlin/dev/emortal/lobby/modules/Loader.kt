package dev.emortal.lobby.modules

import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val LOGGER = LoggerFactory.getLogger("Loader")

internal val loaders: Array<() -> Unit> = arrayOf(
    ::clickySignLoader,
    ::commandLoader,
    ::eventsLoader,
    ::lightsOutLoader,
    ::npcLoader,
    ::playerThrowerLoader,
    ::seatingLoader
)

/** Loads all the loaders from the loader package. */
internal fun loadLoaders() = loaders.forEach {
    try {
        it()
    } catch (e: Exception) {
        LOGGER.error("Loader ${it.javaClass.simpleName} failed to load. Please file an issue on the Sabre github.", e)
        exitProcess(1)
    }
}