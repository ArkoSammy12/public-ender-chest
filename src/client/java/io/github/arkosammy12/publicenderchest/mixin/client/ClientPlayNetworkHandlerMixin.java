package io.github.arkosammy12.publicenderchest.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.arkosammy12.publicenderchest.PublicEnderChestKt;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import io.github.arkosammy12.publicenderchest.util.ducks.ItemStackDuck;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @WrapOperation(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ScreenHandlerSlotUpdateS2CPacket;getStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack restoreOriginalItemStackAndSaveCustomInfo(ScreenHandlerSlotUpdateS2CPacket instance, Operation<ItemStack> original) {
        ItemStack itemStack = original.call(instance);
        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return itemStack;
        }
        String expectedMetadata = PublicEnderChestKt.formatInfoTextMetadata(instance.getSyncId(), instance.getRevision());
        List<Text> originalLines = new ArrayList<>(loreComponent.lines());
        List<Text> originalStyledLines = new ArrayList<>(loreComponent.styledLines());
        ItemStack actualItemStack = itemStack.copy();
        Predicate<Text> lineMetadataFilter = (line) -> {
            boolean containsMetadata = line.getString().contains(expectedMetadata);
            if (containsMetadata) {
                Text customInfoLine = Text.literal(line.getString().replace(expectedMetadata, "")).formatted(Formatting.ITALIC, Formatting.DARK_PURPLE);
                List<Text> currentCustomInfoLines = ((ItemStackDuck) (Object) actualItemStack).publicenderchest$getCustomInfoLines();
                if (!currentCustomInfoLines.contains(customInfoLine)) {
                    ((ItemStackDuck) (Object) actualItemStack).publicenderchest$addCustomInfoLine(customInfoLine);
                }
                return false;
            } else {
                return true;
            }
        };
        List<Text> actualLines = originalLines.stream().filter(lineMetadataFilter).toList();
        List<Text> actualStyledLines = originalStyledLines.stream().filter(lineMetadataFilter).toList();
        LoreComponent actualLoreComponent = new LoreComponent(actualLines, actualStyledLines);
        actualItemStack.set(DataComponentTypes.LORE, actualLoreComponent);
        return actualItemStack;
    }

}
