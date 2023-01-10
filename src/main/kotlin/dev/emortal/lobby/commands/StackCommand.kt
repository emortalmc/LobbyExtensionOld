package dev.emortal.lobby.commands

import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player

object StackCommand : Command("stack") {

    init {
        setCondition { sender, _ ->
            sender.hasLuckPermission("lobby.mount")
        }

        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            var lastPlayer = player
            player.instance!!.players.forEach {
                if (player == it) return@forEach

                lastPlayer.addPassenger(it)
                lastPlayer = it
            }
        }
    }

}