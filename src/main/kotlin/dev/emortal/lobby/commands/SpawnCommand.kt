package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.lobby.LobbyExtension.Companion.SPAWN_POINT
import dev.emortal.lobby.games.LobbyGame
import world.cepi.kstom.command.kommand.Kommand

object SpawnCommand : Kommand({

    onlyPlayers

    default {
        if (!sender.isPlayer) return@default

        val player = sender.asPlayer()

        if (player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
            player.teleport(SPAWN_POINT)
        } else {
            player.joinGameOrNew<LobbyGame>()
        }
    }
}, "spawn", "lobby", "hub", "l")