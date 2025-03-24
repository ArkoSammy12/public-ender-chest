package io.github.arkosammy12.publicenderchest.serialization

import com.mojang.serialization.Codec
import net.minecraft.server.MinecraftServer
import io.github.arkosammy12.publicenderchest.inventory.InventoryManager

interface InventoryManagerSerializer<T : InventoryManager> {

    val inventoryManager: InventoryManager

    fun writeManager(codec: Codec<in T>, fileName: String, server: MinecraftServer, logWrite: Boolean = false)

    fun readManager(codec: Codec<out T>, fileName: String, server: MinecraftServer) : T?

}