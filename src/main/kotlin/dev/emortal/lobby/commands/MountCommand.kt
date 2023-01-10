package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.lobby.games.LobbyExtensionGame
import dev.emortal.lobby.mount.Mount
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player

object MountCommand : Command("mount") {


    init {
        val mountArg = ArgumentType.StringArray("mount").setSuggestionCallback { sender, context, suggestion ->
            Mount.registeredMap.keys.forEach {
                suggestion.addEntry(SuggestionEntry(it))
            }
        }

        setCondition { sender, _ ->
            sender.hasLuckPermission("lobby.mount")
        }

        addConditionalSyntax({ sender, _ ->
            sender.hasLuckPermission("lobby.mount")
        }, { sender, context ->
            val player = sender as? Player ?: return@addConditionalSyntax
            val game = player.game as? LobbyExtensionGame ?: return@addConditionalSyntax

            if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
                player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
                return@addConditionalSyntax
            }

            if (player.vehicle?.entityType != EntityType.PLAYER) player.vehicle?.remove()
            else player.vehicle?.removePassenger(player)

            val mount = context.get(mountArg).joinToString(separator = " ")
            val mountObj: Mount = Mount.registeredMap[mount]?.call() ?: return@addConditionalSyntax
            mountObj.spawn(player.instance!!, player)
            game.mountMap[player.uuid] = mountObj
        }, mountArg)
    }
}