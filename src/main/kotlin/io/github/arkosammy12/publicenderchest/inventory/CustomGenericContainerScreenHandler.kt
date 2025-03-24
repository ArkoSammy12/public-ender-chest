package io.github.arkosammy12.publicenderchest.inventory

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity

class CustomGenericContainerScreenHandler(
    type: ScreenHandlerType<*>,
    syncId: Int,
    playerInventory: PlayerInventory,
    inventory: Inventory,
    rows: Int
) : GenericContainerScreenHandler(type, syncId, playerInventory, inventory, rows) {

    override fun quickMove(player: PlayerEntity, slotNumber: Int): ItemStack {
        val inventory: Inventory = this.inventory
        if (inventory !is PublicInventory || player !is ServerPlayerEntity) {
            return super.quickMove(player, slotNumber)
        }
        inventory.currentPlayerHandler = player
        val returnStack: ItemStack = super.quickMove(player, slotNumber)
        inventory.currentPlayerHandler = null
        return returnStack
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType?, player: PlayerEntity?) {
        val inventory: Inventory = this.inventory
        if (inventory !is PublicInventory || player !is ServerPlayerEntity) {
            super.onSlotClick(slotIndex, button, actionType, player)
            return
        }
        inventory.currentPlayerHandler = player
        super.onSlotClick(slotIndex, button, actionType, player)
        inventory.currentPlayerHandler = null
    }

}