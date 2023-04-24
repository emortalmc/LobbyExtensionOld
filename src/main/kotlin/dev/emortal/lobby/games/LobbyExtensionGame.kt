package dev.emortal.lobby.games

import dev.emortal.immortal.game.Game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.npc.MultilineHologram
import dev.emortal.immortal.util.noItalic
import dev.emortal.immortal.util.playSound
import dev.emortal.immortal.util.showFireworkWithDuration
import dev.emortal.lobby.LobbyExtensionMain
import dev.emortal.lobby.LobbyExtensionMain.Companion.npcs
import dev.emortal.lobby.mount.Mount
import dev.emortal.lobby.occurrences.Occurrence
import dev.emortal.nbstom.commands.MusicPlayerInventory
import dev.emortal.tnt.TNTLoader
import dev.emortal.tnt.source.FileTNTSource
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
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.animal.tameable.CatMeta
import net.minestom.server.entity.metadata.animal.tameable.TameableAnimalMeta
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.batch.BatchOption
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.Direction
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.extra.DustTransition
import world.cepi.particle.showParticle
import java.awt.Color
import java.nio.file.Path
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


    private val gsonSerializer = GsonComponentSerializer.gson()

    private val lightsOutGrid = Array(5) { BooleanArray(5) { true } }
    private val lightsOutX = 5
    private val lightsOutY = 64
    private val lightsOutZ = -12

    val armourStandSeatList = CopyOnWriteArraySet<Point>()
    val mountMap = ConcurrentHashMap<UUID, Mount>()

    private val playerLaunchTasks = ConcurrentHashMap<UUID, Task>()
    private val playerLaunchPowers = ConcurrentHashMap<UUID, Int>()

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

                val playerCountCache = LobbyExtensionMain.playerCountCache[it.gameName]

                hologram.setLine(it.hologramLines.size - 1, Component.text("${playerCountCache ?: 0} online", NamedTextColor.GRAY))
            }

        }

        val signPos = Pos(9.0, 66.0, -15.0)
        instance!!.loadChunk(signPos).thenAccept {
            it.setBlock(signPos.add(-1.0, 0.0, 0.0), Block.AIR)

            val clickedNum = LobbyExtensionMain.buttonPresses.get()
            val componentText1 = Component.text("This sign has", NamedTextColor.BLACK)
            val componentText4 = Component.text()
                .append(Component.text(clickedNum, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" times", TextColor.color(212, 11, 212)))
                .build()

            val newBlock = instance!!.getBlock(signPos)
                .withTag(Tag.String("Text1"), gsonSerializer.serialize(componentText1))
                .withTag(Tag.String("Text4"), gsonSerializer.serialize(componentText4))
            it.setBlock(signPos, newBlock)

            val batch = AbsoluteBlockBatch(BatchOption().setSendUpdate(false)) // update is sent later to fix button anyway

            for (x in 5..9) {
                for (y in -12..-8) {
                    batch.setBlock(x, 64, y, Block.REDSTONE_LAMP)
                }
            }

            repeat(20) {
                val rand = ThreadLocalRandom.current()
                lightsOutClick(batch, rand.nextInt(0, 5), rand.nextInt(0, 5))
            }

            batch.apply(instance!!) {
                it.sendChunk()
            }

            instance!!.setBlock(7, 65, -6, Block.BIRCH_BUTTON.withProperty("face", "floor").withProperty("facing", "north"))
        }

    }

    override fun gameStarted() {

    }

    override fun gameEnded() {
        holograms.clear()
        armourStandSeatList.clear()
    }


    val aprilFoolsMob: MutableMap<UUID, Entity> = ConcurrentHashMap<UUID, Entity>();
    override fun playerJoin(player: Player) {

        player.isInvisible = true
        player.isAutoViewable = false
        val aprilFoolEntity = AprilFoolsEntity()

        val meta = aprilFoolEntity.entityMeta as CatMeta
        meta.color = CatMeta.Color.values().random()
        meta.isTamed = true
        meta.collarColor = ThreadLocalRandom.current().nextInt(15)
        println(CatMeta.Color.values().random().name)
        aprilFoolEntity.isCustomNameVisible = true
        meta.isCustomNameVisible = true

        val tameableMeta = aprilFoolEntity.entityMeta as TameableAnimalMeta
        tameableMeta.isCustomNameVisible = true


        aprilFoolEntity.scheduler().buildTask {
            tameableMeta.isSitting = player.isSneaking

            println("player yaw: " + player.position.yaw)
            aprilFoolEntity.teleport(player.position)
            aprilFoolEntity.setView(player.position.yaw, player.position.pitch)
        }.repeat(TaskSchedule.nextTick()).schedule()
        aprilFoolEntity.customName = Component.text(player.username)
        aprilFoolEntity.setInstance(player.instance, player.position)

        aprilFoolsMob[player.uuid] = aprilFoolEntity

        if (player.username == "emortaldev") {
            var heightI = 0.0
            var spinI = 0.0
            var yRot = 0.0
            var lastPos = player.position
            player.scheduler().buildTask {
                if (player.position.distanceSquared(lastPos) > 0.0) {
                    lastPos = player.position
                    return@buildTask
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
            }.repeat(TaskSchedule.nextTick()).schedule()
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

        player.isAllowFlying = player.hasLuckPermission("lobby.fly")

        player.inventory.setItemStack(
            8,
            ItemStack.builder(Material.JUKEBOX)
                .displayName(Component.text("Music Player", NamedTextColor.GOLD).noItalic())
                .build()
        )

        if (player.hasLuckPermission("lobby.fireworks")) {
            val fireworkItemstack = ItemStack.builder(Material.FIREWORK_ROCKET)
                .displayName(Component.text("Launch a firework", NamedTextColor.LIGHT_PURPLE).noItalic())
                .build()

            player.inventory.setItemStack(7, fireworkItemstack)
        }


        if (player.hasLuckPermission("lobby.doot")) {
            val trumpetItemstack = ItemStack.builder(Material.PHANTOM_MEMBRANE)
                .displayName(Component.text("Trumpet", NamedTextColor.YELLOW).noItalic())
                .meta {
                    it.customModelData(1)
                }
                .build()

            player.inventory.setItemStack(0, trumpetItemstack)
        }
    }

    override fun playerLeave(player: Player) {
        aprilFoolsMob[player.uuid]?.remove()
        npcs.forEach {
            it.removeViewer(player)
        }
    }

    override fun registerEvents(eventNode: EventNode<InstanceEvent>) {
        eventNode.addListener(InventoryPreClickEvent::class.java) { e ->
            e.isCancelled = true
        }
        eventNode.addListener(PlayerSwapItemEvent::class.java) { e ->
            e.isCancelled = true
        }

        eventNode.addListener(PlayerBlockBreakEvent::class.java) { e ->
            e.isCancelled = true
        }
        eventNode.addListener(PlayerBlockPlaceEvent::class.java) { e ->
            e.isCancelled = true
        }

        eventNode.addListener(PlayerEntityInteractEvent::class.java) { e ->
            val player = e.player
            val target = e.target as? Player ?: return@addListener

            if (!player.itemInMainHand.isAir) return@addListener
            if (e.hand != Player.Hand.MAIN) return@addListener

            if (player.isSneaking && player.hasLuckPermission("lobby.pickupplayer")) {
                player.addPassenger(target)
                if (target.vehicle != null && target.vehicle !is Player) {
                    target.vehicle?.remove()
                }

                playerLaunchTasks[player.uuid]?.cancel()
                playerLaunchTasks[player.uuid] = instance!!.scheduler().buildTask(object : Runnable {
                    var i = 1

                    override fun run() {
                        player.sendActionBar(Component.text("Launching with power: $i", NamedTextColor.YELLOW))
                        if (i < 40) {
                            i++
                            playerLaunchPowers[player.uuid] = i
                        }
                    }
                }).repeat(TaskSchedule.tick(2)).schedule()
            }
        }
        eventNode.addListener(PlayerStopSneakingEvent::class.java) { e ->
            val player = e.player

            playerLaunchTasks[player.uuid]?.cancel()
            playerLaunchTasks.remove(player.uuid)

            if (player.passengers.isEmpty()) return@addListener
            player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_TAKEOFF, Sound.Source.MASTER, 1f, 1.1f))


            player.passengers.forEach {
                player.removePassenger(it)
                it.velocity = player.position.direction().mul(playerLaunchPowers[player.uuid]!!.toDouble() * 2)
            }
            playerLaunchPowers.remove(player.uuid)
        }

        eventNode.addListener(PlayerUseItemEvent::class.java) { e ->
            e.isCancelled = true

            val player = e.player

            when (e.itemStack.material()) {
                Material.COMPASS -> {
                    player.openInventory(LobbyExtensionMain.gameSelectorGUI.inventory)
                    player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BIT, Sound.Source.MASTER, 1f, 1.5f))
                }

                Material.JUKEBOX -> {
                    player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 2f))
                    player.openInventory(MusicPlayerInventory.getInventory())
                }

                Material.PHANTOM_MEMBRANE -> {
                    val rand = ThreadLocalRandom.current()
                    e.instance.playSound(Sound.sound(Key.key("item.trumpet.doot"), Sound.Source.MASTER, 1f, rand.nextFloat(0.8f, 1.2f)), player.position)
                    e.instance.getNearbyEntities(player.position, 8.0)
                        .filter { it.entityType == EntityType.PLAYER && it != player }
                        .forEach {
                            it.velocity = it.position.sub(player.position)
                                .asVec()
                                .normalize()
                                .mul(60.0)
                                .withY { 17.0 }
                        }
                }

                else -> {}
            }
        }

        eventNode.addListener(ItemDropEvent::class.java) { e ->
            val item = e.itemStack
            val player = e.player

            if (item.material() != Material.SUNFLOWER) {
                e.isCancelled = true
                return@addListener
            }
            val itemEntity = ItemEntity(item)
            itemEntity.setPickupDelay(Duration.ofMillis(2000))
            val velocity = player.position.direction().mul(6.0)
            itemEntity.velocity = velocity
            itemEntity.scheduleRemove(Duration.ofMinutes(3))
            itemEntity.isCustomNameVisible = true
            itemEntity.customName = item.displayName
            itemEntity.setInstance(player.instance!!, player.position.add(0.0, 1.5, 0.0))
        }

//        eventNode.addListener<PickupItemEvent> {
//            this.entity
//            val player = entity as? Player ?: return@addListener
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

        eventNode.addListener(PlayerPacketEvent::class.java) { e ->
            val packet = e.packet
            val player = e.player

            if (packet is ClientSteerVehiclePacket) {
                val steerPacket = packet as ClientSteerVehiclePacket
                if (steerPacket.flags.toInt() == 2) {
                    if (player.vehicle != null && player.vehicle !is Player) {
                        val entity = player.vehicle!!
                        entity.removePassenger(player)

                    }
                    return@addListener
                }

                val mount = mountMap[player.uuid] ?: return@addListener
                mount.move(player, steerPacket.forward, steerPacket.sideways)

            }

            if (packet is ClientPlayerBlockPlacementPacket) {

                if (player.itemInMainHand.material() == Material.FIREWORK_ROCKET && packet.hand == Player.Hand.MAIN) {

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
                    players.showFireworkWithDuration(e.instance, Pos.fromPoint(packet.blockPosition.add(packet.cursorPositionX.toDouble(), packet.cursorPositionY.toDouble(), packet.cursorPositionZ.toDouble())), 20 + random.nextInt(0, 11), effects)
                }

            }
        }

        eventNode.addListener(PlayerBlockInteractEvent::class.java) { e ->
            val block = e.block
            val blockPos = e.blockPosition
            val player = e.player

            if (e.hand != Player.Hand.MAIN) return@addListener

            if (block.compare(Block.BIRCH_BUTTON)) {
                e.isCancelled = true

                val batch = AbsoluteBlockBatch(BatchOption().setSendUpdate(false)) // update is sent later to fix button anyway

                repeat(20) {
                    val rand = ThreadLocalRandom.current()
                    lightsOutClick(batch, rand.nextInt(0, 5), rand.nextInt(0, 5))
                }

                batch.apply(e.instance) {
                    e.instance.getChunkAt(blockPos)?.sendChunk()
                }
            }

            if (block.compare(Block.BIRCH_WALL_SIGN)) {
                if (blockPos.sameBlock(Pos(9.0, 66.0, -15.0))) {
                    player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.BLOCK, 0.75f, 2f), blockPos.add(0.5))

                    val clickedNum = LobbyExtensionMain.buttonPresses.incrementAndGet()
                    val component = Component.text()
                        .append(Component.text(clickedNum, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                        .append(Component.text(" times", TextColor.color(212, 11, 212)))
                        .build()
                    val newBlock = e.instance.getBlock(blockPos)
                        .withTag(Tag.String("Text4"), gsonSerializer.serialize(component))

                    e.instance.setBlock(blockPos, newBlock)
                }
            }

            if (block.compare(Block.REDSTONE_LAMP)) {
                val batch = AbsoluteBlockBatch()
                lightsOutClick(batch, blockPos.blockX() - lightsOutX, blockPos.blockZ() - lightsOutZ)
                batch.apply(e.instance) {}

                player.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self())
            }

            if (block.name().contains("stair", true)) {
                if (player.vehicle != null) return@addListener
                if (armourStandSeatList.contains(blockPos)) {
                    player.sendActionBar(Component.text("You can't sit on someone's lap", NamedTextColor.RED))
                    return@addListener
                }
                if (block.getProperty("half") == "top") return@addListener
                if (!e.instance.getBlock(blockPos.add(0.0, 1.0, 0.0)).compare(Block.AIR)) return@addListener

                val armourStand = SeatEntity {
                    armourStandSeatList.remove(blockPos)
                }

                val spawnPos = blockPos.add(0.5, 0.3, 0.5)
                val yaw = when (block.getProperty("facing")) {
                    "east" -> 90f
                    "south" -> 180f
                    "west" -> -90f
                    else -> 0f
                }

                armourStand.setInstance(e.instance, Pos(spawnPos, yaw, 0f))
                    .thenRun {
                        armourStand.addPassenger(player)
                    }

                armourStandSeatList.add(blockPos)
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

        val newInstance = MinecraftServer.getInstanceManager().createInstanceContainer()
        newInstance.timeRate = 0
        newInstance.time = 0
        newInstance.timeUpdate = null
        newInstance.chunkLoader = TNTLoader(FileTNTSource(Path.of("./lobby.tnt")))
        newInstance.setTag(Tag.Boolean("doNotAutoUnloadChunk"), true)

        newInstance.enableAutoChunkLoad(false)

        // 1 chunk required for player to spawn
        newInstance.loadChunk(0, 0).thenRun { instanceFuture.complete(newInstance) }

        val radius = 8
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                newInstance.loadChunk(x, z).thenAccept {
                    it.sendChunk()
                }
            }
        }

        return instanceFuture
    }

}