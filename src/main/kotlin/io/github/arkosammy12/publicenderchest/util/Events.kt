package io.github.arkosammy12.publicenderchest.util

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import io.github.arkosammy12.publicenderchest.commands.CommandManager
import io.github.arkosammy12.publicenderchest.networking.C2SHandshakeResponsePayload
import io.github.arkosammy12.publicenderchest.networking.OpenPublicInventoryPayload
import io.github.arkosammy12.publicenderchest.networking.S2CHandshakeRequestPayload
import io.github.arkosammy12.publicenderchest.util.ducks.ServerPlayerEntityDuck
import java.util.UUID

object Events {

    fun registerEvents() {
        CommandRegistrationCallback.EVENT.register(CommandManager::registerCommands)
        UseBlockCallback.EVENT.register(::onBlockInteracted)
        UseItemCallback.EVENT.register(::onItemInteracted)
        ServerTickEvents.END_SERVER_TICK.register(::onServerTick)
        PayloadTypeRegistry.playS2C().register(S2CHandshakeRequestPayload.PACKET_ID, S2CHandshakeRequestPayload.PACKET_CODEC)
        PayloadTypeRegistry.playC2S().register(C2SHandshakeResponsePayload.PACKET_ID, C2SHandshakeResponsePayload.PACKET_CODEC)
        PayloadTypeRegistry.playC2S().register(OpenPublicInventoryPayload.PACKET_ID, OpenPublicInventoryPayload.PACKET_CODEC)
        ServerPlayNetworking.registerGlobalReceiver(OpenPublicInventoryPayload.PACKET_ID, ::handleOpenInventoryPayload)
        ServerPlayNetworking.registerGlobalReceiver(C2SHandshakeResponsePayload.PACKET_ID, ::handleHandshakeResponse)
        ServerPlayConnectionEvents.JOIN.register(::onPlayerJoin)
    }

    private fun onServerTick(server: MinecraftServer) {
        PublicEnderChest.INVENTORY_MANAGER.tick(server)
    }

    private fun onBlockInteracted(player: PlayerEntity, world: World, hand: Hand, hitResult: BlockHitResult) : ActionResult {
        return PublicEnderChest.INVENTORY_MANAGER.onBlockInteractedListener(player, world, hand, hitResult)
    }

    private fun onItemInteracted(player: PlayerEntity, world: World, hand: Hand): ActionResult {
        return PublicEnderChest.INVENTORY_MANAGER.onItemInteractedListener(player, world, hand)
    }

    private fun <T : CustomPayload> handleOpenInventoryPayload(payload: T, context: Context) {
        val player: ServerPlayerEntity = context.player()
        val isUsingPublicInventory: Boolean = player.getAttached(PublicEnderChest.USING_PUBLIC_INVENTORY) ?: true
        if (!isUsingPublicInventory) {
            return
        }
        if (!PublicEnderChest.INVENTORY_MANAGER.publicInventory.canPlayerUse(player)) {
            return
        }
        (player as ServerPlayerEntityDuck).`publicenderchest$openInventory`(PublicEnderChest.PUBLIC_INVENTORY_NAME, PublicEnderChest.INVENTORY_MANAGER.publicInventory)
    }

    private fun <T : CustomPayload> handleHandshakeResponse(payload: T, context: Context) {
        val player: ServerPlayerEntity = context.player()
        (player as ServerPlayerEntityDuck).`publicenderchest$setHasMod`(true)
    }

    private fun onPlayerJoin(handler: ServerPlayNetworkHandler, sender: PacketSender, server: MinecraftServer) {
        if (ServerPlayNetworking.canSend(handler, S2CHandshakeRequestPayload.PACKET_ID)) {
            handler.sendPacket(ServerPlayNetworking.createS2CPacket(S2CHandshakeRequestPayload(UUID.randomUUID())))
        }
    }

}