package dev.emortal.lobby.modules

import dev.emortal.lobby.LobbyMain
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.batch.BatchOption
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.Direction
import java.util.concurrent.ThreadLocalRandom


private const val lightsOutX = 5
private const val lightsOutY = 64
private const val lightsOutZ = -12

private val lightsOutGrid = Array(5) { BooleanArray(5) { true } }

fun lightsOutLoader() {
    val eventNode = MinecraftServer.getGlobalEventHandler()

    val initBatch = AbsoluteBlockBatch(BatchOption().setSendUpdate(false))
    for (x in 5..9) {
        for (y in -12..-8) {
            initBatch.setBlock(x, 64, y, Block.REDSTONE_LAMP)
        }
    }
    initBatch.setBlock(7, 65, -6, Block.BIRCH_BUTTON.withProperty("face", "floor").withProperty("facing", "north"))

    repeat(20) {
        val rand = ThreadLocalRandom.current()
        lightsOutClick(initBatch, rand.nextInt(0, 5), rand.nextInt(0, 5))
    }
    initBatch.apply(LobbyMain.instance, null)

    eventNode.addListener(PlayerBlockInteractEvent::class.java) { e ->
        val block = e.block
        val blockPos = e.blockPosition
        val player = e.player

        if (e.hand != Player.Hand.MAIN) return@addListener

        if (block.compare(Block.BIRCH_BUTTON)) {
            e.isCancelled = true

            val batch =
                AbsoluteBlockBatch(BatchOption().setSendUpdate(false)) // update is sent later to fix button anyway

            repeat(20) {
                val rand = ThreadLocalRandom.current()
                lightsOutClick(batch, rand.nextInt(0, 5), rand.nextInt(0, 5))
            }

            batch.apply(e.instance) {
                e.instance.getChunkAt(blockPos)?.sendChunk()
            }
        }

        if (block.compare(Block.REDSTONE_LAMP)) {
            val batch = AbsoluteBlockBatch()
            lightsOutClick(batch, blockPos.blockX() - lightsOutX, blockPos.blockZ() - lightsOutZ)
            batch.apply(e.instance) {}

            player.playSound(
                Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.MASTER, 1f, 1.5f),
                Sound.Emitter.self()
            )
        }
    }
}

private fun lightsOutClick(batch: AbsoluteBlockBatch, x: Int, y: Int) {
    Direction.HORIZONTAL.forEach {
        val newX = x + it.normalX()
        val newY = y + it.normalZ()
        if (newX !in 0 until 5) return@forEach
        if (newY !in 0 until 5) return@forEach

        val newValue = !lightsOutGrid[newX][newY]
        lightsOutGrid[newX][newY] = newValue

        batch.setBlock(lightsOutX + newX, 64, lightsOutZ + newY, Block.REDSTONE_LAMP.withProperty("lit", newValue.toString()))
    }

    lightsOutGrid[x][y] = !lightsOutGrid[x][y]
    batch.setBlock(lightsOutX + x, lightsOutY, lightsOutZ + y, Block.REDSTONE_LAMP.withProperty("lit", lightsOutGrid[x][y].toString()))
}