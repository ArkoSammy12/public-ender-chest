package io.github.arkosammy12.publicenderchest.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import io.github.arkosammy12.publicenderchest.util.ducks.ItemStackDuck;

import java.util.ArrayList;
import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @ModifyReturnValue(method = "getTooltipFromItem", at = @At("RETURN"))
    private List<Text> addCustomInfoLines(List<Text> original, ItemStack stack) {
        List<Text> originalTooltipLines = new ArrayList<>(original);
        List<Text> customInfoLines = ((ItemStackDuck) (Object) stack).publicenderchest$getCustomInfoLines();
        originalTooltipLines.addAll(customInfoLines);
        return originalTooltipLines;
    }

}
