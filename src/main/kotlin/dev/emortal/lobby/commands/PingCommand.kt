package dev.emortal.lobby.commands

import world.cepi.kstom.adventure.sendMiniMessage
import world.cepi.kstom.command.kommand.Kommand

object PingCommand : Kommand({

    onlyPlayers

    default {
        player.sendMiniMessage("<gray>Your ping is: <white>${player.latency}</white> (likely inaccurate)")
    }

}, "ping")