package io.github.arkosammy12.publicenderchest.logging

import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import java.sql.Connection
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

sealed interface InventoryInteractionLog {

    val playerName: String

    val uuid: String

    val itemStack: ItemStack

    val timestamp: LocalDateTime

    fun getLogText() : MutableText

    fun consumeDbConnection(connection: Connection, registries: RegistryWrapper.WrapperLookup)

    companion object {

        val DTF: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm:ss a")

        fun of(inventoryInteractionType: InventoryInteractionType, player: ServerPlayerEntity, itemStack: ItemStack, count: Int = itemStack.count) : InventoryInteractionLog {
            val now: LocalDateTime = LocalDateTime.now()
            val playerName: String = player.name.string
            val uuid: String = player.uuid.toString()
            return when (inventoryInteractionType) {
                InventoryInteractionType.ITEM_REMOVE -> ItemRemoveLog(playerName, uuid, itemStack.copy().also { s -> s.count = abs(count) }, now)
                InventoryInteractionType.ITEM_INSERT -> ItemInsertLog(playerName, uuid, itemStack.copy().also { s -> s.count = abs(count) }, now)
            }
        }

        fun formatElapsedTime(duration: Duration) : String {
            val days: Long = duration.toDays()
            val hours: Long = duration.toHours() % 24
            val minutes: Long = duration.toMinutes() % 60
            val seconds: Long = duration.toSeconds() % 60
            var result = "${seconds}s ago"
            if (minutes > 0) {
               result = "${minutes}m ago"
            }
            if (hours > 0) {
                result = "${hours}h ago"
            }
            if (days > 0) {
                result = "${days}d ago"
            }
            return result
        }

    }

}