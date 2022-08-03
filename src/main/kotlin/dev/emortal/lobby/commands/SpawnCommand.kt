package dev.emortal.lobby.commands

import dev.emortal.lobby.games.LobbyExtensionGame
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.EntityType
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.command.kommand.Kommand

object SpawnCommand : Kommand({

    onlyPlayers()

    default {
        if (player.vehicle?.entityType != EntityType.PLAYER) player.vehicle?.remove()
        else player.vehicle?.removePassenger(player)
        
        player.teleport(LobbyExtensionGame.spawnPoint).thenRun {
            player.playSound(Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
        }
    }
}, "spawn", "lobby", "hub", "l")