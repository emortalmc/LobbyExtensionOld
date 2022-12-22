package dev.emortal.lobby.games

import dev.emortal.immortal.game.Game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.npc.MultilineHologram
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.RunnableGroup
import dev.emortal.immortal.util.cancel
import dev.emortal.immortal.util.showFireworkWithDuration
import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.LobbyExtension.Companion.lobbyBiome
import dev.emortal.lobby.LobbyExtension.Companion.npcs
import dev.emortal.lobby.mount.Mount
import dev.emortal.lobby.occurrences.Occurrence
import dev.emortal.nbstom.MusicPlayerInventory
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.batch.Batch
import net.minestom.server.instance.batch.BatchOption
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Enchantment
import net.minestom.server.item.ItemHideFlag
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket
import net.minestom.server.resourcepack.ResourcePack
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.Direction
import net.minestom.server.utils.NamespaceID
import net.minestom.server.utils.time.TimeUnit
import net.minestom.server.world.biomes.Biome
import net.minestom.server.world.biomes.BiomeEffects
import world.cepi.kstom.Manager
import world.cepi.kstom.Manager.biome
import world.cepi.kstom.Manager.block
import world.cepi.kstom.adventure.color
import world.cepi.kstom.adventure.noItalic
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.asPos
import world.cepi.kstom.util.playSound
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

    private val christmasBookNames = setOf(
        "The Polar Express",
        "The Velveteen Rabbit",
        "A Christmas Carol",
        "How the Grinch Stole Christmas!",
        "Letters from Father Christmas",
        "A Christmas Memory",
        "The Nutcracker",
        "Rudolph the Red-nosed Reindeer",
        "The Snowman",
        "Miracle on 34th Street",
        "Father Christmas",
        "The Snow Queen",
        "The Lion, the Witch and the Wardrobe",
        "The Night Before Christmas",
        "The Twelve Days of Christmas",
        "Dream Snow",
        "The Jolly Christmas Postman",
        "The Biggest Snowman Ever",
        "The Christmas Tale of Peter Rabbit",
        "Stick Man"
    )


    override val allowsSpectators = false
    override val countdownSeconds = 0
    override val maxPlayers = 50
    override val minPlayers = 0
    override val showScoreboard = false
    override val canJoinDuringGame = true
    override val showsJoinLeaveMessages = true


    private val gsonSerializer = GsonComponentSerializer.gson()

    private val lightsOutGrid = Array(5) { BooleanArray(5) { false } }
    private val lightsOutX = 5
    private val lightsOutY = 64
    private val lightsOutZ = -12

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

        val signPos = Pos(9.0, 66.0, -15.0)
        instance!!.loadChunk(signPos).thenAccept {
            it.setBlock(signPos.add(-1.0, 0.0, 0.0), Block.AIR)

            val clickedNum = LobbyExtension.buttonPresses.get()
            val newBlock = instance!!.getBlock(signPos)
                .withTag(Tag.String("Text1"), "{\"extra\":[{\"bold\":false,\"color\":\"black\",\"text\":\"This sign has\"}],\"text\":\"\"}")
                .withTag(Tag.String("Text4"), "{\"extra\":[{\"bold\":true,\"color\":\"light_purple\",\"text\":\"$clickedNum \"},{\"color\":\"light_purple\",\"text\":\"times\"}],\"text\":\"\"}")
            it.setBlock(signPos, newBlock)

            instance!!.setBlock(7, 65, -6, Block.BIRCH_BUTTON.withProperty("face", "floor").withProperty("facing", "north"))
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
                    if (player.position.distanceSquared(lastPos) > 0.2 * 0.2) {
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

//        if (player.hasLuckPermission("lobby.bobux")) {
//            val bobuxItemStack = ItemStack.builder(Material.SUNFLOWER)
//                .displayName(Component.text("Great British Pound", NamedTextColor.GOLD).noItalic())
//                .amount(64)
//                .build()
//
//            player.inventory.setItemStack(0, bobuxItemStack)
//        }

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

        eventNode.listenOnly<ItemDropEvent> {
            if (itemStack.material() != Material.SUNFLOWER) {
                isCancelled = true
                return@listenOnly
            }
            val itemEntity = ItemEntity(itemStack)
            itemEntity.setPickupDelay(Duration.ofMillis(2000))
            val velocity = player.position.direction().mul(6.0)
            itemEntity.velocity = velocity
            itemEntity.boundingBox = itemEntity.boundingBox.expand(0.5, 0.0, 0.5)
            itemEntity.scheduleRemove(Duration.ofMinutes(3))
            itemEntity.isCustomNameVisible = true
            itemEntity.customName = itemStack.displayName
            itemEntity.setInstance(player.instance!!, player.position.add(0.0, 1.5, 0.0))
        }

//        eventNode.listenOnly<PickupItemEvent> {
//            this.entity
//            val player = entity as? Player ?: return@listenOnly
//
//            val couldAdd = player.inventory.addItemStack(itemStack)
//            isCancelled = !couldAdd
//
//            if (couldAdd && itemStack.material() == Material.SUNFLOWER) {
//                val bobuxAmount = player.inventory.itemStacks.filter { it.material() == Material.SUNFLOWER }.sumOf { it.amount() }
//                player.sendActionBar(Component.text("You now have £$bobuxAmount", NamedTextColor.GREEN))
//                player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_XYLOPHONE, Sound.Source.MASTER, 1f, 2f), Sound.Emitter.self())
//            }
//        }

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
            if (hand != Player.Hand.MAIN) return@listenOnly

            if (block.compare(Block.BIRCH_BUTTON)) {
                isCancelled = true

                val batch = AbsoluteBlockBatch(BatchOption().setSendUpdate(false)) // update is sent later to fix button anyway

                repeat(20) {
                    val rand = ThreadLocalRandom.current()
                    lightsOutClick(batch, rand.nextInt(0, 5), rand.nextInt(0, 5))
                }

                batch.apply(instance) {
                    instance.getChunkAt(blockPosition)?.sendChunk()
                }
            }

            if (block.compare(Block.BIRCH_WALL_SIGN)) {
                if (blockPosition.sameBlock(Pos(9.0, 66.0, -15.0))) {
                    player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.BLOCK, 0.75f, 2f), blockPosition.add(0.5))

                    val clickedNum = LobbyExtension.buttonPresses.incrementAndGet()
                    val component = Component.text(clickedNum, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                        .append(Component.text(" times", TextColor.color(212, 11, 212)))
                    val newBlock = instance.getBlock(blockPosition)
                        .withTag(Tag.String("Text4"), gsonSerializer.serialize(component))

                    instance.setBlock(blockPosition, newBlock)
                }
            }

            if (block.compare(Block.REDSTONE_LAMP)) {
                val batch = AbsoluteBlockBatch()
                lightsOutClick(batch, blockPosition.blockX() - lightsOutX, blockPosition.blockZ() - lightsOutZ)
                batch.apply(instance) {}

                player.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self())
            }

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

    fun lightsOutClick(batch: AbsoluteBlockBatch, x: Int, y: Int) {
        Direction.HORIZONTAL.forEach {
            val newX = x + it.normalX()
            val newY = y + it.normalZ()
            if (newX !in 0 until 5) return@forEach
            if (newY !in 0 until 5) return@forEach

            val newValue = !lightsOutGrid[newX][newY]
            lightsOutGrid[newX][newY] = newValue

            batch.setBlock(lightsOutX + newX, 64, lightsOutZ + newY, Block.REDSTONE_LAMP.withProperty("lit", newValue.toString()))
        }

        lightsOutGrid[x][y] = !lightsOutGrid[x][y]
        batch.setBlock(lightsOutX + x, lightsOutY, lightsOutZ + y, Block.REDSTONE_LAMP.withProperty("lit", lightsOutGrid[x][y].toString()))
    }

    fun refreshHolo(gameName: String, players: Int) {
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
        newInstance.time = 18000
        newInstance.timeUpdate = null
        newInstance.setTag(Tag.Boolean("doNotAutoUnloadChunk"), true)

        newInstance.enableAutoChunkLoad(false)

        // 1 chunk required for player to spawn
        newInstance.loadChunk(0, 0).thenRun { instanceFuture.complete(newInstance) }

        val radius = 8
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                newInstance.loadChunk(x, z).thenAccept {
                    for (x in 0 until Chunk.CHUNK_SIZE_X) {
                        for (y in 60..80) {
                            for (z in 0 until Chunk.CHUNK_SIZE_Z) {
                                it.setBiome(x, y, z, lobbyBiome)
                                if (it.getBlock(x, y, z).compare(Block.BROWN_GLAZED_TERRACOTTA)) {
                                    it.setBlock(x, y, z, Block.AIR)
                                    println("Changed block")
                                }
                            }
                        }
                    }
                    it.sendChunk()
                }
            }
        }

        return instanceFuture
    }

}