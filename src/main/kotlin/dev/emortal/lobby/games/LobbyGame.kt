package dev.emortal.lobby.games

import dev.emortal.immortal.game.Game
import dev.emortal.immortal.game.GameOptions
import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.inventories.GameSelectorInventory
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityPotionAddEvent
import net.minestom.server.event.entity.EntityPotionRemoveEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly

class LobbyGame(gameOptions: GameOptions) : Game(gameOptions) {
    // TODO: Bossbar
    // TODO: Game selector + cool npc thing

    override fun gameStarted() {
    }

    override fun gameDestroyed() {
    }

    override fun playerJoin(player: Player) {
        player.respawnPoint = LobbyExtension.SPAWN_POINT

        // Can cause random unexpected issues due to players joining
        // inside the PlayerLoginEvent
        player.scheduleNextTick {

            val compassItemStack = ItemStack.of(Material.COMPASS)

            player.inventory.setItemStack(0, compassItemStack)
        }
    }

    override fun playerLeave(player: Player) {
        LobbyExtension.playerMusicInvMap.remove(player)
    }

    override fun registerEvents() {
        eventNode.listenOnly<PlayerUseItemEvent> {
            if (itemStack.material == Material.COMPASS) {
                player.openInventory(GameSelectorInventory.inventory)
            }
        }

        eventNode.listenOnly<EntityPotionAddEvent> {
            if (potion.effect == PotionEffect.GLOWING) {
                entity.isGlowing = true
            }
        }
        eventNode.listenOnly<EntityPotionRemoveEvent> {
            if (potion.effect == PotionEffect.GLOWING) {
                entity.isGlowing = false
            }
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (player.position.y < -5) player.teleport(LobbyExtension.SPAWN_POINT)

            if (instance.getBlock(newPosition).compare(Block.RAIL)) {
                player.addEffect(Potion(PotionEffect.LEVITATION, 25, 3))
            }
            if (instance.getBlock(newPosition).compare(Block.CAVE_VINES_PLANT)) {
                player.addEffect(Potion(PotionEffect.GLOWING, 0, 3 * 20))
            }
            if (instance.getBlock(newPosition.sub(0.0, 1.0, 0.0)).compare(Block.SLIME_BLOCK)) {
                player.addEffect(Potion(PotionEffect.JUMP_BOOST, 10, 10))
            }
        }

        eventNode.listenOnly<PlayerPacketEvent> {
            if (packet is ClientSteerVehiclePacket) {
                if ((packet as ClientSteerVehiclePacket).flags.toInt() == 2) {
                    if (player.vehicle != null) {
                        val entity = player.vehicle!!
                        entity.removePassenger(player)

                        if (LobbyExtension.armourStandSeatMap.contains(entity)) {
                            LobbyExtension.occupiedSeats.remove(LobbyExtension.armourStandSeatMap[entity])
                            LobbyExtension.armourStandSeatMap.remove(entity)
                            entity.remove()
                            player.velocity = Vec(0.0, 10.0, 0.0)
                        }
                    }
                }
            }
        }

        eventNode.listenOnly<PlayerBlockInteractEvent> {
            if (block.name().contains("stair", true)) {
                if (player.vehicle != null) return@listenOnly
                if (LobbyExtension.occupiedSeats.contains(blockPosition)) return@listenOnly

                LobbyExtension.occupiedSeats.add(blockPosition)

                val armourStand = Entity(EntityType.ARMOR_STAND)
                val armourStandMeta = armourStand.entityMeta as ArmorStandMeta
                armourStandMeta.setNotifyAboutChanges(false)
                armourStandMeta.isSmall = true
                armourStandMeta.isHasNoBasePlate = true
                armourStandMeta.isMarker = true
                armourStandMeta.isInvisible = true
                armourStandMeta.setNotifyAboutChanges(true)
                armourStand.setNoGravity(true)

                val spawnPos = blockPosition.add(0.5, 0.3, 0.5)
                val yaw = when (block.getProperty("facing")) {
                    "east" -> 90f
                    "south" -> 180f
                    "west" -> -90f
                    else -> 0f
                }

                armourStand.setInstance(instance, Pos(spawnPos, yaw, 0f))
                armourStand.addPassenger(player)

                LobbyExtension.armourStandSeatMap[armourStand] = blockPosition
            }
        }
    }

    override fun instanceCreate(): Instance {
        val instance = Manager.instance.createInstanceContainer()
        instance.chunkLoader = AnvilLoader("lobby")
        return instance
    }
}