package dev.emortal.lobby.games

import dev.emortal.immortal.game.Game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.npc.MultilineHologram
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.RunnableGroup
import dev.emortal.immortal.util.cancel
import dev.emortal.immortal.util.showFireworkWithDuration
import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.LobbyExtension.Companion.npcs
import dev.emortal.lobby.mount.Mount
import dev.emortal.lobby.occurrences.Occurrence
import dev.emortal.nbstom.MusicPlayerInventory
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Enchantment
import net.minestom.server.item.ItemHideFlag
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
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.extra.Dust
import world.cepi.particle.extra.DustTransition
import world.cepi.particle.showParticle
import java.awt.Color
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LobbyExtensionGame : Game() {

    override val allowsSpectators = false
    override val countdownSeconds = 0
    override val maxPlayers = 50
    override val minPlayers = 0
    override val showScoreboard = false
    override val canJoinDuringGame = true
    override val showsJoinLeaveMessages = true


    val armourStandSeatList = CopyOnWriteArraySet<Point>()
    val mountMap = ConcurrentHashMap<UUID, Mount>()

    var currentOccurrence: Occurrence? = null
    var occurrenceStopTask: Task? = null

    val holograms = ConcurrentHashMap<String, MultilineHologram>()

    override fun getSpawnPosition(player: Player, spectator: Boolean): Pos = Pos(0.5, 65.0, 0.5, 180f, 0f)

    override fun gameCreated() {
        start()
        npcs.forEach {
            val hologram = MultilineHologram(it.hologramLines.toMutableList())
            holograms[it.gameName] = hologram
            val spawnPosition = it.position.add(0.0, (it.entityType.height() + 0.2) / 2.0, 0.0)
            instance!!.loadChunk(spawnPosition).thenRun {
                hologram.setInstance(spawnPosition, instance!!)

                val playerCountCache = LobbyExtension.playerCountCache[it.gameName]

                hologram.setLine(it.hologramLines.size - 1, Component.text("${playerCountCache ?: 0} online", NamedTextColor.GRAY))
            }

        }

    }

    override fun gameStarted() {

    }

    override fun gameEnded() {
        holograms.clear()
        armourStandSeatList.clear()
    }

    val playerGroupMap = ConcurrentHashMap<UUID, RunnableGroup>()

    override fun playerJoin(player: Player) {

        if (player.username == "emortaldev") {
            val group = RunnableGroup()
            playerGroupMap[player.uuid] = group

            object : MinestomRunnable(repeat = Duration.ofMillis(MinecraftServer.TICK_MS.toLong()), group = group) {
                var heightI = 0.0
                var spinI = 0.0
                var yRot = 0.0
                var lastPos = player.position

                override fun run() {
                    if (player.position.distanceSquared(lastPos ?: Pos.ZERO) > 0.2 * 0.2) {
                        lastPos = player.position
                        return
                    }
                    lastPos = player.position

                    yRot += 0.05

                    if (yRot > PI*2) yRot = 0.0

                    repeat(3) {
                        heightI += 0.15
                        spinI += 0.15
                        if (spinI > PI*2) spinI = 0.0
                        if (heightI > PI*2) heightI = 0.0

                        instance?.showParticle(
                            Particle.particle(
                                type = ParticleType.DUST_COLOR_TRANSITION,
                                data = OffsetAndSpeed(),
                                extraData = DustTransition(1f, 0.5f, 0f, 1f, 0f, 1f, 1.15f),
                                count = 1
                            ),
                            player.position.add(
                                Vec(
                                    cos(spinI) * 1.2,
                                    (sin(heightI) * 0.5),
                                    sin(spinI) * 1.2
                                ).rotateAroundY(yRot).add(0.0, 1.0, 0.0)
                            ).asVec()
                        )

                    }

                }
            }
        }

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

        player.inventory.setItemStack(
            8,
            ItemStack.builder(Material.MUSIC_DISC_BLOCKS)
                .displayName(Component.text("Music", NamedTextColor.GOLD).noItalic())
                .meta { meta ->
                    meta.enchantment(Enchantment.INFINITY, 1)
                    meta.hideFlag(ItemHideFlag.HIDE_ENCHANTS)

                }
                .build()
        )

        if (player.hasLuckPermission("lobby.fireworks")) {
            val fireworkItemstack = ItemStack.builder(Material.FIREWORK_ROCKET)
                .displayName(Component.text("Launch a firework", NamedTextColor.LIGHT_PURPLE).noItalic())
                .build()

            player.inventory.setItemStack(7, fireworkItemstack)
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
        playerGroupMap[player.uuid]?.cancelAll()
        playerGroupMap.remove(player.uuid)
        npcs.forEach {
            it.removeViewer(player)
        }
    }

    override fun registerEvents(eventNode: EventNode<InstanceEvent>) {
        eventNode.cancel<ItemDropEvent>()
        eventNode.cancel<InventoryPreClickEvent>()
        eventNode.cancel<PlayerSwapItemEvent>()

        eventNode.cancel<PlayerBlockBreakEvent>()
        eventNode.cancel<PlayerBlockPlaceEvent>()

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
                it.velocity = this.player.position.direction().mul(MinecraftServer.TICK_PER_SECOND.toDouble()).mul(1.5)
            }
        }

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (itemStack.material() == Material.COMPASS) {
                this.isCancelled = true
                player.openInventory(LobbyExtension.gameSelectorGUI.inventory)
                player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BIT, Sound.Source.MASTER, 1f, 1.5f))
                return@listenOnly
            }

            if (this.itemStack.material() == Material.MUSIC_DISC_BLOCKS) {
                this.isCancelled = true
                player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 2f))
                player.openInventory(MusicPlayerInventory.inventory)
                return@listenOnly
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
                if (!instance.getBlock(blockPosition.add(0.0, 1.0, 0.0)).compare(Block.AIR)) return@listenOnly

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

        hologram.setLine(hologram.components.size - 1, Component.text("$players online", NamedTextColor.GRAY))
    }

    // heh
    override fun victory(winningPlayers: Collection<Player>) {
    }

    override fun instanceCreate(): CompletableFuture<Instance> {
        val instanceFuture = CompletableFuture<Instance>()

        val newInstance = Manager.instance.createSharedInstance(LobbyExtension.lobbyInstance)
        newInstance.timeRate = 0
        newInstance.timeUpdate = null
        newInstance.setTag(Tag.Boolean("doNotAutoUnloadChunk"), true)

        newInstance.enableAutoChunkLoad(false)

        // 1 chunk required for player to spawn
        newInstance.loadChunk(0, 0).thenRun { instanceFuture.complete(newInstance) }

        val radius = 8
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                newInstance.loadChunk(x, z).thenAccept { it.sendChunk() }
            }
        }

        return instanceFuture
    }

}