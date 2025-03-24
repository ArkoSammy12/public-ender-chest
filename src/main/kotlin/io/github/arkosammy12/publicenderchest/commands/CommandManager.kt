package io.github.arkosammy12.publicenderchest.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import io.github.arkosammy12.publicenderchest.util.ducks.ServerPlayerEntityDuck

object CommandManager {

    fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>, registryAccess: CommandRegistryAccess, environment: CommandManager.RegistrationEnvironment) {

        val parentNode: CommandNode<ServerCommandSource> = dispatcher.root.getChild(PublicEnderChest.MOD_ID) ?: run {
            val node: LiteralCommandNode<ServerCommandSource> = CommandManager
                .literal(PublicEnderChest.MOD_ID)
                .build()
            dispatcher.root.addChild(node)
            node
        }

        val setTogglePublicInventoryUseNode: LiteralCommandNode<ServerCommandSource> = CommandManager
            .literal("usePublicInventory")
            .executes { ctx ->
                val player: ServerPlayerEntity = ctx.source.player ?: return@executes Command.SINGLE_SUCCESS
                val isUsingPublicInventory: Boolean = player.getAttached(PublicEnderChest.USING_PUBLIC_INVENTORY) ?: return@executes Command.SINGLE_SUCCESS
                val feedBackText: MutableText = if (isUsingPublicInventory) Text.literal("enabled").formatted(Formatting.GREEN) else Text.literal("disabled").formatted(Formatting.RED)
                player.sendMessage(Text.literal("Usage of public ender chest inventory is currently ").append(feedBackText))
                Command.SINGLE_SUCCESS
            }
            .build()

        val togglePublicInventoryUseNodeArgumentNode: ArgumentCommandNode<ServerCommandSource, Boolean> = CommandManager
            .argument("toggle", BoolArgumentType.bool())
            .executes { ctx ->
                val player: ServerPlayerEntity = ctx.source.player ?: return@executes Command.SINGLE_SUCCESS
                val usePublicInventory: Boolean = BoolArgumentType.getBool(ctx, "toggle")
                player.setAttached(PublicEnderChest.USING_PUBLIC_INVENTORY, usePublicInventory)
                val feedBackText: MutableText = if (usePublicInventory) Text.literal("enabled").formatted(Formatting.GREEN) else Text.literal("disabled").formatted(Formatting.RED)
                player.sendMessage(Text.literal("Usage of public ender chest inventory has been ").append(feedBackText))
                Command.SINGLE_SUCCESS
            }
            .build()

        val openPublicInventoryNode: LiteralCommandNode<ServerCommandSource> = CommandManager
            .literal("openPublicEnderChestInventory")
            .requires { src -> src.hasPermissionLevel(4) }
            .executes { ctx ->
                val player: ServerPlayerEntity = ctx.source.player ?: return@executes Command.SINGLE_SUCCESS
                (player as ServerPlayerEntityDuck).`publicenderchest$openInventory`(PublicEnderChest.PUBLIC_INVENTORY_NAME, PublicEnderChest.INVENTORY_MANAGER.publicInventory)
                Command.SINGLE_SUCCESS
            }
            .build()

        parentNode.addChild(setTogglePublicInventoryUseNode)
        setTogglePublicInventoryUseNode.addChild(togglePublicInventoryUseNodeArgumentNode)
        parentNode.addChild(openPublicInventoryNode)
        DatabaseCommands.registerCommands(parentNode)

    }

}