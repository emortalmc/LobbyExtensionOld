package dev.emortal.lobby.inventories

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.inventory.GUI
import dev.emortal.immortal.util.noItalic
import dev.emortal.immortal.util.sendServer
import dev.emortal.lobby.LobbyExtensionMain
import dev.emortal.lobby.config.GameListing
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemHideFlag
import net.minestom.server.item.ItemStack

class GameSelectorGUI : GUI() {

    override fun createInventory(): Inventory {
        val inventoryTitle = Component.text("Games", NamedTextColor.BLACK)
        val inventory = Inventory(InventoryType.CHEST_4_ROW, inventoryTitle)

        LobbyExtensionMain.gameListingConfig.gameListings.forEach {
            if (!it.value.itemVisible) return@forEach

            val item = itemFromListing(it.key, it.value, LobbyExtensionMain.playerCountCache[it.key] ?: 0)
            inventory.setItemStack(it.value.slot, item)
        }

        inventory.addInventoryCondition { player, _, _, inventoryConditionResult ->
            inventoryConditionResult.isCancel = true

            if (inventoryConditionResult.clickedItem == ItemStack.AIR) return@addInventoryCondition

            if (inventoryConditionResult.clickedItem.hasTag(GameManager.gameNameTag)) {
                val gameName = inventoryConditionResult.clickedItem.getTag(GameManager.gameNameTag) ?: return@addInventoryCondition
                player.sendServer(gameName)
                player.closeInventory()
            }
        }

        return inventory
    }

    fun refreshPlayers(gameName: String, players: Int) {
        val gameListing = LobbyExtensionMain.gameListingConfig.gameListings[gameName] ?: return
        if (!gameListing.itemVisible) return

        val item = itemFromListing(gameName, gameListing, players)
        inventory.setItemStack(gameListing.slot, item)
    }

    fun itemFromListing(gameName: String, gameListing: GameListing, players: Int): ItemStack {
        val loreList = mutableListOf<String>()
        loreList.add("")
        loreList.addAll(gameListing.description)
        loreList.addAll(listOf(
            "",
            "<dark_gray>/play $gameName",
            "<green>● <bold>${players}</bold> playing"
        ))

        val miniMessage = MiniMessage.miniMessage()

        return ItemStack.builder(gameListing.item)
            .meta {
                it.displayName(miniMessage.deserialize(gameListing.npcTitles.last()).noItalic())
                it.lore(loreList.map { loreLine -> miniMessage.deserialize(loreLine).noItalic() })
                it.hideFlag(*ItemHideFlag.values())
                it.setTag(GameManager.gameNameTag, gameName)
            }
            .build()
    }

}