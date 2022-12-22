package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.lobby.games.LobbyExtensionGame
import dev.emortal.lobby.games.SeatEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.ChangeGameStatePacket
import net.minestom.server.tag.Tag

object SnowCommand : Command("snow") {

    private val snowTag = Tag.Boolean("snowing")

    init {
        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (player.hasTag(snowTag)) {
                player.sendMessage(Component.text("Stopped snowing!", NamedTextColor.GREEN))
                player.removeTag(snowTag)

                val packet = ChangeGameStatePacket(ChangeGameStatePacket.Reason.RAIN_LEVEL_CHANGE, 0f)
                player.sendPacket(packet)
            } else {
                player.sendMessage(Component.text("Began snowing!", NamedTextColor.GREEN))
                player.setTag(snowTag, true)

                val packet = ChangeGameStatePacket(ChangeGameStatePacket.Reason.RAIN_LEVEL_CHANGE, 1f)
                player.sendPacket(packet)
            }

        }
    }

}