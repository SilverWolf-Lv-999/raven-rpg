package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.impl.client.RPGUI;
import keystrokesmod.utility.RPGUIUtility;
import net.minecraft.client.gui.GuiMerchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMerchant.class)
public class MixinGuiMerchant {
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
}
