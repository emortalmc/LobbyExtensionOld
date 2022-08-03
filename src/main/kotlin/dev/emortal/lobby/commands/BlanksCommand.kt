package dev.emortal.lobby.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.lobby.games.LobbyExtensionGame
import dev.emortal.lobby.occurrences.ChatOccurrence
import dev.emortal.lobby.occurrences.ChatOccurrence.Companion.playerCorrectTag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.kommand.Kommand

object BlanksCommand : Kommand({

    onlyPlayers()

    val inputArgument = ArgumentType.StringArray("input")

    syntax(inputArgument) {

        if (player.hasTag(playerCorrectTag)) {
            player.sendMessage(Component.text("You have already gotten this "))
            return@syntax
        }

        val input = (!inputArgument).joinToString(separator = " ")

        val lobbyGame = (player.game ?: return@syntax) as? LobbyExtensionGame ?: return@syntax
        val chatOccTag = lobbyGame.instance.get()?.getTag(ChatOccurrence.chatOccTag) ?: return@syntax

        if (input != chatOccTag) {
            player.sendMessage(Component.text("That is not the word!", NamedTextColor.RED))
            return@syntax
        }

        player.sendMessage("Correct!")
        player.setTag(playerCorrectTag, true)
        lobbyGame.currentOccurrence?.stop(lobbyGame)

    }

}, "blanks")