package dev.emortal.lobby.modules

import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerStopSneakingEvent
import net.minestom.server.sound.SoundEvent

fun playerThrowerLoader() {
    val eventNode = MinecraftServer.getGlobalEventHandler();

    eventNode.addListener(PlayerEntityInteractEvent::class.java) { e ->
        val player = e.player
        val target = e.target as? Player ?: return@addListener

        if (!player.itemInMainHand.isAir) return@addListener
        if (e.hand != Player.Hand.MAIN) return@addListener

        if (player.isSneaking && player.hasLuckPermission("lobby.pickupplayer")) {
            player.addPassenger(target)
            if (target.vehicle != null && target.vehicle !is Player) {
                target.vehicle?.remove()
            }
        }
    }
    eventNode.addListener(PlayerStopSneakingEvent::class.java) { e ->
        val player = e.player

        if (player.passengers.isEmpty()) return@addListener
        player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_TAKEOFF, Sound.Source.MASTER, 1f, 1.1f))

        player.passengers.forEach {
            player.removePassenger(it)
            it.velocity = player.position.direction().mul(40.0)
        }
    }
}