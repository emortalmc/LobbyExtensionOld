package dev.emortal.lobby.occurrences

import dev.emortal.immortal.util.armify
import dev.emortal.immortal.util.centerText
import dev.emortal.immortal.util.parsed
import dev.emortal.lobby.games.LobbyExtensionGame
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.floor

class ChatOccurrence : Occurrence() {

    companion object {
        val chatOccTag = Tag.String("chatOcc")
        val chatOccStartTag = Tag.Long("chatOccStart")
        val playerCorrectTag = Tag.Boolean("playerCorrectChatOcc")
    }

    override fun started(game: LobbyExtensionGame) {

        val instance = game.instance

        val word = Block.values().random().namespace().path().replace("_", " ")

        val hiddenString = hideLetters(word, 0.5f)

        val startMessage = Component.text()
            .append(Component.text(centerText("Random Occurrence", bold = true), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
            .append(Component.newline())
            .append(Component.text(centerText("Fill in the blanks"), NamedTextColor.GRAY))
            .append(Component.text("\n\n${centerText(hiddenString)}", NamedTextColor.WHITE))
            .append(Component.text("\n\nSubmit answer with /blanks <word>", NamedTextColor.GRAY))
            .build()
            .armify()

        instance.setTag(chatOccTag, word)
        instance.setTag(chatOccStartTag, System.currentTimeMillis())

        game.sendMessage(startMessage)

    }

    override fun stopped(game: LobbyExtensionGame) {
        val instance = game.instance
        val correctPlayer = game.players.firstOrNull { it.hasTag(playerCorrectTag) }

        if (correctPlayer == null) {
            game.sendMessage(
                Component.text()
                    .append(Component.text(centerText("Random Occurrence", bold = true), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text(centerText("Fill in the blanks"), NamedTextColor.GRAY))
                    .append(Component.text("\n\nNo one got the word", NamedTextColor.RED))
                    .append(Component.text("\n\nThe word was: ", NamedTextColor.GRAY))
                    .append(Component.text(instance.getTag(chatOccTag), NamedTextColor.WHITE))
                    .build()
                    .armify()
            )

            game.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
        } else {
            correctPlayer.removeTag(playerCorrectTag)

            val occurrenceSecs = ((System.currentTimeMillis() - instance.getTag(chatOccStartTag)!!) / 1000).parsed()
            game.sendMessage(
                Component.text()
                    .append(Component.text(centerText("Random Occurrence", bold = true), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text(centerText("Fill in the blanks"), NamedTextColor.GRAY))
                    .append(Component.text(centerText("\n\n${correctPlayer.username} got the word in ${occurrenceSecs}"), NamedTextColor.GREEN))
                    .append(Component.text(centerText("\n\nThe word was: "), NamedTextColor.GRAY))
                    .append(Component.text(instance.getTag(chatOccTag), NamedTextColor.WHITE))
                    .build()
                    .armify()
            )
        }

        instance.removeTag(chatOccTag)
    }

    private fun hideLetters(string: String, percent: Float): String {
        val rand = ThreadLocalRandom.current()
        val lettersToReplace = floor(string.length.toFloat() * percent).toInt()
        val possibleLetters = string.toCharArray()

        repeat(lettersToReplace) {
            val randNum = rand.nextInt(possibleLetters.size)
            if (possibleLetters[randNum] == '_' || possibleLetters[randNum] == ' ') return@repeat
            possibleLetters[randNum] = '_'
        }

        return possibleLetters.joinToString(separator = "")
    }
}