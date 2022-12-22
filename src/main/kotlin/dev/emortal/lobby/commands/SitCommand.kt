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

object SitCommand : Command("sit") {

    init {
        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
                player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            if (player.vehicle != null) return@setDefaultExecutor

            var i = 0
            while (true) {
                i++
                if (!player.instance!!.getBlock(player.position.blockX(), player.position.blockY() - i, player.position.blockZ()).compare(Block.AIR))
                    break

                if (i > 3) {
                    player.sendActionBar(Component.text("Couldn't reserve a seat", NamedTextColor.RED))
                    return@setDefaultExecutor
                }
            }

            val roundedPos = Pos(
                player.position.blockX().toDouble(),
                player.position.blockY().toDouble() - (i - 1),
                player.position.blockZ().toDouble()
            )

            val game = player.game as LobbyExtensionGame

            if (game.armourStandSeatList.contains(roundedPos)) {
                player.sendActionBar(Component.text("You can't sit on someone's lap", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            val armourStand = SeatEntity {
                game.armourStandSeatList.remove(roundedPos)
            }

            val spawnPos = roundedPos.add(0.5, -0.3, 0.5)
            armourStand.setInstance(player.instance!!, spawnPos.withYaw(player.position.yaw))
                .thenRun {
                    armourStand.addPassenger(player)
                }

            game.armourStandSeatList.add(roundedPos)
        }
    }

}