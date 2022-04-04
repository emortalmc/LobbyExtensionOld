package dev.emortal.lobby.games

import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.Game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.npc.MultilineHologram
import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.LobbyExtension.Companion.npcs
import dev.emortal.lobby.commands.MountCommand.mountMap
import dev.emortal.lobby.util.showFireworkWithDuration
import dev.emortal.rayfast.casting.grid.GridCast
import dev.emortal.rayfast.vector.Vector3d
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityPotionAddEvent
import net.minestom.server.event.entity.EntityPotionRemoveEvent
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.event.player.PlayerStartSneakingEvent
import net.minestom.server.event.player.PlayerStopSneakingEvent
import net.minestom.server.event.player.PlayerSwapItemEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.noItalic
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.item.item
import world.cepi.kstom.util.asPos
import java.awt.Color
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

class LobbyGame(gameOptions: GameOptions) : Game(gameOptions) {

    companion object {
        val spawnPoint = Pos(0.5, 65.0, 0.5, 180f, 0f)

    }

    override var spawnPosition = spawnPoint

    val holograms = mutableMapOf<String, MultilineHologram>()

    override fun gameStarted() {
        npcs.values.forEach {
            val hologram = MultilineHologram(it.hologramLines.toMutableList())
            holograms[it.gameName] = hologram
            hologram.setInstance(it.position.add(0.0, (it.entityType.height() + 0.2) / 2.0, 0.0), instance)
            hologram.setLine(it.hologramLines.size - 1, Component.text("${LobbyExtension.playerCountCache[it.gameName] ?: 0} online", NamedTextColor.GRAY))
        }
    }

    override fun gameDestroyed() {
    }

    override fun playerJoin(player: Player) {
        npcs.values.forEach {
            it.addViewer(player)
        }

        val compassItemStack = item(Material.COMPASS) {
            displayName(
                Component.text("Game Selector", NamedTextColor.GOLD).noItalic()
            )
        }

        player.inventory.setItemStack(4, compassItemStack)

        player.isAllowFlying = player.hasLuckPermission("lobby.fly")

        if (player.hasLuckPermission("lobby.fireworks")) {
            val fireworkItemstack = item(Material.FIREWORK_ROCKET) {
                displayName(
                    Component.text("Launch a firework", NamedTextColor.LIGHT_PURPLE).noItalic()
                )
            }
            player.inventory.setItemStack(8, fireworkItemstack)
        }
    }

    override fun playerLeave(player: Player) {
        npcs.values.forEach {
            it.removeViewer(player)
        }
    }

    override fun registerEvents() {

        eventNode.listenOnly<ItemDropEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<InventoryPreClickEvent> {
            isCancelled = true
        }
        eventNode.listenOnly<PlayerSwapItemEvent> {
            isCancelled = true
        }

        @Suppress("INACCESSIBLE_TYPE")
        eventNode.listenOnly<PlayerUseItemOnBlockEvent> {


            if (itemStack.material == Material.FIREWORK_ROCKET) {
                var hitPos: Pos? = null
                val pos = player.position
                val dir = player.position.direction()
                val gridIterator: Iterator<Vector3d> = GridCast.createExactGridIterator(
                    pos.x(), pos.y() + 1.5, pos.z(),
                    dir.x(), dir.y(), dir.z(),
                    1.0, 4.0
                )

                while (gridIterator.hasNext()) {
                    val gridUnit = gridIterator.next()
                    val pos = Pos(gridUnit[0], gridUnit[1], gridUnit[2])

                    try {
                        val hitBlock = instance.getBlock(pos)

                        if (hitBlock.isSolid) {
                            hitPos = pos
                            break
                        }
                    } catch (e: NullPointerException) {
                        // catch if chunk is not loaded
                        break
                    }
                }

                val random = ThreadLocalRandom.current()
                val effects = mutableListOf(
                    FireworkEffect(
                        random.nextBoolean(),
                        random.nextBoolean(),
                        FireworkEffectType.values().random(),
                        listOf(net.minestom.server.color.Color(Color.HSBtoRGB(random.nextFloat(), 1f, 1f))),
                        listOf(net.minestom.server.color.Color(Color.HSBtoRGB(random.nextFloat(), 1f, 1f)))
                    )
                )
                players.showFireworkWithDuration(instance, hitPos?.add(0.0, 0.5, 0.0) ?: position.asPos().add(0.5, 0.5, 0.5), 20 + random.nextInt(0, 11), effects)
            }
        }

        eventNode.listenOnly<PlayerEntityInteractEvent> {
            val interactedPlayer = target as? Player ?: return@listenOnly
            if (player.hasLuckPermission("lobby.pickupplayer")) {
                player.addPassenger(interactedPlayer)
            }
        }
        eventNode.listenOnly<PlayerStopSneakingEvent> {
            player.passengers.forEach {
                player.removePassenger(it)
                it.velocity = this.player.position.direction().mul(30.0)
            }
        }

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (itemStack.material == Material.COMPASS) {
                player.openInventory(LobbyExtension.gameSelectorGUI.inventory)
                player.playSound(Sound.sound(SoundEvent.UI_TOAST_IN, Sound.Source.MASTER, 1f, 1f))
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
            if (newPosition.y < 55) player.teleport(spawnPosition)

            val blockUnder = instance.getBlock(newPosition.sub(0.0, 1.0, 0.0))

            if (blockUnder.compare(Block.SLIME_BLOCK)) {
                player.addEffect(Potion(PotionEffect.JUMP_BOOST, 10, 10, 0))
            }
        }

        //eventNode.listenOnly<EntityTickEvent> {

        //}

        eventNode.listenOnly<PlayerPacketEvent> {
            if (packet is ClientSteerVehiclePacket) {
                val steerPacket = packet as ClientSteerVehiclePacket
                if (steerPacket.flags.toInt() == 2) {
                    if (player.vehicle != null || player.vehicle !is Player) {
                        val entity = player.vehicle!!
                        entity.removePassenger(player)

                        mountMap[player]?.destroy()
                        mountMap.remove(player)

                        if (LobbyExtension.armourStandSeatMap.contains(entity)) {
                            LobbyExtension.occupiedSeats.remove(LobbyExtension.armourStandSeatMap[entity])
                            LobbyExtension.armourStandSeatMap.remove(entity)
                            entity.remove()
                            player.velocity = Vec(0.0, 10.0, 0.0)
                        }
                    }
                    return@listenOnly
                }

                val mount = mountMap[player] ?: return@listenOnly
                mount.move(player, steerPacket.forward, steerPacket.sideways)

            }
        }

        eventNode.listenOnly<PlayerBlockInteractEvent> {

            if (block.name().contains("stair", true)) {
                if (player.vehicle != null) return@listenOnly
                if (LobbyExtension.occupiedSeats.contains(blockPosition)) return@listenOnly
                if (block.getProperty("half") == "top") return@listenOnly

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
                    .thenRun {
                        armourStand.addPassenger(player)
                    }

                LobbyExtension.armourStandSeatMap[armourStand] = blockPosition
            }
        }
    }

    fun refreshHolo(gameName: String, players: Int) {
        val gameListing = LobbyExtension.gameListingConfig.gameListings[gameName] ?: return
        if (!gameListing.itemVisible) return

        val hologram = holograms[gameName] ?: return

        hologram.setLine(hologram.components.size - 1, Component.text("$players online", NamedTextColor.GRAY))
    }

    override fun instanceCreate(): Instance {
        val newInstance = Manager.instance.createSharedInstance(LobbyExtension.lobbyInstance)
        newInstance.timeRate = 0
        newInstance.timeUpdate = null
        newInstance.setBlock(0, 69, -33, Block.AIR)
        return newInstance
    }

    // Lobby is not winnable
    override fun victory(winningPlayers: Collection<Player>) {
    }

}