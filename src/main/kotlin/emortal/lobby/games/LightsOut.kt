package emortal.lobby.games

import emortal.immortal.game.Game
import emortal.immortal.game.GameOptions
import emortal.lobby.LobbyExtension
import emortal.lobby.util.InbetweenUtil
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.Direction
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.playSound
import java.time.Duration

class LightsOut(options: GameOptions) : Game(options) {

    companion object {
        private val mini = MiniMessage.get()

        private val topLeftCornerPos = Pos(-2.0, 80.0, -33.0)
        private val bottomRightCornerPos = Pos(2.0, 80.0, -29.0)

        private val litRedstoneLamp = Block.REDSTONE_LAMP.withProperty("lit", "true")
        private val unLitRedstoneLamp = Block.REDSTONE_LAMP
        private val pointsBetween = InbetweenUtil.pointsBetween(topLeftCornerPos, bottomRightCornerPos)
    }

    override fun playerJoin(player: Player) {
        Manager.scheduler.buildTask {
            val batch = AbsoluteBlockBatch()

            pointsBetween.forEach {
                batch.setBlock(it, litRedstoneLamp)
            }

            batch.apply(instance) {}
        }.delay(Duration.ofSeconds(2)).schedule()
    }

    override fun playerLeave(player: Player) {

    }

    override fun registerEvents() {
        eventNode.listenOnly<PlayerBlockInteractEvent> {
            if (hand != Player.Hand.MAIN) return@listenOnly

            if (block.compare(Block.REDSTONE_LAMP)) {
                audience.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_PRESSURE_PLATE_CLICK_ON, Sound.Source.AMBIENT, 1f, 1f), blockPosition)

                instance.setBlock(blockPosition, if (isLampLit(block)) unLitRedstoneLamp else litRedstoneLamp)

                for (direction in Direction.HORIZONTAL) {
                    val directionPosition = blockPosition.add(direction.normalX().toDouble(), 0.0, direction.normalZ().toDouble())
                    val directionBlock = instance.getBlock(directionPosition)

                    if (directionBlock.compare(Block.REDSTONE_LAMP)) {
                        instance.setBlock(directionPosition, if (isLampLit(directionBlock)) unLitRedstoneLamp else litRedstoneLamp)
                    }

                }

                if (allLampsUnlit()) {
                    victory(player)
                }
            }
        }
    }

    val audience = Audience.audience(players)

    override fun start() {


        audience.sendMessage(mini.parse("<gray>A game of <gradient:light_purple:gold>LIGHTS OUT</gradient> has started!"))
        audience.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.AMBIENT, 1f, 1f))
    }

    fun victory(player: Player) {
        audience.sendMessage(mini.parse("<gray><gradient:light_purple:gold>${player.username}</gradient> won <gradient:light_purple:gold>LIGHTS OUT</gradient>!"))
        destroy()
    }

    override fun postDestroy() {

    }

    private fun allLampsUnlit() = pointsBetween
            .all { !isLampLit(LobbyExtension.lobbyInstance.getBlock(it)) }

    private fun isLampLit(block: Block) = block.getProperty("lit") == "true"

}