package dev.emortal.lobby.mount

import dev.emortal.lobby.games.SeatEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance

sealed class NoclipMount : Mount(Component.text("Noclip", NamedTextColor.RED)) {

    override fun spawn(instance: Instance, player: Player) {
        val entity = SeatEntity(physics = false, entityType = EntityType.MINECART) {
            destroy()
        }
        entity.setInstance(instance, player.position).thenRun {
            entity.addPassenger(player)
        }
        entities.add(entity)
    }

    override fun tick(player: Player) {

    }

}