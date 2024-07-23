package xd.arkosammy.publicenderchest.inventory

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World

interface InventoryManager {

    fun tick(server: MinecraftServer)

    fun onServerStarting(server: MinecraftServer)

    fun onServerStopping(server: MinecraftServer)

    fun onBlockInteracted(player: PlayerEntity, world: World, hand: Hand, hitResult: BlockHitResult) : ActionResult

    fun onItemInteracted(player: PlayerEntity, world: World, hand: Hand): TypedActionResult<ItemStack>

}