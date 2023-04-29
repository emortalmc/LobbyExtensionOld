package dev.emortal.lobby.commands

import dev.emortal.lobby.LobbyMain
import dev.emortal.lobby.occurrences.ChatOccurrence
import dev.emortal.lobby.occurrences.ChatOccurrence.Companion.playerCorrectTag
import dev.emortal.lobby.occurrences.Occurrence
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentStringArray
import net.minestom.server.entity.Player

object BlanksCommand : Command("blanks") {

    init {
        val inputArgument = ArgumentStringArray("input")

        addSyntax({ sender, context ->
            val player = sender as? Player ?: return@addSyntax

            if (player.hasTag(playerCorrectTag)) {
                player.sendMessage(Component.text("You have already gotten this "))
                return@addSyntax
            }

            val input = context.get(inputArgument).joinToString(separator = " ")

            if (input != LobbyMain.instance.getTag(ChatOccurrence.chatOccTag)) {
                player.sendMessage(Component.text("That is not the word!", NamedTextColor.RED))
                return@addSyntax
            }

            player.sendMessage("Correct!")
            player.setTag(playerCorrectTag, true)
            Occurrence.currentOccurrence?.stop()
        }, inputArgument)
    }

}