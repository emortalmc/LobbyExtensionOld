package dev.emortal.lobby.mount

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag
import kotlin.reflect.full.primaryConstructor

sealed class Mount(val title: Component) {

    companion object {
        val registeredMap
            get() = Mount::class.sealedSubclasses.mapNotNull { it.primaryConstructor }.associateBy { it.javaClass.simpleName }
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
        
    }

    abstract fun move(player: Player, forward: Float, sideways: Float)

    open fun destroyed() {

    }

    open fun leftClick() {

    }

}