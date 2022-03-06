package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.lobby.commands.MountCommand.mountMap
import dev.emortal.lobby.mount.DolphinMount
import dev.emortal.lobby.mount.Mount
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import world.cepi.kstom.command.kommand.Kommand

object MountCommand : Kommand({

    onlyPlayers

    // If no arguments given, open inventory
    default {
        if (!sender.hasLuckPermission("lobby.mount")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@default
        }
        if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
            player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
            return@default
        }

        val mount = DolphinMount()
        mount.spawn(player.instance!!, player)
        mountMap[player]?.destroy()
        mountMap[player] = mount
    }
}, "mount") {

    val mountMap = mutableMapOf<Player, Mount>()

}