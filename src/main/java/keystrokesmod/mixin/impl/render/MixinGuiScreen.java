package keystrokesmod.mixin.impl.render;

import keystrokesmod.Raven;
import keystrokesmod.event.KeyPressEvent;
import keystrokesmod.mixin.interfaces.IMerchantGui;
import keystrokesmod.module.impl.client.RPGUI;
import keystrokesmod.utility.RPGUIUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {
    @Shadow
    public Minecraft mc;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void messageSend(String msg, boolean addToChat, CallbackInfo callbackInfo) {
        if (addToChat && Raven.commandManager != null && Raven.commandManager.handleChatMessage(msg)) {
            this.mc.ingameGUI.getChatGUI().addToSentMessages(msg);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void injectHandleKeyboardInput(CallbackInfo callbackInfo) {
        KeyPressEvent event = new KeyPressEvent(Keyboard.getEventCharacter(), Keyboard.getEventKey());
        MinecraftForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleMouseInput()V", at = @At("HEAD"), cancellable = true)
    private void raven$scrollRpgMerchantTradeList(CallbackInfo callbackInfo) {
        if (!RPGUI.shouldStyleMerchant() || !((Object) this instanceof GuiMerchant)
            || !((Object) this instanceof IMerchantGui)) {
            return;
        }

        int wheelInput = Mouse.getEventDWheel();
        if (wheelInput == 0) {
            return;
        }

        GuiMerchant guiMerchant = (GuiMerchant) (Object) this;
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (!RPGUIUtility.isMerchantTradeListHovered(guiMerchant, mouseX, mouseY)) {
            return;
        }

        IMerchantGui merchantGui = (IMerchantGui) (Object) this;
        int rowDelta = Math.max(1, Math.abs(wheelInput) / 120);
        merchantGui.raven$setMerchantScrollRow(RPGUIUtility.clampMerchantTradeScroll(guiMerchant,
            merchantGui.raven$getMerchantScrollRow() + (wheelInput > 0 ? -rowDelta : rowDelta)));
        callbackInfo.cancel();
    }
}
