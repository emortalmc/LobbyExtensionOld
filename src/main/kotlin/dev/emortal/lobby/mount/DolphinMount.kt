package dev.emortal.lobby.mount

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import kotlin.math.PI

class DolphinMount : Mount(Component.text("Dolphin", NamedTextColor.AQUA)) {

    override fun spawn(instance: Instance, player: Player) {
        val entity = LivingEntity(EntityType.DOLPHIN)
        entity.setNoGravity(true)
        entity.setInstance(instance, player.position).thenRun {
            entity.addPassenger(player)
        }
        entities.add(entity)
    }

    override fun move(player: Player, forward: Float, sideways: Float) {
        val dolphin = entities.first()
        if (forward == 0f && sideways == 0f) {
            dolphin.velocity = dolphin.velocity.mul(0.9)
            return
        }

        val playerDir = player.position.direction()


        dolphin.setView(player.position.yaw, player.position.pitch)


        dolphin.velocity = playerDir.mul(forward.toDouble())
            .add(playerDir.withY(0.0).rotateAroundY(90 * PI/180).mul(sideways.toDouble()))
            .mul(20.0)
    }

}