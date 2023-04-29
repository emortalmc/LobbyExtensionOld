package dev.emortal.lobby.modules

import dev.emortal.lobby.commands.*
import dev.emortal.nbstom.commands.LoopCommand
import dev.emortal.nbstom.commands.MusicCommand
import net.minestom.server.MinecraftServer

fun commandLoader() {
    val cm = MinecraftServer.getCommandManager()

    cm.register(SpawnCommand)
    cm.register(SitCommand)
    cm.register(FireworkCommand)
    cm.register(StackCommand)

    cm.register(StartOccurrence)
    cm.register(BlanksCommand)

    cm.register(MusicCommand())
    cm.register(LoopCommand())
}