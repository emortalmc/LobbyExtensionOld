package dev.emortal.lobby.commands

import net.minestom.server.command.builder.Command
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player

object SpawnCommand : Command("spawn", "lobby", "hub", "l") {

    val SPAWN_POINT = Pos(0.5, 65.0, 0.5, 180f, 0f)

    init {
        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (player.vehicle?.entityType != EntityType.PLAYER) player.vehicle?.remove()
            else player.vehicle?.removePassenger(player)

            player.teleport(SPAWN_POINT)
        }
    }
}