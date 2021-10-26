package dev.emortal.lobby.commands

import dev.emortal.lobby.LobbyExtension
import dev.emortal.lobby.LobbyExtension.Companion.SPAWN_POINT
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.command.builder.Command
import world.cepi.kstom.Manager
import java.time.Duration

object SpawnCommand : Command("spawn", "lobby", "hub", "l") {

    init {
        setDefaultExecutor { sender, _ ->
            if (!sender.isPlayer) return@setDefaultExecutor

            val player = sender.asPlayer()

            player.sendActionBar(Component.text("Joining lobby...", NamedTextColor.GREEN))

            player.showTitle(
                Title.title(
                    Component.text("\uE00A"),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ofMillis(500),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500)
                    )
                )
            )

            Manager.scheduler.buildTask {
                if (player.instance!! != LobbyExtension.lobbyInstance) {
                    player.respawnPoint = SPAWN_POINT
                    player.setInstance(LobbyExtension.lobbyInstance, SPAWN_POINT)
                } else {
                    player.teleport(SPAWN_POINT)
                }
            }.delay(Duration.ofMillis(500)).schedule()
        }
    }

}