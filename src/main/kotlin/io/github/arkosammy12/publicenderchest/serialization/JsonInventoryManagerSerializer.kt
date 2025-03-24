package io.github.arkosammy12.publicenderchest.serialization

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.JsonOps
import net.minecraft.server.MinecraftServer
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import io.github.arkosammy12.publicenderchest.getModFolderPath
import io.github.arkosammy12.publicenderchest.inventory.InventoryManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class JsonInventoryManagerSerializer<T : InventoryManager>(override val inventoryManager: T) : InventoryManagerSerializer<T> {

    override fun writeManager(codec: Codec<in T>, fileName: String, server: MinecraftServer, logWrite: Boolean) {
        val filePath: Path = getModFolderPath(server).resolve(getPathNameForFile(fileName))
        val encodedManager: DataResult<JsonElement> = codec.encodeStart(server.registryManager.getOps(JsonOps.COMPRESSED), inventoryManager)
        val encodedJsonOptional: Optional<JsonElement> = encodedManager.resultOrPartial { e ->
            PublicEnderChest.LOGGER.error("Error attempting to serialize Public Ender Chest inventory: $e")
        }
        if (encodedJsonOptional.isEmpty) {
            PublicEnderChest.LOGGER.error("Error attempting to serialize Public Ender Chest inventory: Empty JsonElement value!")
            return
        }
        val encodedJson: JsonElement = encodedJsonOptional.get()
        try {
            val gson = Gson()
            val jsonString: String = gson.toJson(encodedJson)
            Files.newBufferedWriter(filePath).use { bw ->
                bw.write(jsonString)
                if (logWrite) {
                    PublicEnderChest.LOGGER.info("Stored inventory manager to: $filePath")
                }
            }
        } catch (e: Exception) {
            PublicEnderChest.LOGGER.error("Error attempting to serialize Public Ender Chest inventory: $e")
        }
    }

    override fun readManager(codec: Codec<out T>, fileName: String, server: MinecraftServer): T? {
        val filePath: Path = getModFolderPath(server).resolve(getPathNameForFile(fileName))
        try {
            if (!Files.exists(filePath)) {
                PublicEnderChest.LOGGER.warn("Public ender chest file not found! Creating new one at $filePath")
                Files.createFile(filePath)
                return null
            }
            Files.newBufferedReader(filePath).use { br ->
                val jsonElement: JsonElement = JsonParser.parseReader(br)
                val decodedManager: DataResult<out T> = codec.parse(server.registryManager.getOps(JsonOps.COMPRESSED), jsonElement)
                val optionalManager: Optional<out T> = decodedManager.resultOrPartial { e -> PublicEnderChest.LOGGER.error("Error reading public inventory manager file $filePath : $e") }
                return optionalManager.get()
            }
        } catch (e: Exception) {
            PublicEnderChest.LOGGER.error("Error reading public inventory manager file $filePath : $e")
            return null
        }
    }

    companion object {

        fun getPathNameForFile(fileName: String) : String = "$fileName.json"

    }

}