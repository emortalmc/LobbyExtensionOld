package dev.emortal.lobby.commands

import dev.emortal.lobby.util.armify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand

object PerformanceCommand : Kommand({
    onlyPlayers

    default {
        val ramUsage = Manager.benchmark.usedMemory / 1024 / 1024

        val onlinePlayers = Manager.connection.onlinePlayers.size
        val entities = Manager.instance.instances.sumOf { it.entities.size } - onlinePlayers
        val instances = Manager.instance.instances.size

        MinecraftServer.process().ticker()

        player.sendMessage(
            Component.text()
                .append(Component.text("RAM Usage: ", NamedTextColor.GRAY))
                .append(Component.text("${ramUsage}MB", NamedTextColor.GOLD))
                .append(Component.text("\nTPS: ", NamedTextColor.GRAY))
                .append(Component.text("probably 20", NamedTextColor.GOLD))
                .append(Component.text("\nPlayers: ", NamedTextColor.GRAY))
                .append(Component.text(onlinePlayers, NamedTextColor.GOLD))
                .append(Component.text("\nEntities: ", NamedTextColor.GRAY))
                .append(Component.text(entities, NamedTextColor.GOLD))
                .append(Component.text("\nInstances: ", NamedTextColor.GRAY))
                .append(Component.text(instances, NamedTextColor.GOLD))
                .armify()
        )
    }

}, "perf", "performance", "tps")