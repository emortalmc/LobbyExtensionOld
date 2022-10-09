package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.lobby.util.showFireworkWithDuration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import world.cepi.kstom.command.kommand.Kommand
import java.awt.Color
import java.util.concurrent.ThreadLocalRandom

object FireworkCommand : Command("fw") {

    init {
        setCondition { sender, _ ->
            if (sender is Player) {
                return@setCondition sender.username == "emortaldev"
            }
            false
        }

        setDefaultExecutor { sender, _ ->
            val player = sender as? Player ?: return@setDefaultExecutor

            if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
                player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            if (player.username != "emortaldev") return@setDefaultExecutor

            for (x in 0..10) {
                for (y in 0..5) {
                    for (z in 0..10) {
                        val random = ThreadLocalRandom.current()
                        val effects = mutableListOf(
                            FireworkEffect(
                                false,//random.nextBoolean(),
                                false,//random.nextBoolean(),
                                FireworkEffectType.values().random(),
                                listOf(net.minestom.server.color.Color(Color.HSBtoRGB(random.nextFloat(), 1f, 1f))),
                                listOf(net.minestom.server.color.Color(Color.HSBtoRGB(random.nextFloat(), 1f, 1f)))
                            )
                        )
                        player.instance!!.players.showFireworkWithDuration(player.instance!!, player.position.add(x.toDouble() / 1.5, z.toDouble() / 1.5, y.toDouble() / 1.5), 10, effects)

                    }
                }
            }
        }
    }

}