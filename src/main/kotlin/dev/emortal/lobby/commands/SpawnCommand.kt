package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.lobby.games.LobbyGame
import net.kyori.adventure.sound.Sound
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.command.kommand.Kommand

object SpawnCommand : Kommand({

    onlyPlayers

    default {
        val completableFuture = if (player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
            player.vehicle?.removePassenger(player)
            player.teleport(LobbyGame.spawnPoint)
        } else {
            player.joinGameOrNew("lobby")
        }

        completableFuture?.thenRun {
            player.playSound(Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
        }
    }
}, "spawn", "lobby", "hub", "l")