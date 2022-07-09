package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.lobby.games.LobbyGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.command.kommand.Kommand

object SitCommand : Kommand({

    onlyPlayers

    condition {
        if (sender !is Player) {
            return@condition false
        }
        if ((sender as Player).instance?.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
            return@condition true
        }
        return@condition false
    }

    default {
        if (!player.instance!!.getTag(GameManager.gameNameTag).contentEquals("lobby", true)) {
            player.sendActionBar(Component.text("Not in a lobby!", NamedTextColor.RED))
            return@default
        }

        if (player.vehicle != null) return@default

        var i = 0
        while (true) {
            i++
            if (!player.instance!!.getBlock(player.position.blockX(), player.position.blockY() - i, player.position.blockZ()).compare(Block.AIR))
                break

            if (i > 3) {
                player.sendActionBar(Component.text("Couldn't reserve a seat", NamedTextColor.RED))
                return@default
            }
        }

        val roundedPos = Pos(
            player.position.blockX().toDouble(),
            player.position.blockY().toDouble() - (i - 1),
            player.position.blockZ().toDouble()
        )

        val game = player.game as LobbyGame

        if (game.occupiedSeats.contains(roundedPos)) {
            player.sendActionBar(Component.text("You can't sit on someone's lap", NamedTextColor.RED))
            return@default
        }

        if (MountCommand.mountMap.containsKey(player)) {
            player.playSound(Sound.sound(SoundEvent.ENTITY_DOLPHIN_PLAY, Sound.Source.MASTER, 1f, 1f))
            player.sendMessage(
                Component.text()
                    .append(Component.text("RC Dolphin (⌐▨_▨)", NamedTextColor.RED))
            )
        }

        game.occupiedSeats.add(roundedPos)

        val armourStand = Entity(EntityType.ARMOR_STAND)
        val armourStandMeta = armourStand.entityMeta as ArmorStandMeta
        armourStandMeta.setNotifyAboutChanges(false)
        armourStandMeta.isSmall = true
        armourStandMeta.isHasNoBasePlate = true
        armourStandMeta.isMarker = true
        armourStandMeta.isInvisible = true
        armourStandMeta.setNotifyAboutChanges(true)
        armourStand.setNoGravity(true)

        val spawnPos = roundedPos.add(0.5, -0.3, 0.5)
        armourStand.setInstance(player.instance!!, spawnPos.withYaw(player.position.yaw))
            .thenRun {
                armourStand.addPassenger(player)
            }

        game.armourStandSeatMap[armourStand] = roundedPos
    }

}, "sit")