package io.github.arkosammy12.publicenderchest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import io.github.arkosammy12.publicenderchest.inventory.CustomGenericContainerScreenHandler;
import io.github.arkosammy12.publicenderchest.inventory.PublicInventory;
import java.util.function.Supplier;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @ModifyExpressionValue(method = "checkSlotUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/sync/TrackedSlot;isInSync(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean forceSlotUpdateForPublicInventory(boolean original, int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
        if (stack.isEmpty()) {
            return original;
        }
        if (!(((ScreenHandler) (Object) this) instanceof CustomGenericContainerScreenHandler screenHandler)) {
            return original;
        }
        Inventory inventory = screenHandler.getInventory();
        if (!(inventory instanceof PublicInventory)) {
            return original;
        }
        // Expression value is inverted by a not operator
        return false;
    }

}
