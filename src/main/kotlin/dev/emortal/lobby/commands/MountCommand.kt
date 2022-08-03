package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.lobby.commands.MountCommand.mountMap
import dev.emortal.lobby.mount.DolphinMount
import dev.emortal.lobby.mount.Mount
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.EntityType
import world.cepi.kstom.command.kommand.Kommand
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object MountCommand : Kommand({

    onlyPlayers()

    condition {
        sender.hasLuckPermission("lobby.mount")
    }

    playerCallbackFailMessage = {
        it.sendMessage(Component.text("No permission", NamedTextColor.RED))
    }

    default {
        if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
            player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
            return@default
        }

        if (player.vehicle?.entityType != EntityType.PLAYER) player.vehicle?.remove()
        else player.vehicle?.removePassenger(player)

        val mount = DolphinMount()
        mount.spawn(player.instance!!, player)
        mountMap[player.uuid]?.destroy()
        mountMap[player.uuid] = mount
    }
}, "mount") {

    val mountMap = ConcurrentHashMap<UUID, Mount>()

}