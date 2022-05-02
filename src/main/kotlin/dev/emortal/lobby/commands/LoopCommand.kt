package dev.emortal.lobby.commands

import dev.emortal.lobby.commands.LoopCommand.loopTag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.tag.Tag
import world.cepi.kstom.command.kommand.Kommand

object LoopCommand : Kommand({

    onlyPlayers

    default {

        if (player.hasTag(loopTag)) {
            player.removeTag(loopTag)
            player.sendMessage(Component.text("Current song will no longer loop!", NamedTextColor.GREEN))
        } else {
            player.setTag(loopTag, true)
            player.sendMessage(Component.text("Current song will now loop!", NamedTextColor.GREEN))
        }

    }



}, "loop") {

    val loopTag = Tag.Boolean("loopSong")

}