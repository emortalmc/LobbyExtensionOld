package dev.emortal.lobby.inventories

import dev.emortal.immortal.game.GameManager
import dev.emortal.lobby.LobbyExtension
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemMeta
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import world.cepi.kstom.adventure.asMini

object GameSelectorInventory {
    val inventory: Inventory

    init {
        val inventoryTitle = Component.text("Games", NamedTextColor.BLACK)
        inventory = Inventory(InventoryType.CHEST_6_ROW, inventoryTitle)

        val itemStacks = Array(inventory.size) { ItemStack.AIR }

        // TODO: Add player count of the game to lore
        LobbyExtension.gameListingConfig.gameListings.forEach {
            val loreList = it.value.description.toMutableList()
            loreList.addAll(listOf("", "<green>● <bold>${GameManager.gameMap[it.key]?.size ?: 0}</bold> playing"))

            itemStacks[it.value.slot] = ItemStack.builder(it.value.item)
                .displayName(it.value.name.asMini().decoration(TextDecoration.ITALIC, false))
                .lore(loreList.map { loreLine -> loreLine.asMini().decoration(TextDecoration.ITALIC, false) })
                .meta<ItemMetaBuilder> { meta ->
                    meta.setTag(GameManager.gameNameTag, it.key)
                    meta
                }
                .build()

        }

        inventory.copyContents(itemStacks)

        inventory.addInventoryCondition { player, _, _, inventoryConditionResult ->
            inventoryConditionResult.isCancel = true

            if (inventoryConditionResult.clickedItem == ItemStack.AIR) return@addInventoryCondition

            if (inventoryConditionResult.clickedItem.hasTag(GameManager.gameNameTag)) {
                val gameName = inventoryConditionResult.clickedItem.getTag(GameManager.gameNameTag)
                player.chat("/play $gameName")
            }
        }
    }

}