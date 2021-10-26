package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.lobby.LobbyExtension.Companion.SPAWN_POINT
import dev.emortal.lobby.games.LobbyGame
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand
import java.time.Duration

object SpawnCommand : Kommand({

    onlyPlayers

    default {
        if (!sender.isPlayer) return@default

        val player = sender.asPlayer()

        Manager.scheduler.buildTask {
            if (player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
                player.teleport(SPAWN_POINT)
            } else {
                player.joinGameOrNew<LobbyGame>()
            }
        }.delay(Duration.ofMillis(500)).schedule()
    }
}, "spawn", "lobby", "hub", "l")