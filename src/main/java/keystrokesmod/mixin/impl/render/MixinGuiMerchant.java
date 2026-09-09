package keystrokesmod.mixin.impl.render;

import keystrokesmod.mixin.impl.accessor.IAccessorGuiScreen;
import keystrokesmod.module.impl.client.RPGUI;
import keystrokesmod.utility.RPGUIUtility;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMerchant.class)
public class MixinGuiMerchant {
    @Inject(method = "initGui()V", at = @At("HEAD"))
    private void raven$prepareRpgMerchantGui(CallbackInfo callbackInfo) {
        if (RPGUI.shouldStyleMerchant()) {
            RPGUIUtility.updateMerchantGuiSize((GuiMerchant) (Object) this);
        }
    }

    @Inject(method = "initGui()V", at = @At("TAIL"))
    private void raven$removeMerchantButtons(CallbackInfo callbackInfo) {
        if (RPGUI.shouldStyleMerchant() && ((GuiMerchant) (Object) this).buttonList.size() >= 2) {
            GuiMerchant guiMerchant = (GuiMerchant) (Object) this;
            guiMerchant.buttonList.remove(guiMerchant.buttonList.size() - 1);
            guiMerchant.buttonList.remove(guiMerchant.buttonList.size() - 1);
        }
    }

    @Inject(method = "drawScreen(IIF)V", at = @At("HEAD"))
    private void raven$updateRpgMerchantGui(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        GuiMerchant guiMerchant = (GuiMerchant) (Object) this;
        if (RPGUI.shouldStyleMerchant()) {
            RPGUIUtility.updateMerchantGuiSize(guiMerchant);
        } else if (guiMerchant.xSize != 176) {
            guiMerchant.xSize = 176;
            guiMerchant.guiLeft = (guiMerchant.width - guiMerchant.xSize) / 2;
        }
    }

    @Inject(method = "drawGuiContainerBackgroundLayer(FII)V", at = @At("HEAD"), cancellable = true)
    private void raven$drawRpgBackground(float partialTicks, int mouseX, int mouseY, CallbackInfo callbackInfo) {
        if (RPGUI.shouldStyleMerchant()) {
            RPGUIUtility.drawMerchantBackground((GuiMerchant) (Object) this);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "drawGuiContainerForegroundLayer(II)V", at = @At("HEAD"), cancellable = true)
    private void raven$drawRpgForeground(int mouseX, int mouseY, CallbackInfo callbackInfo) {
        if (RPGUI.shouldStyleMerchant()) {
            RPGUIUtility.drawMerchantForeground((GuiMerchant) (Object) this);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "drawScreen(IIF)V", at = @At("TAIL"))
    private void raven$drawRpgTradeList(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (RPGUI.shouldStyleMerchant()) {
            GuiMerchant guiMerchant = (GuiMerchant) (Object) this;
            ItemStack hoveredStack = RPGUIUtility.drawMerchantTradeList(guiMerchant, guiMerchant.selectedMerchantRecipe, mouseX, mouseY);
            if (hoveredStack != null) {
                ((IAccessorGuiScreen) guiMerchant).callRenderToolTip(hoveredStack, mouseX, mouseY);
            }
        }
    }
}
