package dev.emortal.lobby.commands

import dev.emortal.lobby.commands.PerformanceCommand.LAST_TICK
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.monitoring.TickMonitor
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.floor
import kotlin.math.min


object PerformanceCommand : Kommand({
     onlyPlayers

    default {
        val ramUsage = Manager.benchmark.usedMemory / 1024 / 1024
        val tickMonitor = LAST_TICK.get()
        val tickMs = tickMonitor.tickTime
        val tps = floor(1000 / tickMs).toInt()

        player.sendMessage(
            Component.text()
                .append(Component.text("RAM Usage: ", NamedTextColor.GRAY))
                .append(Component.text("${ramUsage}MB", NamedTextColor.GOLD))
                .append(Component.text("\nTPS: ", NamedTextColor.GRAY))
                .append(Component.text("${min(tps, 20)}", NamedTextColor.GOLD))
                .append(Component.text(" (${tickMs}ms)", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC))
        )
    }

}, "perf", "performance", "tps") {

    private val LAST_TICK = AtomicReference<TickMonitor>()

    init {
        Manager.update.addTickMonitor(LAST_TICK::set);
    }

}