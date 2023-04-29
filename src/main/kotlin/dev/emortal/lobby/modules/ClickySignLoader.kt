package dev.emortal.lobby.modules

import dev.emortal.immortal.util.playSound
import dev.emortal.lobby.LobbyMain
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.batch.BatchOption
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag


fun clickySignLoader() {
    val eventNode = MinecraftServer.getGlobalEventHandler()

    val signPos = Pos(9.0, 66.0, -15.0)
    LobbyMain.instance.loadChunk(signPos).thenAccept {
        val componentText1 = Component.text("This sign has", NamedTextColor.BLACK)
        val componentText2 = Component.text("been clicked", NamedTextColor.BLACK)
        val componentText3 = Component.text(0, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
        val componentText4 = Component.text(" times", TextColor.color(212, 11, 212))

        it.setBlock(
            signPos,
            Block.BIRCH_WALL_SIGN
                .withProperty("facing", "south")
                .withTag(Tag.String("Text1"), LobbyMain.gsonSerializer.serialize(componentText1))
                .withTag(Tag.String("Text2"), LobbyMain.gsonSerializer.serialize(componentText2))
                .withTag(Tag.String("Text3"), LobbyMain.gsonSerializer.serialize(componentText3))
                .withTag(Tag.String("Text4"), LobbyMain.gsonSerializer.serialize(componentText4))
        )

        val batch = AbsoluteBlockBatch(BatchOption().setSendUpdate(false))

        batch.apply(LobbyMain.instance) {
            it.sendChunk()
        }

    }

    var buttonPresses = 0L
    eventNode.addListener(PlayerBlockInteractEvent::class.java) { e ->
        val block = e.block
        val blockPos = e.blockPosition
        val player = e.player

        if (e.hand != Player.Hand.MAIN) return@addListener

        if (block.compare(Block.BIRCH_WALL_SIGN)) {
            if (blockPos.sameBlock(Pos(9.0, 66.0, -15.0))) {
                player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.BLOCK, 0.75f, 2f), blockPos.add(0.5))

                val clickedNum = ++buttonPresses
                val component = Component.text(clickedNum, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                val newBlock = e.instance.getBlock(blockPos)
                    .withTag(Tag.String("Text3"), LobbyMain.gsonSerializer.serialize(component))

                e.instance.setBlock(blockPos, newBlock)
            }
        }
    }
}