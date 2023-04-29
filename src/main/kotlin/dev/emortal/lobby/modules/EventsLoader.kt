package dev.emortal.lobby.modules

import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.util.noItalic
import dev.emortal.immortal.util.showFirework
import dev.emortal.immortal.util.showFireworkWithDuration
import dev.emortal.lobby.LobbyMain
import dev.emortal.lobby.commands.SpawnCommand
import dev.emortal.nbstom.commands.MusicPlayerInventory
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.extra.DustTransition
import world.cepi.particle.showParticle
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin





private val imposters = mutableListOf("brayjamin")

private val GAME_SELECTOR_ITEM = ItemStack.builder(Material.COMPASS)
    .displayName(Component.text("Game Selector", NamedTextColor.GOLD).noItalic())
    .build()
private val JUKEBOX_ITEM = ItemStack.builder(Material.JUKEBOX)
    .displayName(Component.text("Music Player", NamedTextColor.GOLD).noItalic())
    .build()
private val FIREWORK_ITEM = ItemStack.builder(Material.FIREWORK_ROCKET)
    .displayName(Component.text("Launch a firework", NamedTextColor.LIGHT_PURPLE).noItalic())
    .build()
private val TRUMPET_ITEM = ItemStack.builder(Material.PHANTOM_MEMBRANE)
    .displayName(Component.text("Trumpet", NamedTextColor.YELLOW).noItalic())
    .meta {
        it.customModelData(1)
    }
    .build()

fun eventsLoader() {
    val eventNode = MinecraftServer.getGlobalEventHandler()

    eventNode.addListener(PlayerLoginEvent::class.java) { e ->
        e.setSpawningInstance(LobbyMain.instance)
        e.player.respawnPoint = SpawnCommand.SPAWN_POINT
        e.player.gameMode = GameMode.ADVENTURE

        LobbyMain.instance.sendMessage(
            Component.text()
                .append(Component.text("JOIN", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(e.player.username, NamedTextColor.GREEN))
                .append(Component.text(" joined the game ", NamedTextColor.GRAY))
        )
        LobbyMain.instance.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 1.2f))
    }

    eventNode.addListener(PlayerSpawnEvent::class.java) { e ->
        val player = e.player

        player.isAllowFlying = player.hasLuckPermission("lobby.fly")

        // Items
        player.inventory.setItemStack(4, GAME_SELECTOR_ITEM)
        player.inventory.setItemStack(8, JUKEBOX_ITEM)
        if (player.hasLuckPermission("lobby.fireworks")) player.inventory.setItemStack(7, FIREWORK_ITEM)
        if (player.hasLuckPermission("lobby.doot")) player.inventory.setItemStack(0, TRUMPET_ITEM)

        if (e.player.username == "emortaldev") activateEmortalMode(player, LobbyMain.instance)

        LobbyMain.npcs.forEach {
            it.addViewer(player)
        }

        if (e.isFirstSpawn) {

            if (imposters.contains(player.username)) {
                player.sendMessage("sus")
            }

            if (player.displayName != null) player.sendMessage(
                Component.text()
                    .append(Component.text("Welcome, ", NamedTextColor.GRAY))
                    .append(player.displayName!!)
                    .append(Component.text(", to ", NamedTextColor.GRAY))
                    .append(LobbyMain.emortalmcGradient)
            )

            player.scheduler().buildTask {

                player.showFirework(
                    player.instance!!,
                    player.position.add(0.0, 1.0, 0.0),
                    mutableListOf(
                        FireworkEffect(
                            false,
                            false,
                            FireworkEffectType.LARGE_BALL,
                            mutableListOf(Color(NamedTextColor.LIGHT_PURPLE)),
                            mutableListOf(Color(NamedTextColor.GOLD))
                        )
                    )
                )
            }.delay(TaskSchedule.millis(500)).schedule()
        }
    }

    eventNode.addListener(PlayerDisconnectEvent::class.java) { e ->
        LobbyMain.npcs.forEach {
            it.removeViewer(e.player)
        }

        LobbyMain.instance.sendMessage(
            Component.text()
                .append(Component.text("QUIT", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(e.player.username, NamedTextColor.RED))
                .append(Component.text(" left the game ", NamedTextColor.GRAY))
        )
        LobbyMain.instance.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 0.5f))

    }

    eventNode.addListener(PlayerMoveEvent::class.java) { e ->
        // Teleport before player dies from void
        if (e.player.position.y < 52) {
            e.player.teleport(e.player.respawnPoint)
        }
    }

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



    eventNode.addListener(PlayerUseItemEvent::class.java) { e ->
        e.isCancelled = true

        val player = e.player

        when (e.itemStack.material()) {
            Material.COMPASS -> {
                player.openInventory(LobbyMain.gameSelectorGUI.inventory)
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
                            .withY(17.0)
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

        if (packet is ClientPlayerBlockPlacementPacket) {

            if (player.itemInMainHand.material() == Material.FIREWORK_ROCKET && packet.hand == Player.Hand.MAIN) {

                val random = ThreadLocalRandom.current()
                val effects = mutableListOf(
                    FireworkEffect(
                        random.nextBoolean(),
                        random.nextBoolean(),
                        FireworkEffectType.values().random(),
                        listOf(Color(java.awt.Color.HSBtoRGB(random.nextFloat(), 1f, 1f))),
                        listOf(Color(java.awt.Color.HSBtoRGB(random.nextFloat(), 1f, 1f)))
                    )
                )
                e.instance.players.showFireworkWithDuration(e.instance, Pos.fromPoint(packet.blockPosition.add(packet.cursorPositionX.toDouble(), packet.cursorPositionY.toDouble(), packet.cursorPositionZ.toDouble())), 20 + random.nextInt(0, 11), effects)
            }

        }
    }
}

private fun activateEmortalMode(player: Player, instance: Instance) {
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

        if (yRot > PI *2) yRot = 0.0

        repeat(3) {
            heightI += 0.15
            spinI += 0.15
            if (spinI > PI *2) spinI = 0.0
            if (heightI > PI *2) heightI = 0.0

            instance.showParticle(
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