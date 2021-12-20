package dev.emortal.lobby.inventories

import dev.emortal.immortal.game.GameManager
import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.games.LobbyGame
import dev.emortal.lobby.util.pointsBetween
import dev.emortal.lobby.util.setBlock
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.metadata.other.AreaEffectCloudMeta
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.item.item
import world.cepi.particle.Particle
import world.cepi.particle.ParticleType
import world.cepi.particle.data.OffsetAndSpeed
import world.cepi.particle.showParticle
import java.time.Duration

object LecternGameSelectorInventory {
    val inventory: Inventory
    val lecternInventoryTag = Tag.Byte("lectern")
    val selectedGameTag = Tag.String("selectedGame")
    val portalPoints = pointsBetween(Pos(1.0, 70.0, -37.5), Pos(-1.0, 71.0, -37.5))

    init {
        val inventoryTitle = Component.text("Select a game", NamedTextColor.BLACK)
        inventory = Inventory(InventoryType.CHEST_6_ROW, inventoryTitle)
        inventory.setTag(lecternInventoryTag, 1)

        val itemStacks = Array(inventory.size) { ItemStack.AIR }

        // TODO: Add player count of the game to lore
        LobbyExtension.gameListingConfig.gameListings.forEach {
            val loreList = it.value.description.toMutableList()
            loreList.addAll(listOf("", "<green>● <bold>${GameManager.gameMap[it.key]?.size ?: 0}</bold> playing"))

            itemStacks[it.value.slot] = item(it.value.item) {
                displayName(it.value.name.asMini().decoration(TextDecoration.ITALIC, false))
                lore(loreList.map { loreLine -> loreLine.asMini().decoration(TextDecoration.ITALIC, false) })
                setTag(GameManager.gameNameTag, it.key)
            }
        }

        inventory.copyContents(itemStacks)

        inventory.addInventoryCondition { player, _, _, inventoryConditionResult ->
            inventoryConditionResult.isCancel = true

            if (inventoryConditionResult.clickedItem == ItemStack.AIR) return@addInventoryCondition

            if (inventoryConditionResult.clickedItem.hasTag(GameManager.gameNameTag)) {
                val gameName = inventoryConditionResult.clickedItem.getTag(GameManager.gameNameTag)

                player.setTag(selectedGameTag, gameName)
                player.closeInventory()

                val lastPos = player.position
                player.gameMode = GameMode.SPECTATOR

                val hologram = Entity(EntityType.AREA_EFFECT_CLOUD)
                val holoMeta = hologram.entityMeta as AreaEffectCloudMeta

                holoMeta.setNotifyAboutChanges(false)
                holoMeta.radius = 0f
                holoMeta.isHasNoGravity = true
                holoMeta.setNotifyAboutChanges(true)

                hologram.setInstance(player.instance!!, LobbyGame.gameSelectorPos.add(0.0, 1.0, 0.0))
                player.spectate(hologram)

                portalPoints.forEach {
                    player.setBlock(it, Block.AIR)
                }

                player.playSound(Sound.sound(SoundEvent.BLOCK_PORTAL_TRIGGER, Sound.Source.MASTER, 1f, 1.5f))
                player.showTitle(
                    Title.title(
                        Component.empty(),
                        Component.text("Creating portal...", NamedTextColor.GRAY),
                        Title.Times.of(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1))
                    )
                )

                Manager.scheduler.buildTask {
                    player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 0.7f))
                    player.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.MASTER, 2f, 0.5f))
                    player.showParticle(
                        Particle.particle(
                            type = ParticleType.EXPLOSION_EMITTER,
                            count = 1,
                            data = OffsetAndSpeed()
                        ), Vec(0.5, 70.5, -37.5)
                    )

                    player.showTitle(
                        Title.title(
                            Component.empty(),
                            Component.text("Portal created!", NamedTextColor.LIGHT_PURPLE),
                            Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))
                        )
                    )

                    Manager.scheduler.buildTask {
                        player.stopSpectating()
                        player.gameMode = GameMode.ADVENTURE
                        player.teleport(lastPos)
                        hologram.remove()
                    }.delay(Duration.ofMillis(1000)).schedule()

                    portalPoints.forEach {
                        player.setBlock(it, Block.NETHER_PORTAL)
                    }
                }.delay(Duration.ofMillis(3000)).schedule()
            }
        }
    }

}