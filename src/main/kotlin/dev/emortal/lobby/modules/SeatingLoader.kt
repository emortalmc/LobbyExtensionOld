package dev.emortal.lobby.modules

import dev.emortal.lobby.SeatEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import java.util.concurrent.CopyOnWriteArraySet


val armourStandSeatList = CopyOnWriteArraySet<Point>()
fun seatingLoader() {
    val eventNode = MinecraftServer.getGlobalEventHandler();

    eventNode.addListener(PlayerBlockInteractEvent::class.java) { e ->
        val block = e.block
        val blockPos = e.blockPosition
        val player = e.player

        if (e.hand != Player.Hand.MAIN) return@addListener

        if (block.name().contains("stair", true)) {
            if (player.vehicle != null) return@addListener
            if (armourStandSeatList.contains(blockPos)) {
                player.sendActionBar(Component.text("You can't sit on someone's lap", NamedTextColor.RED))
                return@addListener
            }
            if (block.getProperty("half") == "top") return@addListener
            if (!e.instance.getBlock(blockPos.add(0.0, 1.0, 0.0)).compare(Block.AIR)) return@addListener

            val armourStand = SeatEntity {
                armourStandSeatList.remove(blockPos)
            }

            val spawnPos = blockPos.add(0.5, 0.3, 0.5)
            val yaw = when (block.getProperty("facing")) {
                "east" -> 90f
                "south" -> 180f
                "west" -> -90f
                else -> 0f
            }

            armourStand.setInstance(e.instance, Pos(spawnPos, yaw, 0f))
                .thenRun {
                    armourStand.addPassenger(player)
                }

            armourStandSeatList.add(blockPos)
        }
    }

    eventNode.addListener(PlayerPacketEvent::class.java) { e ->
        val packet = e.packet
        val player = e.player

        if (packet is ClientSteerVehiclePacket) {
            if (packet.flags.toInt() == 2) {
                if (player.vehicle != null && player.vehicle !is Player) {
                    val entity = player.vehicle!!
                    entity.removePassenger(player)

                }
                return@addListener
            }
        }
    }
}