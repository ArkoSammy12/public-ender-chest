package io.github.arkosammy12.publicenderchest.mixin.client;


import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.arkosammy12.publicenderchest.PublicEnderChestKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import io.github.arkosammy12.publicenderchest.PublicEnderChestClient;

@Mixin(ClientPlayerInteractionManager.class)
abstract public class ClientPlayerInteractionManagerMixin {

    @WrapMethod(method = "clickSlot")
    private void onSlotClicked(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, Operation<Void> original) {
        if (slotId < 0 || slotId >= player.currentScreenHandler.slots.size()) {
            original.call(syncId, slotId, button, actionType, player);
            return;
        }
        if (!areInteractionKeysActive()) {
            original.call(syncId, slotId, button, actionType, player);
            return;
        }
        if (actionType != SlotActionType.PICKUP) {
            original.call(syncId, slotId, button, actionType, player);
            return;
        }
        Slot selectedSlot = player.currentScreenHandler.getSlot(slotId);
        if (selectedSlot == null || !(selectedSlot.inventory instanceof PlayerInventory)) {
            original.call(syncId, slotId, button, actionType, player);
            return;
        }
        if (!player.currentScreenHandler.getCursorStack().isEmpty()) {
            original.call(syncId, slotId, button, actionType, player);
            return;
        }
        ItemStack selectedStack = selectedSlot.getStack();
        if (!PublicEnderChestKt.isEnderChest(selectedStack)) {
            original.call(syncId, slotId, button, actionType, player);
            return;
        }
        PublicEnderChestClient.sendOpenPublicInventoryPayload();
    }

    @Unique
    private static boolean isPressed(int keyCode, long handle) {
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }

    @Unique
    private static boolean areInteractionKeysActive() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return isPressed(GLFW.GLFW_KEY_LEFT_CONTROL, handle) || isPressed(GLFW.GLFW_KEY_RIGHT_CONTROL, handle) || isPressed(GLFW.GLFW_KEY_LEFT_ALT, handle) || isPressed(GLFW.GLFW_KEY_RIGHT_ALT, handle);
    }

}
