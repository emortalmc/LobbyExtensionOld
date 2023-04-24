package dev.emortal.lobby.games

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.animal.tameable.CatMeta
import java.util.concurrent.ThreadLocalRandom

class AprilFoolsEntity : Entity(EntityType.CAT) {

    init {
        hasPhysics = false
        setNoGravity(true)


    }

}