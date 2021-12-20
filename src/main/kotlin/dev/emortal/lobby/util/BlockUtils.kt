package dev.emortal.lobby.util

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.BlockChangePacket

fun Player.setBlock(point: Point, block: Block) {
    sendPacket(BlockChangePacket(point, block.stateId().toInt()))
}
