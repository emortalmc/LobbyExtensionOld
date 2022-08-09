package dev.emortal.lobby

import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.WhenToRegisterEvents
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.RedisStorage.redisson
import dev.emortal.lobby.commands.*
import dev.emortal.lobby.config.GameListingConfig
import dev.emortal.lobby.games.LobbyExtensionGame
import dev.emortal.lobby.inventories.GameSelectorGUI
import dev.emortal.lobby.inventories.MusicPlayerInventory
import dev.emortal.lobby.util.showFirework
import dev.emortal.tnt.TNTLoader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.IChunkLoader
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.firework.FireworkEffectType
import org.tinylog.kotlin.Logger
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class LobbyExtension : Extension() {

    companion object {
        val npcs = CopyOnWriteArrayList<PacketNPC>()
        val oldNpcs = CopyOnWriteArrayList<PacketNPC>()

        lateinit var gameListingConfig: GameListingConfig
        val gameListingPath = Path.of("./gameListings.json")

        lateinit var gameSelectorGUI: GameSelectorGUI

        val playerCountCache = ConcurrentHashMap<String, Int>()

        lateinit var sharedLoader: IChunkLoader
        lateinit var oldSharedLoader: IChunkLoader
    }

    override fun initialize() {
        gameListingConfig = ConfigHelper.initConfigFile(gameListingPath, GameListingConfig())
        gameSelectorGUI = GameSelectorGUI()

        Logger.info("Preloading lobby world")
        sharedLoader = TNTLoader("./lobby.tnt")

        GameManager.registerGame<LobbyExtensionGame>(
            "lobby",
            Component.text("Lobby", NamedTextColor.GREEN),
            showsInSlashPlay = false,
            canSpectate = false,
            WhenToRegisterEvents.IMMEDIATELY,
            GameOptions(
                maxPlayers = 50,
                minPlayers = 0,
                countdownSeconds = 0,
                canJoinDuringGame = true,
                showScoreboard = false,
                allowsSpectators = false
            )
        )

        oldNpcs.add(
            PacketNPC(
                Pos(-8.5, 100.0, 5.5),
                listOf(Component.text("Block Sumo", NamedTextColor.GOLD, TextDecoration.BOLD)),
                "blocksumo",
                PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTY0ODk0Njk5OTE3OSwKICAicHJvZmlsZUlkIiA6ICI3YmQ1YjQ1OTFlNmI0NzUzODI3NDFmYmQyZmU5YTRkNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJlbW9ydGFsZGV2IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzkxZDdhOWMwODBlZTdjYTZkZjlhYWJlM2I5NTliYWE4MThkYWUzYjQ1ZWI3YWRjMTMwZmYyNjU1YzlkOTRjY2YiCiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzIzNDBjMGUwM2RkMjRhMTFiMTVhOGIzM2MyYTdlOWUzMmFiYjIwNTFiMjQ4MWQwYmE3ZGVmZDYzNWNhN2E5MzMiCiAgICB9CiAgfQp9", "prWY0EtGdAfqt/nc4Vv/sMBcVEb6WMvaRumTk72e/NKe3dUPxmzlDRm6rw/mEE332JND6+sEI9PmDQ4jj/W41cn/XR/uZIBS+1qLE+57slEQA+ds+/kffKt1358JEV5/qyqCnODLVjwsRwazXJstC3eKNByaTQEyZ2jv/mFeAIAOF+0eQqDaaMGgxdIMRvWR8Nj6uIiBFTdCPIw3OYZ5bxqxm8Epr5PppF+sj7ZK0UyQ5/f1UoO9B/YMUt0OW+UlF3NYcMDs5mrg1N8Ajxvqbe8l0X7eWHgSYd0S/FopSCiVVQOQdZHRyhicmzLv6rE+xW03SB8NRfoaEEvSh8+QiLGMcyETeriwdDzdf8H9Iin3vDkVbMyRTAJ5jL/xDkoFDFR5HtNkrwYBRJoVkiWbnWyoBFofAjmmMmGmT5SFABS7I0iWLEoP4EMzqy84zDbpwOOioQz9UFlZV8AqmyEDv8Hx6px20zdR/jPr7tRQgqRhcPyzNsElcNLhkBfHmhpKffkrEPOAaal49rtB+3Jq+nX8Z1VyEZSW4MYnuq91bFZ1ciMzopYulPwP4cZkrGaqV84lxsqStI5+STi105KP4Bws+XDpop1eyPdDuVL/axq3VVkKeRqSoMv+xRYONGZJQgZ1t0WRIsXk7DLsjtx8QgffxqOwjW5CmZR0liSPfYM=")
            )
        )
        oldNpcs.add(
            PacketNPC(
                Pos(-10.5, 100.0, 0.5),
                listOf(Component.text("Survival", NamedTextColor.GREEN, TextDecoration.BOLD)),
                "",
                PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTYwOTMyNzE4Njc1MiwKICAicHJvZmlsZUlkIiA6ICI2ZmU4OTUxZDVhY2M0NDc3OWI2ZmYxMmU3YzFlOTQ2MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJlcGhlbXJhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzRlNjc2ZmY4ZmRjODk3Zjg5M2I3ZWY4NzliMWU5Y2ZiNmQ4M2I0ZmI0NjBiNDM0MTAyNjQwNTllNzU1YTA4NjEiCiAgICB9CiAgfQp9", "QiPzP+TZqfXZPiXsLHaEBLtkFX2KEWutEc8QE1nVIkNkWL4DpxfKl0Kw0Bd+HnotD3xGFyyORMKLCPhhEP1Ob52dXceGpkgX10XMcGwvZXJnR01eIeGKKHL353uklP3xxDY04MWwIlU55ogeyVTCjE9lC92wwifDDbXR0VSJ8uECN9YtXiG/hStwmjBH6N2KM3yevDNeqRe8DphywgSnIwBhDq9qNkert08mKjVbrEhxgrU8K0TJaZSHEz+ilR930fE8Afcau/zwzsWhsqzoY11wh2poZU9O6AU0C/x5/ZYbddn9iqbj7gx0jmsFq4Ri3BLfBYb7xK6KrZVe4RZz/muBjvSaPwSA7ydJgsXbZ/OakAg2/V7YuEE5AQ2LdCeWBWboFxaXvHFUJkH7vRuUoEoIZoLu5C7mICGZiFw/I84eieau2AOBi2CjTVww4vO1MyeeNGt6tn6j5EUsr3Vlii6AbNwot5lagbzWW0pLdV0c3ux4TTEhaeGSBKAf+ckuOKJjkFfNuyVOKxAzpGUIjJP0NSJ6+xixw7cdz8tOJWWRjGgIv8FQNMAHq2Ut1vXzwclyCD5ZkAjNnK/hFhItsNNXVNLNF9fjfY7zxwgMhlJcLu03Jk3swuPDIU3t13ND2TI31/A4zIyfYcjYVSHn7vg2SrOne2731Pxr1USNaq4=")
            )
        )
        oldNpcs.add(
            PacketNPC(
                Pos(-8.5, 100.0, -4.5),
                listOf(Component.text("Parkour", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)),
                "",
                PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTYwOTMyNzE4Njc1MiwKICAicHJvZmlsZUlkIiA6ICI2ZmU4OTUxZDVhY2M0NDc3OWI2ZmYxMmU3YzFlOTQ2MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJlcGhlbXJhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzRlNjc2ZmY4ZmRjODk3Zjg5M2I3ZWY4NzliMWU5Y2ZiNmQ4M2I0ZmI0NjBiNDM0MTAyNjQwNTllNzU1YTA4NjEiCiAgICB9CiAgfQp9", "QiPzP+TZqfXZPiXsLHaEBLtkFX2KEWutEc8QE1nVIkNkWL4DpxfKl0Kw0Bd+HnotD3xGFyyORMKLCPhhEP1Ob52dXceGpkgX10XMcGwvZXJnR01eIeGKKHL353uklP3xxDY04MWwIlU55ogeyVTCjE9lC92wwifDDbXR0VSJ8uECN9YtXiG/hStwmjBH6N2KM3yevDNeqRe8DphywgSnIwBhDq9qNkert08mKjVbrEhxgrU8K0TJaZSHEz+ilR930fE8Afcau/zwzsWhsqzoY11wh2poZU9O6AU0C/x5/ZYbddn9iqbj7gx0jmsFq4Ri3BLfBYb7xK6KrZVe4RZz/muBjvSaPwSA7ydJgsXbZ/OakAg2/V7YuEE5AQ2LdCeWBWboFxaXvHFUJkH7vRuUoEoIZoLu5C7mICGZiFw/I84eieau2AOBi2CjTVww4vO1MyeeNGt6tn6j5EUsr3Vlii6AbNwot5lagbzWW0pLdV0c3ux4TTEhaeGSBKAf+ckuOKJjkFfNuyVOKxAzpGUIjJP0NSJ6+xixw7cdz8tOJWWRjGgIv8FQNMAHq2Ut1vXzwclyCD5ZkAjNnK/hFhItsNNXVNLNF9fjfY7zxwgMhlJcLu03Jk3swuPDIU3t13ND2TI31/A4zIyfYcjYVSHn7vg2SrOne2731Pxr1USNaq4=")
            )
        )

        gameListingConfig.gameListings.entries.forEach {
            val hologramLines = it.value.npcTitles.map(String::asMini).toMutableList()

            hologramLines.add(Component.text("0 online", NamedTextColor.GRAY))

            npcs.add(PacketNPC(
                it.value.npcPosition,
                hologramLines,
                it.key,
                if (it.value.npcSkinValue.isNotEmpty() && it.value.npcSkinSignature.isNotEmpty()) PlayerSkin(it.value.npcSkinValue, it.value.npcSkinSignature) else null,
                EntityType.fromNamespaceId(it.value.npcEntityType)
            ))
        }

        redisson?.getTopic("playercount")?.addListenerAsync(String::class.java) { channel, msg ->
            val args = msg.split(" ")
            val gameName = args[0]
            val playerCount = args[1].toInt()

            playerCountCache[gameName] = playerCount
            gameSelectorGUI.refreshPlayers(gameName, playerCount)
            val games = GameManager.gameMap["lobby"]!!
            games.forEach {
                val lobbyGame = it as LobbyExtensionGame
                lobbyGame.refreshHolo(gameName, playerCount)
            }
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {

                if (player.username == "brayjamin") {
                    //NBS.play(susNBS, player)
                    player.sendMessage("sus")
                }

                if (player.displayName != null) player.sendMessage(
                    Component.text()
                        .append(Component.text("Welcome, ", NamedTextColor.GRAY))
                        .append(player.displayName!!)
                        .append(Component.text(", to ", NamedTextColor.GRAY))
                        .append("<bold><gradient:gold:light_purple>EmortalMC".asMini())
                )

                player.scheduler().buildTask {

                    player.showFirework(
                        player.instance!!,
                        player.position.add(0.0, 1.0, 0.0),
                        mutableListOf(
                            FireworkEffect(
                                false,
                                false,
                                FireworkEffectType.LARGE_BALL,
                                mutableListOf(Color(NamedTextColor.LIGHT_PURPLE)),
                                mutableListOf(Color(NamedTextColor.GOLD))
                            )
                        )
                    )
                }.delay(Duration.ofMillis(500)).schedule()
            }
        }

        MusicPlayerInventory.init()
        DiscCommand.register()
        SpawnCommand.register()
        SitCommand.register()
        MountCommand.register()
        FireworkCommand.register()
        LoopCommand.register()

        DiscCommand.refreshSongs()

        StartOccurrence.register()
        BlanksCommand.register()

        logger.info("[LobbyExtension] Initialized!")
    }

    override fun terminate() {
        DiscCommand.unregister()
        SpawnCommand.unregister()
        SitCommand.unregister()
        MountCommand.unregister()
        FireworkCommand.unregister()
        LoopCommand.unregister()

        StartOccurrence.unregister()
        BlanksCommand.unregister()

        logger.info("[LobbyExtension] Terminated!")
    }

}