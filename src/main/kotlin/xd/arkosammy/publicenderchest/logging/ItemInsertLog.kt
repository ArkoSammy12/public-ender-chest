package xd.arkosammy.publicenderchest.logging

import net.minecraft.item.ItemStack
import net.minecraft.text.*
import net.minecraft.util.Formatting
import java.sql.Connection
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDateTime

class ItemInsertLog(
    override val playerName: String,
    override val uuid: String,
    override val itemStack: ItemStack,
    override val timestamp: LocalDateTime
) : InventoryInteractionLog {

    override fun getLogText(): MutableText {
        val duration: Duration = Duration.between(timestamp, LocalDateTime.now())
        val timestampText: MutableText = Text.literal("${InventoryInteractionLog.formatElapsedTime(duration)} ")
            .setStyle(Style.EMPTY.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(timestamp.format(InventoryInteractionLog.DTF)))))
            .formatted(Formatting.DARK_AQUA)
        val playerNameText: MutableText = Text.literal("$playerName ")
            .setStyle(Style.EMPTY.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("$uuid (Click to copy to clipboard)"))).withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid)))
            .formatted(Formatting.AQUA)
        val interactedInventoryText: MutableText = Text.literal("inserted ")
            .formatted(Formatting.GOLD)
        val quantityText: MutableText = Text.literal("${itemStack.count} ")
            .formatted(Formatting.BLUE)
        val itemText: MutableText = Text.literal("${itemStack.getIdentifier()}")
            .setStyle(Style.EMPTY.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_ITEM, HoverEvent.ItemStackContent(this.itemStack))))
            .formatted(Formatting.GREEN)

        return Text.empty().append(timestampText).append(playerNameText).append(interactedInventoryText).append(quantityText).append(itemText)
    }

    override fun consumeDbConnection(connection: Connection) {
        val itemStackJson: String = this.itemStack.getJsonString() ?: return
        connection.prepareStatement("INSERT INTO ${InventoryDatabaseManager.MAIN_TABLE_NAME} (player, uuid, itemStack, timestamp, interactionType) VALUES (?, ?, ?, ?, ?)").use { statement ->
            statement.setString(1, playerName)
            statement.setString(2, uuid)
            statement.setString(3, itemStackJson)
            statement.setTimestamp(4, Timestamp.valueOf(timestamp))
            statement.setString(5, InventoryInteractionType.ITEM_INSERT.asString())
            statement.executeUpdate()
        }
    }

}