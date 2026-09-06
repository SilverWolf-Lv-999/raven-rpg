package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.impl.client.RPGUI;
import keystrokesmod.utility.RPGUIUtility;
import net.minecraft.client.gui.inventory.GuiChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChest.class)
public class MixinGuiChest {
    @Inject(method = "drawGuiContainerBackgroundLayer(FII)V", at = @At("HEAD"), cancellable = true)
    private void raven$drawRpgBackground(float partialTicks, int mouseX, int mouseY, CallbackInfo callbackInfo) {
        GuiChest guiChest = (GuiChest) (Object) this;
        if (RPGUI.shouldStyleChest(guiChest)) {
            RPGUIUtility.drawChestBackground(guiChest);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "drawGuiContainerForegroundLayer(II)V", at = @At("HEAD"), cancellable = true)
    private void raven$drawRpgForeground(int mouseX, int mouseY, CallbackInfo callbackInfo) {
        GuiChest guiChest = (GuiChest) (Object) this;
        if (RPGUI.shouldStyleChest(guiChest)) {
            RPGUIUtility.drawChestForeground(guiChest);
            callbackInfo.cancel();
        }
    }
}
