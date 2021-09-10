package emortal.lobby

import emortal.immortal.game.GameManager
import emortal.immortal.game.GameManager.joinGameOrNew
import emortal.immortal.game.GameOptions
import emortal.immortal.game.GameTypeInfo
import emortal.immortal.util.VoidGenerator
import emortal.lobby.blockhandler.CampfireHandler
import emortal.lobby.blockhandler.SignHandler
import emortal.lobby.blockhandler.SkullHandler
import emortal.lobby.commands.DiscCommand
import emortal.lobby.commands.SpawnCommand
import emortal.lobby.games.LightsOut
import emortal.lobby.inventories.MusicPlayerInventory
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.advancements.Advancement
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityPotionAddEvent
import net.minestom.server.event.entity.EntityPotionRemoveEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import world.cepi.kstom.Manager
import world.cepi.kstom.command.register
import world.cepi.kstom.command.unregister
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.clone

class LobbyExtension : Extension() {

    companion object {
        val mini = MiniMessage.get()

        val occupiedSeats = mutableSetOf<Point>()
        val armourStandSeatMap = HashMap<Entity, Point>()
        val playerMusicInvMap = HashMap<Player, Inventory>()

        val spawnPosition = Pos(0.5, 65.0, 0.5, 180f, 0f)
        lateinit var lobbyInstance: InstanceContainer

        lateinit var extensionEventNode: EventNode<Event>
    }

    override fun initialize() {
        Manager.block.registerHandler("minecraft:sign") { SignHandler }
        Manager.block.registerHandler("minecraft:campfire") { CampfireHandler }
        Manager.block.registerHandler("minecraft:skull") { SkullHandler }

        lobbyInstance = Manager.instance.createInstanceContainer()
        lobbyInstance.chunkLoader = AnvilLoader("lobby")
        lobbyInstance.chunkGenerator = VoidGenerator

        extensionEventNode = eventNode

        MusicPlayerInventory.init()
        SpawnCommand.register()
        DiscCommand.register()

        GameManager.registerGame<LightsOut>(
            GameTypeInfo(
                eventNode,
                "lightsout",
                null,
                false,
                GameOptions(
                    { lobbyInstance },
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    joinableMidGame = true,
                    autoRejoin = false,
                    hasScoreboard = false
                )
            )
        )

        eventNode.listenOnly<AdvancementTabEvent> {

            Advancement()
            this.player.closeInventory()

            val message = mini.parse("<green>Thanks for your interest!</green> <gray>Click to join our Discord</gray>")

            this.player.sendMessage(message)
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

        eventNode.listenOnly<PlayerLoginEvent> {
            setSpawningInstance(lobbyInstance)
            player.respawnPoint = spawnPosition

            playerMusicInvMap[player] = MusicPlayerInventory.inventory.clone()
        }
        eventNode.listenOnly<PlayerSpawnEvent> {
            if (spawnInstance != lobbyInstance) {

            } else {

                val game = player.joinGameOrNew<LightsOut>(GameOptions(
                    { lobbyInstance },
                    1,
                    Integer.MAX_VALUE, // We want to handle starting the game ourselves
                    joinableMidGame = true,
                    autoRejoin = false,
                    hasScoreboard = false
                ))

                game.start()

                player.respawnPoint = spawnPosition
                player.gameMode = GameMode.ADVENTURE

            }
        }
        eventNode.listenOnly<PlayerMoveEvent> {
            if (player.instance!! != lobbyInstance) return@listenOnly
            if (lobbyInstance.getBlock(newPosition).compare(Block.RAIL)) {
                player.addEffect(Potion(PotionEffect.LEVITATION, 25, 3))
            }
            if (lobbyInstance.getBlock(newPosition).compare(Block.CAVE_VINES_PLANT)) {
                player.addEffect(Potion(PotionEffect.GLOWING, 0, 3*20))
            }
            if (lobbyInstance.getBlock(newPosition.sub(0.0, 1.0, 0.0)).compare(Block.SLIME_BLOCK)) {
                player.addEffect(Potion(PotionEffect.JUMP_BOOST, 10, 10))
            }
        }

        eventNode.listenOnly<PlayerPacketEvent> {
            if (player.instance!! != lobbyInstance) return@listenOnly

            if (packet is ClientSteerVehiclePacket) {
                if ((packet as ClientSteerVehiclePacket).flags.toInt() == 2) {
                    if (player.vehicle != null) {
                        val entity = player.vehicle!!
                        entity.removePassenger(player)

                        if (armourStandSeatMap.contains(entity)) {
                            occupiedSeats.remove(armourStandSeatMap[entity])
                            armourStandSeatMap.remove(entity)
                            entity.remove()
                            player.velocity = Vec(0.0, 10.0, 0.0)
                        }
                    }
                }
            }
        }

        eventNode.listenOnly<PlayerBlockInteractEvent> {
            if (player.instance!! != lobbyInstance) return@listenOnly

            if (block.name().contains("stair", true)) {
                if (player.vehicle != null) return@listenOnly

                occupiedSeats.add(blockPosition)

                val armourStand = Entity(EntityType.ARMOR_STAND)
                val armourStandMeta = armourStand.entityMeta as ArmorStandMeta
                armourStandMeta.setNotifyAboutChanges(false)
                armourStandMeta.isSmall = true
                armourStandMeta.isHasNoBasePlate = true
                armourStandMeta.isMarker = true
                armourStandMeta.isInvisible = true
                armourStandMeta.setNotifyAboutChanges(true)

                val spawnPos = blockPosition.add(0.5, 0.3, 0.5)
                var yaw = 0f
                val facing = block.getProperty("facing")

                if (facing == "east") yaw = 90f
                if (facing == "south") yaw = 180f
                if (facing == "west") yaw = -90f

                armourStand.setInstance(lobbyInstance, Pos(spawnPos).withYaw(yaw))
                armourStand.addPassenger(player)

                armourStandSeatMap[armourStand] = blockPosition
            }
        }

        logger.info("[LobbyExtension] Initialized!")
    }

    override fun terminate() {
        SpawnCommand.unregister()
        DiscCommand.unregister()

        logger.info("[LobbyExtension] Terminated!")
    }

}