package dev.emortal.lobby

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.ArmorStandMeta

class SeatEntity(physics: Boolean = false, entityType: EntityType = EntityType.ARMOR_STAND, val onRemove: (SeatEntity) -> Unit) : Entity(entityType) {

    init {
        if (entityType == EntityType.ARMOR_STAND) {
            val meta = entityMeta as ArmorStandMeta
            meta.setNotifyAboutChanges(false)
            meta.isMarker = true
            meta.isInvisible = true
            meta.isHasNoBasePlate = true
            meta.isSmall = true
            meta.setNotifyAboutChanges(true)
        }

        setNoGravity(true)
        hasPhysics = physics
    }

    override fun updateVelocity(wasOnGround: Boolean, flying: Boolean, positionBeforeMove: Pos?, newVelocity: Vec?) {
        if (newVelocity != null) {
            val newVelocityy = newVelocity // Convert from block/tick to block/sec
                .mul(MinecraftServer.TICK_PER_SECOND.toDouble()) // Prevent infinitely decreasing velocity
                .mul(0.95)
                .apply(Vec.Operator.EPSILON)

            velocity = newVelocityy
        }
    }

    override fun removePassenger(entity: Entity) {
        super.removePassenger(entity)

        entity.velocity = Vec(0.0, MinecraftServer.TICK_PER_SECOND * 0.5, 0.0)

        if (passengers.isEmpty()) {
            onRemove(this)
            remove()
        }
    }

}