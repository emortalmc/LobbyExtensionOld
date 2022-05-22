package dev.emortal.lobby.games

import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.event.PlayerDismountEvent
import dev.emortal.immortal.game.Game
import dev.emortal.immortal.game.GameManager.doNotTeleportTag
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.npc.MultilineHologram
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.LobbyExtension.Companion.npcs
import dev.emortal.lobby.commands.MountCommand.mountMap
import dev.emortal.lobby.util.showFireworkWithDuration
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.entity.metadata.other.FallingBlockMeta
import net.minestom.server.event.entity.EntityPotionAddEvent
import net.minestom.server.event.entity.EntityPotionRemoveEvent
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.adventure.noItalic
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.asPos
import world.cepi.kstom.util.playSound
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.extra.Dust
import world.cepi.particle.renderer.Renderer
import world.cepi.particle.showParticle
import java.awt.Color
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class LobbyGame(gameOptions: GameOptions) : Game(gameOptions) {

    companion object {
        val spawnPoint = Pos(0.5, 65.0, -0.5, 180f, 0f)
    }

    val occupiedSeats: MutableSet<Point> = ConcurrentHashMap.newKeySet()
    val armourStandSeatMap = ConcurrentHashMap<Entity, Point>()

    override var spawnPosition = spawnPoint

    val holograms = ConcurrentHashMap<String, MultilineHologram>()

    override fun gameStarted() {
        npcs.values.forEach {
            val hologram = MultilineHologram(it.hologramLines.toMutableList())
            holograms[it.gameName] = hologram
            hologram.setInstance(it.position.add(0.0, (it.entityType.height() + 0.2) / 2.0, 0.0), instance)
            hologram.setLine(it.hologramLines.size - 1, Component.text("${LobbyExtension.playerCountCache[it.gameName] ?: 0} online", NamedTextColor.GRAY))
        }

        val connectHologram = MultilineHologram(mutableListOf("<gradient:red:gold><bold>Connect 4".asMini()))
        connectHologram.setInstance(Pos(18.0, 66.5, -2.0), instance)

        object : MinestomRunnable(coroutineScope = coroutineScope, repeat = Duration.ofMillis(100)) {
            override suspend fun run() {

                instance.showParticle(
                    world.cepi.particle.Particle.particle(
                        type = ParticleType.DUST,
                        data = OffsetAndSpeed(),
                        extraData = Dust(1f, 0f, 0f, 0.75f),
                        count = 1
                    ),
                    Renderer.fixedRectangle(Vec(18.5, 66.5, -12.5), Vec(31.5, 66.5, 8.5), step = 1.0)
                )

            }
        }


        // Connect 4 animation
        val redTurnTag = Tag.Boolean("redTurn")
        val gridX = 7
        val gridY = 6

        val xOffset = 29
        val yOffset = 65
        val zOffset = -9

        fun resetBoard() {
            val batch = AbsoluteBlockBatch()
            var white = true
            for (x in 0 until gridX) {
                for (y in 0 until gridY) {
                    for (x2 in 0..1) {
                        for (y2 in 0..1) {
                            batch.setBlock(xOffset, ((y*2) + y2) + yOffset, (x*2) + x2 + zOffset, if (white) Block.GRAY_CONCRETE else Block.GRAY_WOOL)
                            batch.setBlock(xOffset - 1, ((y*2) + y2) + yOffset, (x*2) + x2 + zOffset, Block.AIR)
                        }
                    }

                    white = !white
                }
                white = !white
            }

            batch.apply(instance, null)
        }

        fun setGreen() {
            val batch = AbsoluteBlockBatch()
            for (z in 0..1) {
                for (y in 0..7) {
                    batch.setBlock(xOffset - 1, y + yOffset, z + zOffset + (4*2), Block.LIME_CONCRETE)
                }
            }
            batch.apply(instance, null)
        }
        fun spawn4(gridX: Int, red: Boolean) {
            for (x in 0..1) {
                for (y in 1..2) {
                    val entity = Entity(EntityType.FALLING_BLOCK)
                    val meta = entity.entityMeta as FallingBlockMeta
                    meta.block = if (red) Block.RED_CONCRETE_POWDER else Block.YELLOW_CONCRETE_POWDER
                    entity.setInstance(instance, Pos(xOffset - 0.5, y + (gridY * 2) + yOffset + 0.5, x + (gridX * 2) + 0.5 + zOffset))
                    entity.setTag(redTurnTag, red)

                }
            }
        }

        object : MinestomRunnable(coroutineScope = coroutineScope, repeat = Duration.ofMillis(50)) {
            var redTurn = false

            override suspend fun run() {

                val currentIter = currentIteration.get()

                if (currentIter < 6*30 + 20 && currentIter % 30 == 0) {
                    spawn4(if (redTurn) 2 else 4, redTurn)
                    redTurn = !redTurn
                }

                if (currentIter == 6*30 + 19) {
                    setGreen()
                }
                if (currentIter == 6*30 + 60) {
                    resetBoard()
                    currentIteration.set(-1)
                    redTurn = false
                }
            }
        }

        eventNode.listenOnly<EntityTickEvent> {
            if (entity.entityType == EntityType.FALLING_BLOCK && gameState == GameState.ENDING) {
                entity.remove()
                return@listenOnly
            }

            if (entity.entityType == EntityType.FALLING_BLOCK && entity.isOnGround) {
                instance.setBlock(entity.position, if (entity.getTag(redTurnTag)) Block.RED_CONCRETE_POWDER else Block.YELLOW_CONCRETE_POWDER)
                entity.remove()
            }
        }

    }

    override fun gameDestroyed() {
    }

    override fun playerJoin(player: Player) {
        npcs.values.forEach {
            it.addViewer(player)
        }

        val compassItemStack = ItemStack.builder(Material.COMPASS)
            .displayName(Component.text("Game Selector", NamedTextColor.GOLD).noItalic())
            .build()

        player.inventory.setItemStack(4, compassItemStack)

        player.isAllowFlying = player.hasLuckPermission("lobby.fly")

        if (player.hasLuckPermission("lobby.fireworks")) {
            val fireworkItemstack = ItemStack.builder(Material.FIREWORK_ROCKET)
                .displayName(Component.text("Launch a firework", NamedTextColor.LIGHT_PURPLE).noItalic())
                .build()

            player.inventory.setItemStack(8, fireworkItemstack)
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

        eventNode.listenOnly<PlayerEntityInteractEvent> {
            if (!player.itemInMainHand.isAir) return@listenOnly
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

        eventNode.listenOnly<EntityTickEvent> {
            if (entity.entityType == EntityType.DOLPHIN) {
                val pass = entity.passengers.firstOrNull()?.position
                if (pass != null) this.entity.setView(pass.yaw, pass.pitch)
            }
        }

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (itemStack.material() == Material.COMPASS) {
                player.openInventory(LobbyExtension.gameSelectorGUI.inventory)
                player.playSound(Sound.sound(SoundEvent.UI_TOAST_IN, Sound.Source.MASTER, 1f, 1f))
            }

            if (itemStack.material() == Material.PHANTOM_MEMBRANE) {
                val rand = ThreadLocalRandom.current()
                instance.playSound(Sound.sound(Key.key("item.trumpet.doot"), Sound.Source.MASTER, 1f, rand.nextFloat(0.8f, 1.2f)), player.position)
                instance.getNearbyEntities(player.position, 8.0).filter { it.entityType == EntityType.PLAYER && it != player }.forEach {
                    it.velocity = it.position.sub(player.position).asVec().normalize().mul(60.0).withY { 17.0 }
                }
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
            if (newPosition.y < 55) {
                player.teleport(spawnPosition)
                return@listenOnly
            }

            /*if (newPosition.y < 62) {
                // BACKROOMS
                player.leaveGame()
                val out = ByteBufOutputStream(ByteBufAllocator.DEFAULT.buffer())
                out.writeUTF("Connect")
                out.writeUTF("backrooms")
                val buffer = out.buffer()
                val bytes = ByteArray(buffer.readableBytes())
                buffer.duplicate().readBytes(bytes)
                out.flush()
                player.sendPacket(PluginMessagePacket("BungeeCord", bytes))
            }*/

            val blockUnder = instance.getBlock(newPosition.sub(0.0, 1.0, 0.0))

            if (blockUnder.compare(Block.SLIME_BLOCK)) {
                player.addEffect(Potion(PotionEffect.JUMP_BOOST, 10, 10, 0))
            }

            if (newPosition.x > 19 && newPosition.x < 31 && newPosition.y < 70 && newPosition.y >= 65.0 && newPosition.z < 9 && newPosition.z > -13) {
                player.setTag(doNotTeleportTag, true)
                player.joinGameOrNew("connect4")
            }
        }

        //eventNode.listenOnly<EntityTickEvent> {

        //}

        eventNode.listenOnly<PlayerPacketEvent> {
            if (packet is ClientSteerVehiclePacket) {
                val steerPacket = packet as ClientSteerVehiclePacket
                if (steerPacket.flags.toInt() == 2) {
                    if (player.vehicle != null && player.vehicle !is Player) {
                        val entity = player.vehicle!!
                        entity.removePassenger(player)

                        mountMap[player]?.destroy()
                        mountMap.remove(player)

                        if (armourStandSeatMap.containsKey(entity)) {
                            occupiedSeats.remove(armourStandSeatMap[entity])
                            armourStandSeatMap.remove(entity)
                            entity.remove()
                            player.velocity = Vec(0.0, 10.0, 0.0)
                        }
                    }
                    return@listenOnly
                }

                val mount = mountMap[player] ?: return@listenOnly
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
                if (occupiedSeats.contains(blockPosition)) return@listenOnly
                if (block.getProperty("half") == "top") return@listenOnly

                occupiedSeats.add(blockPosition)

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

                armourStandSeatMap[armourStand] = blockPosition
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
        val newInstance = Manager.instance.createInstanceContainer()
        newInstance.chunkLoader = AnvilLoader("lobby")
        newInstance.timeRate = 0
        newInstance.timeUpdate = null

        return newInstance
    }

    // Lobby is not winnable
    override fun victory(winningPlayers: Collection<Player>) {
    }

}