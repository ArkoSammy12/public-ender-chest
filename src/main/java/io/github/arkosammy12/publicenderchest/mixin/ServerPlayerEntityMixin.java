package io.github.arkosammy12.publicenderchest.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import io.github.arkosammy12.publicenderchest.PublicEnderChest;
import io.github.arkosammy12.publicenderchest.inventory.CustomGenericContainerScreenHandler;
import io.github.arkosammy12.publicenderchest.logging.InventoryInteractionLog;
import io.github.arkosammy12.publicenderchest.logging.QueryContext;
import io.github.arkosammy12.publicenderchest.util.ducks.ServerPlayerEntityDuck;

import java.util.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ServerPlayerEntityDuck {

    @Shadow public abstract OptionalInt openHandledScreen(@Nullable NamedScreenHandlerFactory factory);

    @Shadow @Final public MinecraftServer server;

    @Shadow public abstract void sendMessage(Text message);

    @Unique
    private final List<List<InventoryInteractionLog>> cachedLogs = new ArrayList<>();

    @Unique
    private int pageIndex = 0;

    @Unique
    private boolean hasMod = false;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Override
    public void publicenderchest$setHasMod(boolean hasMod) {
        this.hasMod = hasMod;
    }

    @Override
    public boolean publicenderchest$hasMod() {
        return this.hasMod;
    }

    @Override
    public void publicenderchest$setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    @Override
    public List<List<InventoryInteractionLog>> publicenderchest$getCachedLogs() {
        return this.cachedLogs;
    }

    @Override
    public void publicenderchest$showLogs(QueryContext queryContext) {
        MinecraftServer server = this.getServer();
        if (server == null) {
            return;
        }
        List<InventoryInteractionLog> results = PublicEnderChest.INSTANCE.getDATABASE_MANAGER().query(server, queryContext);
        if (results.isEmpty()) {
            this.sendMessage(Text.literal("No logs found for provided query!").formatted(Formatting.RED));
            return;
        }
        results.sort(Comparator.comparing(InventoryInteractionLog::getTimestamp).reversed());
        cachedLogs.clear();
        this.pageIndex = 0;

        // Paginate logs
        for (int i = 0; i < results.size(); i++) {
            if (i % 10 == 0) {
                List<InventoryInteractionLog> page = new ArrayList<>();
                cachedLogs.add(page);
            }
            cachedLogs.getLast().add(results.get(i));
        }
        this.publicenderchest$showPage();

    }

    @Override
    public void publicenderchest$showPage() {
        MutableText headerText = Text.literal("-- Showing logs for Public Ender Chest Inventory --")
                .formatted(Formatting.DARK_AQUA);
        MutableText logLines = Text.empty();

        Iterator<InventoryInteractionLog> logIterator = this.cachedLogs.get(this.pageIndex).iterator();
        while (logIterator.hasNext()) {
            InventoryInteractionLog inventoryInteractionLog = logIterator.next();
            MutableText logLineText = inventoryInteractionLog.getLogText();
            logLines.append((logIterator.hasNext() ? logLineText.append("\n") : logLineText));
        }

        MutableText footerPrefix = Text.literal("--- ")
                .formatted(Formatting.DARK_AQUA);
        MutableText footerPreviousPage = Text.literal("<< ")
                .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(String.format("/%s database query page %d", PublicEnderChest.MOD_ID, this.pageIndex))).withHoverEvent(new HoverEvent.ShowText(Text.literal("Turn to previous page"))))
                .formatted(Formatting.BLUE);
        MutableText footerMiddle = Text.literal(String.format("Showing page [%d of %d] ", pageIndex + 1, this.cachedLogs.size()))
                .formatted(Formatting.AQUA);
        MutableText footerNextPage = Text.literal(">> ")
                .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(String.format("/%s database query page %d", PublicEnderChest.MOD_ID, this.pageIndex + 2))).withHoverEvent(new HoverEvent.ShowText(Text.literal("Turn to next page"))))
                .formatted(Formatting.BLUE);
        MutableText footerSuffix = Text.literal("---")
                .formatted(Formatting.DARK_AQUA);

        MutableText footerText = Text.empty().append(footerPrefix).append(footerPreviousPage).append(footerMiddle).append(footerNextPage).append(footerSuffix);

        this.sendMessage(headerText);
        this.sendMessage(logLines);
        this.sendMessage(footerText);
    }


    @Override
    public void publicenderchest$openInventory(String name, Inventory inventory) {
        NamedScreenHandlerFactory screenHandlerFactory = new SimpleNamedScreenHandlerFactory((syncId, playerInventory, player) -> new CustomGenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6), Text.literal(name));
        this.openHandledScreen(screenHandlerFactory);
    }
}
