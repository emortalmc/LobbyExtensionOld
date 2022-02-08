package dev.emortal.lobby.games

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.event.PlayerJoinGameEvent
import dev.emortal.immortal.event.PlayerLeaveGameEvent
import dev.emortal.immortal.game.Game
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameOptions
import dev.emortal.immortal.util.MultilineHologram
import dev.emortal.immortal.util.PacketNPC
import dev.emortal.immortal.util.PermissionUtils.hasLuckPermission
import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.LobbyExtension.Companion.npcs
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.adventure.audience.Audiences.players
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.entity.EntityPotionAddEvent
import net.minestom.server.event.entity.EntityPotionRemoveEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.item.item
import java.time.Duration

class LobbyGame(gameOptions: GameOptions) : Game(gameOptions) {
    // TODO: Bossbar
    // TODO: Game selector + cool npc thing

    companion object {
        val spawnPoint = Pos(0.5, 65.0, 0.5, 180f, 0f)

    }

    override var spawnPosition = spawnPoint

    val holograms = mutableMapOf<String, MultilineHologram>()

    override fun gameStarted() {
        npcs.values.forEach {
            val hologram = MultilineHologram(it.hologramLines.toMutableList())
            holograms[it.gameName] = hologram
            hologram.setInstance(it.position.add(0.0, 1.0, 0.0), instance)
            val playerCount = GameManager.gameMap[it.gameName]?.sumOf { it.players.size } ?: 0
            hologram.setLine(hologram.components.size - 1, Component.text("$playerCount online", NamedTextColor.GRAY))
        }
    }

    override fun gameDestroyed() {
    }

    override fun playerJoin(player: Player) {
        val compassItemStack = item(Material.COMPASS) {
            displayName(
                Component.text("Game Selector", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            )
        }

        npcs.values.forEach {
            it.addViewer(player)
        }

        player.inventory.setItemStack(4, compassItemStack)

        if (player.hasLuckPermission("lobby.fly")) player.isAllowFlying = true
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

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (itemStack.material == Material.COMPASS) {
                player.chat("/play")
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

        Manager.globalEvent.listenOnly<PlayerJoinGameEvent> {
            val gameName = getGame().gameTypeInfo.gameName
            val gameListing = ImmortalExtension.gameListingConfig.gameListings[gameName] ?: return@listenOnly
            if (!gameListing.itemVisible) return@listenOnly

            val hologram = holograms[gameName] ?: return@listenOnly
            val playerCount = GameManager.gameMap[gameName]?.sumOf { it.players.size } ?: 0

            hologram.setLine(hologram.components.size - 1, Component.text("$playerCount online", NamedTextColor.GRAY))
        }
        Manager.globalEvent.listenOnly<PlayerLeaveGameEvent> {
            val gameName = getGame().gameTypeInfo.gameName
            val gameListing = ImmortalExtension.gameListingConfig.gameListings[gameName] ?: return@listenOnly
            if (!gameListing.itemVisible) return@listenOnly

            val hologram = holograms[gameName] ?: return@listenOnly
            val playerCount = GameManager.gameMap[gameName]?.sumOf { it.players.size } ?: 0

            hologram.setLine(hologram.components.size - 1, Component.text("$playerCount online", NamedTextColor.GRAY))
        }

        eventNode.listenOnly<PlayerMoveEvent> {
            if (newPosition.y < 0) player.teleport(spawnPosition)

            if (instance.getBlock(newPosition.sub(0.0, 1.0, 0.0)).compare(Block.SLIME_BLOCK)) {
                player.addEffect(Potion(PotionEffect.JUMP_BOOST, 10, 10, 0))
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

    override fun instanceCreate(): Instance {
        //val newInstance = Manager.instance.createInstanceContainer()
        //newInstance.chunkLoader = AnvilLoader("lobby")
        //return newInstance
        return Manager.instance.createSharedInstance(LobbyExtension.lobbyInstance)
    }

    // Lobby is not winnable
    override fun victory(winningPlayers: Collection<Player>) {
    }

}