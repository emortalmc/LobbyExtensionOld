package dev.emortal.lobby.mount

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag
import kotlin.math.PI
import kotlin.reflect.full.primaryConstructor

sealed class Mount(val title: Component) {

    companion object {
        val registeredMap: Map<String, Mount>
            get() = Mount::class.sealedSubclasses.mapNotNull { it.objectInstance }.associateBy { it.javaClass.simpleName }
        val mountTag = Tag.Byte("mount")
    }

    val entities = mutableListOf<Entity>()

    abstract fun spawn(instance: Instance, player: Player)

    fun destroy() {
        entities.forEach {
            it.remove()
        }
        destroyed()
    }

    open fun tick(player: Player) {
        entities.forEach {
            it.setView(player.position.yaw, player.position.pitch)
        }
    }

    open fun move(player: Player, forward: Float, sideways: Float) {
        entities.forEach { entity ->
            if (forward == 0f && sideways == 0f) {
                entity.velocity = entity.velocity.mul(MinecraftServer.TICK_PER_SECOND * 0.05)
                return
            }

            val playerDir = player.position.direction()
            //dolphin.setView(player.position.yaw, player.position.pitch)

            entity.velocity = playerDir.mul(forward.toDouble())
                .add(playerDir.withY(0.0).rotateAroundY(90 * PI /180).mul(sideways.toDouble()))
                .mul(MinecraftServer.TICK_PER_SECOND * 0.8)
        }

    }

    open fun destroyed() {

    }

    open fun leftClick() {

    }

}