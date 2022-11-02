package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.lobby.games.LobbyExtensionGame
import dev.emortal.lobby.occurrences.ChatOccurrence
import dev.emortal.lobby.occurrences.ChatOccurrence.Companion.playerCorrectTag
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

            val lobbyGame = (player.game ?: return@addSyntax) as? LobbyExtensionGame ?: return@addSyntax
            val chatOccTag = lobbyGame.instance?.getTag(ChatOccurrence.chatOccTag) ?: return@addSyntax

            if (input != chatOccTag) {
                player.sendMessage(Component.text("That is not the word!", NamedTextColor.RED))
                return@addSyntax
            }

            player.sendMessage("Correct!")
            player.setTag(playerCorrectTag, true)
            lobbyGame.currentOccurrence?.stop(lobbyGame)
        }, inputArgument)
    }

}