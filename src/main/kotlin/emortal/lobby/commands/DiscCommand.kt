package emortal.lobby.commands

import emortal.lobby.LobbyExtension
import emortal.lobby.inventories.MusicPlayerInventory
import emortal.lobby.util.MusicDisc
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import world.cepi.kstom.Manager
import world.cepi.kstom.command.addSyntax
import world.cepi.kstom.command.arguments.suggest
import java.time.Duration

object DiscCommand : Command("disc", "disk", "music") {

    val stopPlayingTaskMap = HashMap<Player, Task>()
    val playingDiscTag = Tag.Integer("playingDisc")

    init {
        // If no arguments given, open inventory
        setDefaultExecutor { sender, _ ->
            if (!sender.isPlayer) return@setDefaultExecutor

            val player = sender.asPlayer()

            val playingDisc = player.getTag(DiscCommand.playingDiscTag)?.let { MusicDisc.values()[it] }
            val musicPlayerInventory = LobbyExtension.playerMusicInvMap[player] ?: return@setDefaultExecutor

            val playingText = Component.text("\uF808\uE00B", NamedTextColor.WHITE)
            if (playingDisc != null) playingText.append(
                MusicPlayerInventory.mini.parse("  <gray>Playing: <aqua>${playingDisc.description}</aqua>")
            )

            musicPlayerInventory.title = playingText

            player.openInventory(musicPlayerInventory)
        }

        val discArgument = ArgumentType.String("disc").suggest {
            val list = mutableListOf<String>("stop")
            list.addAll(MusicDisc.values().map { it.shortName })
            list
        }

        addSyntax(discArgument) {
            if (!sender.isPlayer) return@addSyntax

            val player = sender.asPlayer()
            val disc = context.get(discArgument)
            val discValues = MusicDisc.values()
            val playingDisc = player.getTag(playingDiscTag)?.let { discValues[it] }

            playingDisc?.sound?.let {
                player.stopSound(SoundStop.named(it))
                player.removeTag(playingDiscTag)
            }
            stopPlayingTaskMap[player]?.cancel()
            if (disc.lowercase() == "stop") return@addSyntax

            val nowPlayingDisc = MusicDisc.valueOf("MUSIC_DISC_${disc.uppercase()}")
            player.setTag(playingDiscTag, discValues.indexOf(nowPlayingDisc))

            player.playSound(Sound.sound(playingDisc!!.sound, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
            player.sendActionBar(MusicPlayerInventory.mini.parse("<gray>Playing: <aqua>${nowPlayingDisc.description}</aqua>"))

            stopPlayingTaskMap[player] = Manager.scheduler.buildTask {
                player.chat("/disc stop")
            }.delay(Duration.ofSeconds(nowPlayingDisc.length.toLong())).schedule()
        }
    }

}