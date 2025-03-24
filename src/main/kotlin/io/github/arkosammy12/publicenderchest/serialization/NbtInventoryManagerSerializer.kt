package io.github.arkosammy12.publicenderchest.serialization

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.server.MinecraftServer
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import io.github.arkosammy12.publicenderchest.getModFolderPath
import io.github.arkosammy12.publicenderchest.inventory.InventoryManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class NbtInventoryManagerSerializer<T : InventoryManager>(override val inventoryManager: T) : InventoryManagerSerializer<T> {

    override fun writeManager(codec: Codec<in T>, fileName: String, server: MinecraftServer, logWrite: Boolean) {
        val filePath: Path = getModFolderPath(server).resolve(getPathNameForFile(fileName))
        val encodedManager: DataResult<NbtElement> = codec.encodeStart(server.registryManager.getOps(NbtOps.INSTANCE), inventoryManager)
        val encodedNbtOptional: Optional<NbtElement> = encodedManager.resultOrPartial { e ->
            PublicEnderChest.LOGGER.error("Error attempting to serialize Public Ender Chest inventory: $e")
        }
        if (encodedNbtOptional.isEmpty) {
            PublicEnderChest.LOGGER.error("Error attempting to serialize Public Ender Chest inventory: Empty NbtElement value!")
            return
        }
        val encodedNbt: NbtElement = encodedNbtOptional.get()
        if (encodedNbt !is NbtCompound) {
            PublicEnderChest.LOGGER.error("Error attempting to serialize Public Ender Chest inventory: Encoded nbt is not an NbtCompound!")
            return
        }
        try {
            NbtIo.writeCompressed(encodedNbt, filePath)
            if (logWrite) {
                PublicEnderChest.LOGGER.info("Stored inventory manager to: $filePath")
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
            val nbtCompound: NbtCompound = NbtIo.readCompressed(filePath, NbtSizeTracker.ofUnlimitedBytes())
            val decodedManager: DataResult<out T> = codec.parse(server.registryManager.getOps(NbtOps.INSTANCE), nbtCompound)
            val optionalManager: Optional<out T> = decodedManager.resultOrPartial { e -> PublicEnderChest.LOGGER.error("Error reading public inventory manager file $filePath : $e") }
            return optionalManager.get()
        } catch (e: Exception) {
            PublicEnderChest.LOGGER.error("Error reading public inventory manager file $filePath : $e")
            return null
        }
    }

    companion object {

        fun getPathNameForFile(fileName: String) : String = "$fileName.nbt"

    }

}