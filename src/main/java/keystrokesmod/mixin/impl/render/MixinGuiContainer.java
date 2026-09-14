package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.RPGUI;
import keystrokesmod.mixin.interfaces.IMerchantGui;
import keystrokesmod.utility.RPGUIUtility;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void raven$cancelManagedInventoryInput(int mouseX, int mouseY, int mouseButton, CallbackInfo callbackInfo) {
        if (RPGUI.shouldStyleMerchant() && mouseButton == 0 && (Object) this instanceof GuiMerchant
            && (Object) this instanceof IMerchantGui) {
            GuiMerchant guiMerchant = (GuiMerchant) (Object) this;
            int recipeIndex = RPGUIUtility.getMerchantTradeIndex(guiMerchant, mouseX, mouseY,
                ((IMerchantGui) (Object) this).raven$getMerchantScrollRow());
            if (recipeIndex >= 0) {
                RPGUIUtility.selectAndFillMerchantTrade(guiMerchant, recipeIndex, GuiScreen.isShiftKeyDown());
                callbackInfo.cancel();
                return;
            }
        }
        if (shouldCancelManualInventoryInput()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "mouseClickMove", at = @At("HEAD"), cancellable = true)
    private void raven$cancelManagedInventoryMouseDrag(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick, CallbackInfo callbackInfo) {
        if (shouldCancelManualInventoryInput()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void raven$cancelManagedInventoryMouseRelease(int mouseX, int mouseY, int state, CallbackInfo callbackInfo) {
        if (shouldCancelManualInventoryInput()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void raven$handleAutoOperation(Slot slotIn, int slotId, int clickedButton, int clickType, CallbackInfo callbackInfo) {
        if (shouldCancelManualInventoryInput()) {
            callbackInfo.cancel();
            return;
        }
        if (ModuleManager.autoOperation != null && ModuleManager.autoOperation.startOperation(slotIn, clickedButton, clickType)) {
            callbackInfo.cancel();
        }
    }

    private static boolean shouldCancelManualInventoryInput() {
        return ModuleManager.invManager != null && ModuleManager.invManager.shouldCancelManualInventoryInput();
    }
}
