package io.github.arkosammy12.publicenderchest.inventory

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.collection.DefaultedList
import io.github.arkosammy12.monkeyconfig.managers.getRawBooleanSettingValue
import io.github.arkosammy12.monkeyconfig.managers.getRawStringListSettingValue
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import io.github.arkosammy12.publicenderchest.config.ConfigUtils
import io.github.arkosammy12.publicenderchest.logging.InventoryInteractionLog
import io.github.arkosammy12.publicenderchest.logging.InventoryInteractionType
import io.github.arkosammy12.publicenderchest.serialization.SerializedItemStack
import io.github.arkosammy12.publicenderchest.util.CustomMutableText
import io.github.arkosammy12.publicenderchest.util.ducks.ItemStackDuck
import java.time.Duration
import java.time.LocalDateTime

class PublicInventory(private val itemStacks: DefaultedList<ItemStack> = DefaultedList.ofSize(SLOT_SIZE, ItemStack.EMPTY)) : Inventory {

    var dirty: Boolean = false
        private set
        get() {
            val currentValue: Boolean = field
            field = false
            return currentValue
        }

    var currentPlayerHandler: ServerPlayerEntity? = null
    private var previousItemStack: DefaultedList<ItemStack> = DefaultedList.copyOf(ItemStack.EMPTY, *this.itemStacks.map { stack -> stack.copy() }.toTypedArray())

    override fun clear() {
        this.itemStacks.clear()
    }

    override fun size(): Int = this.itemStacks.size

    override fun isEmpty(): Boolean {
        for (stack: ItemStack in this.itemStacks) {
            if (!stack.isEmpty) {
                return false
            }
        }
        return true
    }

    override fun getStack(slot: Int): ItemStack =
        if (slot in (0 until this.size())) this.itemStacks[slot] else ItemStack.EMPTY

    override fun removeStack(slot: Int, amount: Int): ItemStack =
        Inventories.splitStack(this.itemStacks, slot, amount)

    override fun removeStack(slot: Int): ItemStack =
        Inventories.removeStack(this.itemStacks, slot)

    override fun setStack(slot: Int, stack: ItemStack) {
        this.itemStacks[slot] = stack
        if (stack.isEmpty) {
            return
        }
        (stack as ItemStackDuck).`publicenderchest$setInsertedTime`(LocalDateTime.now())
        (stack as ItemStackDuck).`publicenderchest$setInserterName`(this.currentPlayerHandler?.name ?: return)
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean {
        if (player !is ServerPlayerEntity) {
            return false
        }
        if (player.hasPermissionLevel(4)) {
            return true
        }
        val blackListEnabled: Boolean = PublicEnderChest.CONFIG_MANAGER.getRawBooleanSettingValue(ConfigUtils.ENABLE_PLAYER_BLACKLIST) ?: false
        val playerBlacklist: List<String> = PublicEnderChest.CONFIG_MANAGER.getRawStringListSettingValue(ConfigUtils.PLAYER_BLACKLIST) ?: mutableListOf()
        if (blackListEnabled && playerBlacklist.contains(player.name.string)) {
            return false
        }
        val dimensionBlackListEnabled = PublicEnderChest.CONFIG_MANAGER.getRawBooleanSettingValue(ConfigUtils.ENABLE_DIMENSION_BLACKLIST) ?: false
        val dimensionBlackList: List<String> = PublicEnderChest.CONFIG_MANAGER.getRawStringListSettingValue(ConfigUtils.DIMENSION_BLACKLIST) ?: mutableListOf()
        val currentWorld: String = player.world.registryKey.value.toString()
        if (dimensionBlackListEnabled && dimensionBlackList.contains(currentWorld)) {
            return false
        }
        return true
    }

    override fun markDirty() {
        this.dirty = true

        val player: ServerPlayerEntity? = this.currentPlayerHandler
        if (player == null) {
            this.updatePreviousStacks()
            return
        }

        for (i in 0 until this.itemStacks.size) {

            val previousStack: ItemStack = this.previousItemStack[i]
            val currentStack: ItemStack = this.itemStacks[i]

            // If the item stacks are of the same item, and
            // have the same components and count, then this slot remained unchanged
            if (ItemStack.areEqual(previousStack, currentStack)) {
                continue
            }

            // If the item stacks are of the same item and have the same components,
            // but are different in count, then log based on the count difference
            if (ItemStack.areItemsAndComponentsEqual(previousStack,  currentStack)) {
                val countDifference: Int = currentStack.count - previousStack.count
                if (countDifference > 0) {
                    val insertAction: InventoryInteractionLog = InventoryInteractionLog.of(InventoryInteractionType.ITEM_INSERT, player, currentStack, countDifference)
                    PublicEnderChest.DATABASE_MANAGER.logInventoryInteraction(insertAction, player.server)
                } else if (countDifference < 0) {
                    val removeAction: InventoryInteractionLog = InventoryInteractionLog.of(InventoryInteractionType.ITEM_REMOVE, player, currentStack, countDifference)
                    PublicEnderChest.DATABASE_MANAGER.logInventoryInteraction(removeAction, player.server)
                }
                continue
            }

            // If the item stacks differ in components and count,
            // then the item stacks cannot be combined,
            // so a complete replacement of the stack has occurred in this slot.
            // Log a removal and insertion based on the previous and current items stacks for the slot.
            if (!previousStack.isEmpty) {
                val removeAction: InventoryInteractionLog = InventoryInteractionLog.of(InventoryInteractionType.ITEM_REMOVE, player, previousStack)
                PublicEnderChest.DATABASE_MANAGER.logInventoryInteraction(removeAction, player.server)
            }
            if (!currentStack.isEmpty) {
                val insertAction: InventoryInteractionLog = InventoryInteractionLog.of(InventoryInteractionType.ITEM_INSERT, player, currentStack)
                PublicEnderChest.DATABASE_MANAGER.logInventoryInteraction(insertAction, player.server)
            }

        }
        this.updatePreviousStacks()
    }

    private fun updatePreviousStacks() {
        this.previousItemStack = DefaultedList.copyOf(ItemStack.EMPTY, *this.itemStacks.map { stack -> stack.copy() }.toTypedArray())
    }

    companion object {

        const val SLOT_SIZE = 54

        val CODEC: Codec<PublicInventory> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.list(SerializedItemStack.CODEC).fieldOf("item_stacks").forGetter { publicInventory ->
                    val serializedItemStacks: MutableList<SerializedItemStack> = mutableListOf()
                    for(i in 0 until publicInventory.size()) {
                        val currentStack = publicInventory.itemStacks[i]
                        if (currentStack.isEmpty) {
                            continue
                        }
                        serializedItemStacks.add(SerializedItemStack(i, currentStack))
                    }
                    return@forGetter serializedItemStacks
                }
            ).apply(instance) { serializedItemStacks ->
                val deserializedItemStacks: DefaultedList<ItemStack> = DefaultedList.ofSize(SLOT_SIZE, ItemStack.EMPTY)
                for (indexedStack: SerializedItemStack in serializedItemStacks) {
                    deserializedItemStacks[indexedStack.slotIndex] = indexedStack.itemStack
                }
                PublicInventory(DefaultedList.copyOf(ItemStack.EMPTY, *deserializedItemStacks.toTypedArray()))
            }
        }
    }

}

fun ItemStack.getCustomInfoLines() : List<CustomMutableText> {
    if (this.isEmpty) {
        return listOf()
    }
    val inserterName: Text = (this as ItemStackDuck).`publicenderchest$getInserterName`() ?: return listOf()
    var infoText: CustomMutableText = CustomMutableText(Text.empty().append(Text.literal("Inserted by ")).append(MutableText.of(inserterName.content).formatted(Formatting.ITALIC)))
    val insertedTime: LocalDateTime? = (this as ItemStackDuck).`publicenderchest$getInsertedTime`()
    if (insertedTime != null) {
        val duration: Duration = Duration.between(insertedTime, LocalDateTime.now())
        val elapsedTime: String = InventoryInteractionLog.formatElapsedTime(duration)
        infoText = CustomMutableText(Text.empty().append(infoText).append(Text.literal(" $elapsedTime")))
    }
    return listOf(infoText)
}