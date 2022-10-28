package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager.game
import net.kyori.adventure.sound.Sound
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.sound.SoundEvent

object SpawnCommand : Command("spawn", "lobby", "hub", "l") {
    init {
        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (player.vehicle?.entityType != EntityType.PLAYER) player.vehicle?.remove()
            else player.vehicle?.removePassenger(player)

            player.teleport(player.game!!.getSpawnPosition(player)).thenRun {
                player.playSound(Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
            }
        }
    }
}