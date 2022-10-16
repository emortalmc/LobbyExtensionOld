package dev.emortal.lobby.games

import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.ArmorStandMeta

class SeatEntity(val onRemove: (SeatEntity) -> Unit) : Entity(EntityType.ARMOR_STAND) {

    init {
        val meta = entityMeta as ArmorStandMeta
        meta.setNotifyAboutChanges(false)
        setNoGravity(true)
        hasPhysics = false
        meta.isMarker = true
        meta.isInvisible = true
        meta.isHasNoBasePlate = true
        meta.isSmall = true
        meta.setNotifyAboutChanges(true)
    }

    override fun removePassenger(entity: Entity) {
        super.removePassenger(entity)

        entity.velocity = Vec(0.0, 10.0, 0.0)

        if (passengers.isEmpty()) {
            onRemove(this)
            remove()
        }
    }

}