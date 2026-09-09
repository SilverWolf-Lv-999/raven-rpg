package keystrokesmod.mixin.impl.client;

import keystrokesmod.module.impl.client.RPGUI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerMerchant.class)
public class MixinContainerMerchant {
    @Inject(method = "transferStackInSlot", at = @At("HEAD"), cancellable = true)
    private void raven$quickMoveToMerchantInput(EntityPlayer player, int index, CallbackInfoReturnable<ItemStack> callbackInfo) {
        if (!RPGUI.shouldStyleMerchant() || index < 3 || index >= 39) {
            return;
        }
        ContainerMerchant containerMerchant = (ContainerMerchant) (Object) this;
        Slot sourceSlot = containerMerchant.inventorySlots.get(index);
        if (sourceSlot == null || !sourceSlot.getHasStack()) {
            callbackInfo.setReturnValue(null);
            return;
        }
        ItemStack sourceStack = sourceSlot.getStack();
        ItemStack originalStack = sourceStack.copy();
        if (!containerMerchant.mergeItemStack(sourceStack, 0, 2, false)) {
            callbackInfo.setReturnValue(null);
            return;
        }
        if (sourceStack.stackSize == 0) {
            sourceSlot.putStack(null);
        } else {
            sourceSlot.onSlotChanged();
        }
        if (sourceStack.stackSize == originalStack.stackSize) {
            callbackInfo.setReturnValue(null);
            return;
        }
        sourceSlot.onPickupFromSlot(player, sourceStack);
        callbackInfo.setReturnValue(originalStack);
    }
}
