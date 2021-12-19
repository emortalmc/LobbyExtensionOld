package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.lobby.LobbyExtension.Companion.SPAWN_POINT
import net.kyori.adventure.sound.Sound
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.command.kommand.Kommand

object SpawnCommand : Kommand({

    onlyPlayers

    default {
        if (player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
            player.teleport(SPAWN_POINT)
            player.playSound(Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
        } else {
            player.joinGameOrNew("lobby")
            player.playSound(Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
        }
    }
}, "spawn", "lobby", "hub", "l")