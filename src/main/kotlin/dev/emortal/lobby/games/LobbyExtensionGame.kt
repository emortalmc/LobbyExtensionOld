package dev.emortal.lobby.games

import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.LobbyGame
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.npc.MultilineHologram
import dev.emortal.immortal.util.cancel
import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.LobbyExtension.Companion.npcs
import dev.emortal.lobby.commands.MountCommand.mountMap
import dev.emortal.lobby.occurrences.Occurrence
import dev.emortal.lobby.util.showFireworkWithDuration
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.noItalic
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.asPos
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadLocalRandom

class LobbyExtensionGame(gameOptions: GameOptions) : LobbyGame(gameOptions) {

    val armourStandSeatList = CopyOnWriteArraySet<Point>()

    override var spawnPosition = Pos(0.5, 65.0, 0.5, 180f, 0f)

    var currentOccurrence: Occurrence? = null
    var occurrenceStopTask: Task? = null

    val holograms = ConcurrentHashMap<String, MultilineHologram>()

    override fun gameStarted() {
        instance.get()?.enableAutoChunkLoad(false)

        val radius = 5
        val diameter = radius * 2
        val chunks = (diameter + 1) * (diameter + 1)
        val countDownLatch = CountDownLatch((diameter + 1) * (diameter + 1))
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                instance.get()?.loadChunk(x, z)?.thenRun {
                    countDownLatch.countDown()
                }
            }
        }

        npcs.forEach {
            val hologram = MultilineHologram(it.hologramLines.toMutableList())
            holograms[it.gameName] = hologram
            hologram.setInstance(it.position.add(0.0, (it.entityType.height() + 0.2) / 2.0, 0.0), instance.get()!!)

            val playerCountCache = LobbyExtension.playerCountCache[it.gameName]
            if (playerCountCache != -1) {
                hologram.setLine(it.hologramLines.size - 1, Component.text("${playerCountCache ?: 0} online", NamedTextColor.GRAY))
            } else {
                hologram.setLine(it.hologramLines.size - 1, Component.text("Game unavailable", TextColor.color(217, 54, 54)))
            }


        }


    }

    override fun gameDestroyed() {
        holograms.clear()
        armourStandSeatList.clear()
    }

    override fun playerJoin(player: Player) {
        npcs.forEach {
            it.addViewer(player)
        }

        val compassItemStack = ItemStack.builder(Material.COMPASS)
            .displayName(Component.text("Game Selector", NamedTextColor.GOLD).noItalic())
            .build()

        //player.inventory.helmet = ItemStack.of(Material.GOLDEN_HELMET)
        //player.sendMessage("o7")

        player.inventory.setItemStack(4, compassItemStack)

        //player.isAllowFlying = player.hasLuckPermission("lobby.fly")

        if (player.hasLuckPermission("lobby.fireworks")) {
            val fireworkItemstack = ItemStack.builder(Material.FIREWORK_ROCKET)
                .displayName(Component.text("Launch a firework", NamedTextColor.LIGHT_PURPLE).noItalic())
                .build()

            player.inventory.setItemStack(8, fireworkItemstack)
        }

//        if (player.hasLuckPermission("lobby.doot")) {
//            val trumpetItemstack = ItemStack.builder(Material.PHANTOM_MEMBRANE)
//                .displayName(Component.text("Trumpet", NamedTextColor.YELLOW).noItalic())
//                .meta {
//                    it.customModelData(1)
//                }
//                .build()
//
//            player.inventory.setItemStack(0, trumpetItemstack)
//        }
    }

    override fun playerLeave(player: Player) {
        npcs.forEach {
            it.removeViewer(player)
        }
    }

    override fun registerEvents() {

        eventNode.cancel<ItemDropEvent>()
        eventNode.cancel<InventoryPreClickEvent>()
        eventNode.cancel<PlayerSwapItemEvent>()

        eventNode.listenOnly<PlayerEntityInteractEvent> {
            if (!player.itemInMainHand.isAir) return@listenOnly
            val interactedPlayer = target as? Player ?: return@listenOnly
            if (player.hasLuckPermission("lobby.pickupplayer")) {
                player.addPassenger(interactedPlayer)
                if (interactedPlayer.vehicle != null && interactedPlayer.vehicle !is Player) {
                    interactedPlayer.vehicle?.remove()
                }
            }
        }
        eventNode.listenOnly<PlayerStopSneakingEvent> {
            player.passengers.forEach {
                player.removePassenger(it)
                it.velocity = this.player.position.direction().mul(30.0)
            }
        }

        eventNode.listenOnly<EntityTickEvent> {
            if (entity.entityType == EntityType.DOLPHIN) {
                val pass = entity.passengers.firstOrNull()?.position
                if (pass != null) this.entity.setView(pass.yaw, pass.pitch)
            }
        }

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (itemStack.material() == Material.COMPASS) {
                player.openInventory(LobbyExtension.gameSelectorGUI.inventory)
                player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BIT, Sound.Source.MASTER, 1f, 1.5f))
            }

            if (itemStack.material() == Material.PHANTOM_MEMBRANE) {
                val rand = ThreadLocalRandom.current()
                instance.playSound(Sound.sound(Key.key("item.trumpet.doot"), Sound.Source.MASTER, 1f, rand.nextFloat(0.8f, 1.2f)), player.position)
                instance.getNearbyEntities(player.position, 8.0).filter { it.entityType == EntityType.PLAYER && it != player }.forEach {
                    it.velocity = it.position.sub(player.position).asVec().normalize().mul(60.0).withY { 17.0 }
                }
            }
        }

        eventNode.listenOnly<PlayerPacketEvent> {
            if (packet is ClientSteerVehiclePacket) {
                val steerPacket = packet as ClientSteerVehiclePacket
                if (steerPacket.flags.toInt() == 2) {
                    if (player.vehicle != null && player.vehicle !is Player) {
                        val entity = player.vehicle!!
                        entity.removePassenger(player)

                        mountMap[player.uuid]?.destroy()
                        mountMap.remove(player.uuid)

                    }
                    return@listenOnly
                }

                val mount = mountMap[player.uuid] ?: return@listenOnly
                mount.move(player, steerPacket.forward, steerPacket.sideways)

            }

            if (packet is ClientPlayerBlockPlacementPacket) {
                val placePacket = packet as ClientPlayerBlockPlacementPacket

                if (player.itemInMainHand.material() == Material.FIREWORK_ROCKET && placePacket.hand == Player.Hand.MAIN) {

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
                    players.showFireworkWithDuration(instance, placePacket.blockPosition.add(placePacket.cursorPositionX.toDouble(), placePacket.cursorPositionY.toDouble(), placePacket.cursorPositionZ.toDouble()).asPos(), 20 + random.nextInt(0, 11), effects)
                }

            }
        }

        eventNode.listenOnly<PlayerBlockInteractEvent> {

            if (block.name().contains("stair", true)) {
                if (player.vehicle != null) return@listenOnly
                if (armourStandSeatList.contains(blockPosition)) {
                    player.sendActionBar(Component.text("You can't sit on someone's lap", NamedTextColor.RED))
                    return@listenOnly
                }
                if (block.getProperty("half") == "top") return@listenOnly

                val armourStand = SeatEntity {
                    armourStandSeatList.remove(blockPosition)
                }

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

                armourStandSeatList.add(blockPosition)
            }
        }
    }

    fun refreshHolo(gameName: String, players: Int) {
        val gameListing = LobbyExtension.gameListingConfig.gameListings[gameName] ?: return
        if (!gameListing.itemVisible) return

        val hologram = holograms[gameName] ?: return

        if (players == -1) {
            hologram.setLine(hologram.components.size - 1, Component.text("Game unavailable", TextColor.color(217, 54, 54)))
        } else {
            hologram.setLine(hologram.components.size - 1, Component.text("$players online", NamedTextColor.GRAY))
        }
    }

    override fun instanceCreate(): Instance {
        val newInstance = Manager.instance.createInstanceContainer()
        newInstance.chunkLoader = LobbyExtension.sharedLoader
        newInstance.timeRate = 0
        newInstance.timeUpdate = null
        newInstance.setTag(Tag.Boolean("doNotAutoUnloadChunk"), true)

        return newInstance
    }

}