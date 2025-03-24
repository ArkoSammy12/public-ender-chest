package io.github.arkosammy12.publicenderchest.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.command.CommandSource
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import io.github.arkosammy12.monkeyconfig.managers.getRawNumberSettingValue
import io.github.arkosammy12.publicenderchest.PublicEnderChest
import io.github.arkosammy12.publicenderchest.config.ConfigUtils
import io.github.arkosammy12.publicenderchest.logging.QueryContext
import io.github.arkosammy12.publicenderchest.logging.TimeQueryType
import io.github.arkosammy12.publicenderchest.util.ducks.ServerPlayerEntityDuck

object DatabaseCommands {

    fun registerCommands(rootNode: CommandNode<ServerCommandSource>) {

        val databaseNode: LiteralCommandNode<ServerCommandSource> = CommandManager
            .literal("database")
            .requires { src -> src.hasPermissionLevel(4) }
            .build()

        val purgeNode: LiteralCommandNode<ServerCommandSource> = CommandManager
            .literal("purge")
            .requires { src -> src.hasPermissionLevel(4) }
            .executes { ctx ->
                val player: ServerPlayerEntity? = ctx.source.player
                sendMessageToPlayerOrConsole(player, Text.literal("Initiating purge...").formatted(Formatting.GRAY), LogType.NORMAL)
                val deletedRows: Int = PublicEnderChest.DATABASE_MANAGER.purge(ctx.source.server, PublicEnderChest.CONFIG_MANAGER.getRawNumberSettingValue<Int>(ConfigUtils.PURGE_OLDER_THAN_X_DAYS) ?: 30)
                val deletedRowsText: MutableText = Text.literal("$deletedRows").formatted(Formatting.AQUA)
                sendMessageToPlayerOrConsole(player, Text.empty().append(Text.literal("Purged ")).append(deletedRowsText).append(Text.literal(" entries from the Public Ender Chest inventory database.")), LogType.NORMAL)
                Command.SINGLE_SUCCESS
            }
            .build()

        val purgeDaysArgumentNode: ArgumentCommandNode<ServerCommandSource, Int> = CommandManager
            .argument("entriesOlderThanDays", IntegerArgumentType.integer(1))
            .requires { src -> src.hasPermissionLevel(4) }
            .executes { ctx ->
                val player: ServerPlayerEntity? = ctx.source.player
                val olderThanDaysValue: Int = IntegerArgumentType.getInteger(ctx, "entriesOlderThanDays")
                sendMessageToPlayerOrConsole(player, Text.literal("Initiating purge...").formatted(Formatting.GRAY), LogType.NORMAL)
                val deletedRows: Int = PublicEnderChest.DATABASE_MANAGER.purge(ctx.source.server, olderThanDaysValue)
                val deletedRowsText: MutableText = Text.literal("$deletedRows").formatted(Formatting.AQUA)
                sendMessageToPlayerOrConsole(player, Text.empty().append(Text.literal("Purged ")).append(deletedRowsText).append(Text.literal(" entries from the Public Ender Chest inventory database.")), LogType.NORMAL)
                Command.SINGLE_SUCCESS
            }
            .build()

        val pageNode: LiteralCommandNode<ServerCommandSource> = CommandManager
            .literal("page")
            .requires { src -> src.hasPermissionLevel(4) }
            .build()

        val pageArgumentNode: ArgumentCommandNode<ServerCommandSource, Int> = CommandManager
            .argument("page", IntegerArgumentType.integer(0))
            .requires { src -> src.hasPermissionLevel(4) }
            .executes { ctx ->
                val player: ServerPlayerEntity = ctx.source.playerOrThrow
                val newPageIndex: Int = IntegerArgumentType.getInteger(ctx, "page") - 1
                if (newPageIndex < 0 || newPageIndex > (player as ServerPlayerEntityDuck).`publicenderchest$getCachedLogs`().size - 1) {
                    player.sendMessage(Text.literal("No more pages to show!").formatted(Formatting.RED))
                    return@executes Command.SINGLE_SUCCESS
                }
                (player as ServerPlayerEntityDuck).`publicenderchest$setPageIndex`(newPageIndex)
                (player as ServerPlayerEntityDuck).`publicenderchest$showPage`()
                Command.SINGLE_SUCCESS
            }
            .build()

        val queryNode: LiteralCommandNode<ServerCommandSource> = CommandManager
            .literal("query")
            .requires { src -> src.hasPermissionLevel(4) }
            .build()

        val timeQueryTypeNode: ArgumentCommandNode<ServerCommandSource, String> = CommandManager
            .argument("timeQueryType", StringArgumentType.word())
            .suggests { _, suggestionBuilder ->
                CommandSource.suggestMatching(
                    TimeQueryType.entries.map { type -> type.commandNodeName },
                    suggestionBuilder
                )
            }
            .requires { src -> src.hasPermissionLevel(4) }
            .build()

        val queryWithDays: ArgumentCommandNode<ServerCommandSource, Int> = CommandManager
            .argument("days", IntegerArgumentType.integer(0))
            .requires { src -> src.hasPermissionLevel(4) }
            .executes { ctx ->
                val player: ServerPlayerEntity = ctx.source.playerOrThrow
                val queryTypeName: String = StringArgumentType.getString(ctx, "timeQueryType")
                val queryType: TimeQueryType = TimeQueryType.getFromCommandNodeName(queryTypeName) ?: return@executes Command.SINGLE_SUCCESS.also {
                    player.sendMessage(Text.literal("Time query type \"$queryTypeName\" does not exist!").formatted(Formatting.RED))
                }
                val days: Int = IntegerArgumentType.getInteger(ctx, "days")
                val queryContext = QueryContext(queryType, days)
                (player as ServerPlayerEntityDuck).`publicenderchest$showLogs`(queryContext)
                Command.SINGLE_SUCCESS
            }
            .build()

        val queryWithHours: ArgumentCommandNode<ServerCommandSource, Int> = CommandManager
            .argument("hours", IntegerArgumentType.integer(0))
            .requires { src -> src.hasPermissionLevel(4) }
            .executes { ctx ->
                val player: ServerPlayerEntity = ctx.source.playerOrThrow
                val queryTypeName: String = StringArgumentType.getString(ctx, "timeQueryType")
                val queryType: TimeQueryType = TimeQueryType.getFromCommandNodeName(queryTypeName) ?: return@executes Command.SINGLE_SUCCESS.also {
                    player.sendMessage(Text.literal("Time query type \"$queryTypeName\" does not exist!").formatted(Formatting.RED))
                }
                val days: Int = IntegerArgumentType.getInteger(ctx, "days")
                val hours: Int = IntegerArgumentType.getInteger(ctx, "hours")
                val queryContext = QueryContext(queryType, days, hours)
                (player as ServerPlayerEntityDuck).`publicenderchest$showLogs`(queryContext)
                Command.SINGLE_SUCCESS
            }
            .build()

        val queryWithMinutes: ArgumentCommandNode<ServerCommandSource, Int> = CommandManager
            .argument("minutes", IntegerArgumentType.integer(0))
            .requires { src -> src.hasPermissionLevel(4) }
            .executes { ctx ->
                val player: ServerPlayerEntity = ctx.source.playerOrThrow
                val queryTypeName: String = StringArgumentType.getString(ctx, "timeQueryType")
                val queryType: TimeQueryType = TimeQueryType.getFromCommandNodeName(queryTypeName) ?: return@executes Command.SINGLE_SUCCESS.also {
                    player.sendMessage(Text.literal("Time query type \"$queryTypeName\" does not exist!").formatted(Formatting.RED))
                }
                val days: Int = IntegerArgumentType.getInteger(ctx, "days")
                val hours: Int = IntegerArgumentType.getInteger(ctx, "hours")
                val minutes: Int = IntegerArgumentType.getInteger(ctx, "minutes")
                val queryContext = QueryContext(queryType, days, hours, minutes)
                (player as ServerPlayerEntityDuck).`publicenderchest$showLogs`(queryContext)
                Command.SINGLE_SUCCESS
            }
            .build()

        val queryWithSeconds: ArgumentCommandNode<ServerCommandSource, Int> = CommandManager
            .argument("seconds", IntegerArgumentType.integer(0))
            .requires { src -> src.hasPermissionLevel(4) }
            .executes { ctx ->
                val player: ServerPlayerEntity = ctx.source.playerOrThrow
                val queryTypeName: String = StringArgumentType.getString(ctx, "timeQueryType")
                val queryType: TimeQueryType = TimeQueryType.getFromCommandNodeName(queryTypeName) ?: return@executes Command.SINGLE_SUCCESS.also {
                    player.sendMessage(Text.literal("Time query type \"$queryTypeName\" does not exist!").formatted(Formatting.RED))
                }
                val days: Int = IntegerArgumentType.getInteger(ctx, "days")
                val hours: Int = IntegerArgumentType.getInteger(ctx, "hours")
                val minutes: Int = IntegerArgumentType.getInteger(ctx, "minutes")
                val seconds: Int = IntegerArgumentType.getInteger(ctx, "seconds")
                val queryContext = QueryContext(queryType, days, hours, minutes, seconds)
                (player as ServerPlayerEntityDuck).`publicenderchest$showLogs`(queryContext)
                Command.SINGLE_SUCCESS
            }
            .build()

        val playerNameArgumentNode: ArgumentCommandNode<ServerCommandSource, String> = CommandManager
            .argument("playerName", StringArgumentType.word())
            .requires { src -> src.hasPermissionLevel(4) }
            .suggests { context, builder ->
                CommandSource.suggestMatching(
                    context.source.server.playerNames,
                    builder
                )
            }
            .executes { ctx ->
                val playerName: String = StringArgumentType.getString(ctx, "playerName")
                val player: ServerPlayerEntity = ctx.source.playerOrThrow
                val queryTypeName: String = StringArgumentType.getString(ctx, "timeQueryType")
                val queryType: TimeQueryType = TimeQueryType.getFromCommandNodeName(queryTypeName) ?: return@executes Command.SINGLE_SUCCESS.also {
                    player.sendMessage(Text.literal("Time query type \"$queryTypeName\" does not exist!").formatted(Formatting.RED))
                }
                val days: Int = IntegerArgumentType.getInteger(ctx, "days")
                val hours: Int = IntegerArgumentType.getInteger(ctx, "hours")
                val minutes: Int = IntegerArgumentType.getInteger(ctx, "minutes")
                val seconds: Int = IntegerArgumentType.getInteger(ctx, "seconds")
                val queryContext = QueryContext(queryType, days, hours, minutes, seconds, playerName)
                (player as ServerPlayerEntityDuck).`publicenderchest$showLogs`(queryContext)
                Command.SINGLE_SUCCESS
            }
            .build()

        rootNode.addChild(databaseNode)

        databaseNode.addChild(purgeNode)
        purgeNode.addChild(purgeDaysArgumentNode)

        databaseNode.addChild(queryNode)

        queryNode.addChild(pageNode)
        pageNode.addChild(pageArgumentNode)

        queryNode.addChild(timeQueryTypeNode)
        timeQueryTypeNode.addChild(queryWithDays)
        queryWithDays.addChild(queryWithHours)
        queryWithHours.addChild(queryWithMinutes)
        queryWithMinutes.addChild(queryWithSeconds)

        queryWithSeconds.addChild(playerNameArgumentNode)

    }

    private fun sendMessageToPlayerOrConsole(player: ServerPlayerEntity?, text: MutableText, logType: LogType) {
        if (player != null) {
            player.sendMessage(text)
        } else {
            when (logType) {
                LogType.NORMAL -> PublicEnderChest.LOGGER.info(text.string)
                LogType.WARNING -> PublicEnderChest.LOGGER.warn(text.string)
                LogType.ERROR -> PublicEnderChest.LOGGER.error(text.string)
            }
        }
    }

    private enum class LogType {
        NORMAL,
        WARNING,
        ERROR
    }

}