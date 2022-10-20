package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.lobby.mount.DolphinMount
import dev.emortal.lobby.mount.Mount
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object MountCommand : Command("mount") {

    val mountMap = ConcurrentHashMap<UUID, Mount>()

    init {
        setCondition { sender, _ ->
            sender.hasLuckPermission("lobby.mount")
        }

        setDefaultExecutor { sender, context ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
                player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            if (player.vehicle?.entityType != EntityType.PLAYER) player.vehicle?.remove()
            else player.vehicle?.removePassenger(player)

            val mount = DolphinMount()
            mount.spawn(player.instance!!, player)
            mountMap[player.uuid]?.destroy()
            mountMap[player.uuid] = mount
        }
    }
}