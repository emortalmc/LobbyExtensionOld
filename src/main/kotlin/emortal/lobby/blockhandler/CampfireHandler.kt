package emortal.lobby.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.utils.NamespaceID

object CampfireHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID = NamespaceID.from(Key.key("minecraft:campfire"))
    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        // I don't need any campfire tags :p
        return mutableListOf<Tag<*>>()
    }
}