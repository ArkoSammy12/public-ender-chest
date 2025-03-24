package io.github.arkosammy12.publicenderchest.logging

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mojang.serialization.DataResult
import com.mojang.serialization.JsonOps
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import io.github.arkosammy12.publicenderchest.getModFolderPath
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

class InventoryDatabaseManager(server: MinecraftServer) {

    init {

        val tableInit = """
           CREATE TABLE IF NOT EXISTS $MAIN_TABLE_NAME (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player TEXT,
            uuid TEXT,
            itemStack TEXT,
            timestamp TIMESTAMP,
            interactionType TEXT
        );"""

        val url: String = getDatabaseFileUrl(server)

        DriverManager.getConnection(url).use { c ->
            try {
                Class.forName("org.sqlite.JDBC")
                val statement: Statement = c.createStatement()
                statement.execute(tableInit)
            } catch (e: Exception) {
                PublicEnderChest.LOGGER.error("Error initializing Public Ender Chest database: $e")
            }
        }

    }

    fun logInventoryInteraction(inventoryInteractionLog: InventoryInteractionLog, server: MinecraftServer) {
        val url: String = getDatabaseFileUrl(server)
        try {
            DriverManager.getConnection(url).use { c ->
                inventoryInteractionLog.consumeDbConnection(c, server.registryManager)
            }
        } catch (e: Exception) {
            PublicEnderChest.LOGGER.error("Error attempting to log to Public Ender Chest database: $e")
        }
    }

    fun query(server: MinecraftServer, queryContext: QueryContext) : List<InventoryInteractionLog> {
        val url: String = getDatabaseFileUrl(server)
        val queryTimeStamp: Timestamp = Timestamp.valueOf(LocalDateTime.now().minusDays(queryContext.days.toLong()).minusHours(queryContext.hours.toLong()).minusMinutes(queryContext.minutes.toLong()).minusSeconds(queryContext.seconds.toLong()))
        var query: String = when (queryContext.timeQueryType) {
            TimeQueryType.BEFORE -> "SELECT * FROM $MAIN_TABLE_NAME WHERE timestamp < ?"
            TimeQueryType.AFTER -> "SELECT * FROM $MAIN_TABLE_NAME WHERE timestamp > ?"
        }
        val playerName: String? = queryContext.playerName
        if (playerName != null) {
            query += " AND player=?"
        }
        val results: MutableList<InventoryInteractionLog> = mutableListOf()
        try {
            DriverManager.getConnection(url).use { connection ->
                connection.prepareStatement(query).use { statement ->
                    statement.setTimestamp(1, queryTimeStamp)
                    if (playerName != null) {
                        statement.setString(2, playerName)
                    }
                    statement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            val playerName: String = resultSet.getString("player")
                            val uuid: String = resultSet.getString("uuid")
                            val itemString: String = resultSet.getString("itemStack")
                            val timeStamp: LocalDateTime = resultSet.getTimestamp("timestamp").toLocalDateTime()
                            val interactionType: String = resultSet.getString("interactionType")
                            val itemStack: ItemStack = getItemStackFromJsonString(itemString, server.registryManager) ?: continue
                            val inventoryInteractionLog: InventoryInteractionLog = when (interactionType) {
                                InventoryInteractionType.ITEM_REMOVE.asString() -> ItemRemoveLog(playerName, uuid, itemStack, timeStamp)
                                InventoryInteractionType.ITEM_INSERT.asString() -> ItemInsertLog(playerName, uuid, itemStack, timeStamp)
                                else -> continue
                            }
                            results.add(inventoryInteractionLog)
                        }

                    }

                }

            }
        } catch (e: Exception) {
            PublicEnderChest.LOGGER.error("Error attempting to query from Public Ender Chest database: $e")
        }
        return results.toList()
    }

    fun purge(server: MinecraftServer, daysBefore: Int) : Int {
        var deletedRows: Int
        val url: String = getDatabaseFileUrl(server)
        DriverManager.getConnection(url).use { connection ->
            connection.prepareStatement("DELETE FROM $MAIN_TABLE_NAME WHERE timestamp < ?").use { statement ->
                val timestamp: Timestamp = Timestamp.valueOf(LocalDateTime.now().minusDays(daysBefore.toLong()))
                statement.setTimestamp(1, timestamp)
                deletedRows = statement.executeUpdate()
            }
        }
        PublicEnderChest.LOGGER.info("Purged $deletedRows entries from Public Ender Chest inventory log database")
        return deletedRows
    }

    companion object {

        const val MAIN_TABLE_NAME = "public_inventory_interactions"
        private const val DATABASE_FILE_NAME = "${PublicEnderChest.MOD_ID}.db"

        private fun getDatabaseFileUrl(server: MinecraftServer) : String =
            "jdbc:sqlite:${getModFolderPath(server).resolve(DATABASE_FILE_NAME)}"

    }

}

fun ItemStack.getJsonString(registries: RegistryWrapper.WrapperLookup) : String? {
    val encodedStack: DataResult<JsonElement> = ItemStack.CODEC.encodeStart(registries.getOps(JsonOps.COMPRESSED), this)
    val jsonOptional: Optional<JsonElement> = encodedStack.resultOrPartial { e ->
        PublicEnderChest.LOGGER.error("Error attempting to log Item stack data: $e")
    }
    if (jsonOptional.isEmpty) {
        return null
    }
    val jsonElement: JsonElement = jsonOptional.get()
    val gson = Gson()
    return gson.toJson(jsonElement)
}

fun getItemStackFromJsonString(jsonString: String, registries: RegistryWrapper.WrapperLookup) : ItemStack? {
    try {
        val jsonElement: JsonElement = JsonParser.parseString(jsonString)
        val decodedStack: DataResult<ItemStack> = ItemStack.CODEC.parse(registries.getOps(JsonOps.COMPRESSED), jsonElement)
        val itemStackOptional: Optional<ItemStack> = decodedStack.resultOrPartial { e ->
            PublicEnderChest.LOGGER.error("Error attempting to read Item stack from database: $e")
        }
        if (itemStackOptional.isEmpty) {
            return null
        }
        return itemStackOptional.get()
    } catch (e: Exception) {
        PublicEnderChest.LOGGER.error("Error attempting to read Item stack from database: $e")
        return null
    }

}

fun ItemStack.getIdentifier() : Identifier {
    val itemStackRegistryKey: RegistryKey<Item>? = Registries.ITEM.getKey(this.item).orElse(null)
    val itemStackId: Identifier = if (itemStackRegistryKey == null) {
        PublicEnderChest.LOGGER.error("Error logging Public Ender Chest interaction: Unknown Item \"$item\"")
        Identifier.ofVanilla("unknown_item_identifier")
    } else {
        itemStackRegistryKey.value
    }
    return itemStackId
}


