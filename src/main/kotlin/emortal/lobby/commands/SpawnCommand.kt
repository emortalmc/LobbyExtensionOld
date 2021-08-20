package emortal.lobby.commands

import emortal.lobby.LobbyExtension
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.minestom.server.command.builder.Command
import world.cepi.kstom.Manager
import java.time.Duration

object SpawnCommand : Command("spawn", "lobby", "hub", "l") {

    init {
        setDefaultExecutor { sender, _ ->
            if (!sender.isPlayer) return@setDefaultExecutor

            val player = sender.asPlayer()

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
                    player.setInstance(LobbyExtension.lobbyInstance, LobbyExtension.spawnPosition)
                } else {
                    player.teleport(LobbyExtension.spawnPosition)
                }
            }.delay(Duration.ofMillis(500)).schedule()
        }
    }

}