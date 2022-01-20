package dev.emortal.lobby.commands

import dev.emortal.lobby.commands.DiscordCommand.message
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import world.cepi.kstom.command.kommand.Kommand

object DiscordCommand : Kommand({

    default {
        sender.sendMessage(message)
    }

}, "discord") {
    private val message = Component.text()
        .append(Component.text("Click ", NamedTextColor.GRAY))
        .append(
            Component.text("HERE", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD, TextDecoration.UNDERLINED)
                .hoverEvent(HoverEvent.showText(Component.text("https://discord.gg/TZyuMSha96", NamedTextColor.GREEN)))
                .clickEvent(ClickEvent.openUrl("https://discord.gg/TZyuMSha96"))
        )
        .append(Component.text(" to join our Discord!", NamedTextColor.GRAY))
}
