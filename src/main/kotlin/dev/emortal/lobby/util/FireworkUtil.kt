package dev.emortal.lobby.util

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.FireworkRocketMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.metadata.FireworkMeta
import net.minestom.server.network.packet.server.play.EntityStatusPacket
import net.minestom.server.utils.PacketUtils

object FireworkUtil {

    /**
     * Creates a firework explode effect
     *
     * e.g. `FireworkUtil.explode(instance, player.position, mutableListOf(...))`
     *
     * @param instance The instance to explode in
     * @param position Where to explode
     * @param players Players to show the effect to. If not specified it will use all players in the instance
     * @param effects List of FireworkEffect
     */
    fun explode(
        instance: Instance,
        position: Pos,
        effects: MutableList<FireworkEffect>,
        players: Collection<Player> = instance.players
    ) {
        val fireworkMeta = FireworkMeta.Builder().effects(effects).build()
        val fireworkItemStack = ItemStack.builder(Material.FIREWORK_ROCKET).meta(fireworkMeta).build()
        val firework = Entity(EntityType.FIREWORK_ROCKET)
        val meta = firework.entityMeta as FireworkRocketMeta

        meta.fireworkInfo = fireworkItemStack
        firework.setInstance(instance, position)

        val fireworkExplodePacket = EntityStatusPacket()
        fireworkExplodePacket.entityId = firework.entityId
        fireworkExplodePacket.status = 17

        PacketUtils.sendGroupedPacket(players, fireworkExplodePacket)

        firework.remove()
    }

}