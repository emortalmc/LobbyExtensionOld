package dev.emortal.lobby.commands

import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.util.MusicDisc
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import net.minestom.server.timer.Task
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand
import java.time.Duration

object DiscCommand : Kommand({
    // If no arguments given, open inventory
    default {
        if (!sender.isPlayer) return@default

        val player = sender.asPlayer()

        val playingDisc = player.getTag(DiscCommand.playingDiscTag)?.let { MusicDisc.values()[it] }
        val musicPlayerInventory = LobbyExtension.playerMusicInvMap[player] ?: return@default

        val playingText = Component.text("\uF808\uE00B", NamedTextColor.WHITE)
        if (playingDisc != null) playingText.append(
            "  <gray>Playing: <aqua>${playingDisc.description}</aqua>".asMini()
        )

        musicPlayerInventory.title = playingText

        player.openInventory(musicPlayerInventory)
    }

    val discArgument = ArgumentType.String("disc").suggest {
        val list = mutableListOf<String>("stop")
        list.addAll(MusicDisc.values().map { it.shortName })
        list
    }

    syntax(discArgument) {
        if (!sender.isPlayer) return@syntax

        val player = sender.asPlayer()
        val disc = context.get(discArgument)
        val discValues = MusicDisc.values()
        val playingDisc = player.getTag(DiscCommand.playingDiscTag)?.let { discValues[it] }

        playingDisc?.sound?.let {
            player.stopSound(SoundStop.named(it))
            player.removeTag(DiscCommand.playingDiscTag)
        }
        DiscCommand.stopPlayingTaskMap[player]?.cancel()
        if (disc.lowercase() == "stop") return@syntax

        val nowPlayingDisc = MusicDisc.valueOf("MUSIC_DISC_${disc.uppercase()}")
        player.setTag(DiscCommand.playingDiscTag, discValues.indexOf(nowPlayingDisc))

        player.playSound(Sound.sound(playingDisc!!.sound, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
        player.sendActionBar("<gray>Playing: <aqua>${nowPlayingDisc.description}</aqua>".asMini())

        DiscCommand.stopPlayingTaskMap[player] = Manager.scheduler.buildTask {
            player.chat("/disc stop")
        }.delay(Duration.ofSeconds(nowPlayingDisc.length.toLong())).schedule()
    }
}, "disc", "disk", "music") {

    val stopPlayingTaskMap = HashMap<Player, Task>()
    val playingDiscTag = Tag.Integer("playingDisc")

}