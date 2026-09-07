package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoCD extends Module {
    private long lastRightClick;

    public AutoCD() {
        super("Auto CD", category.player);
    }

    @Override
    public void onEnable() {
        lastRightClick = 0L;
    }

    @Override
    public void onDisable() {
        lastRightClick = 0L;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button != 1 || !event.buttonstate) {
            return;
        }

        if (!Utils.nullCheck() || mc.currentScreen != null || !mc.thePlayer.isSneaking()) {
            lastRightClick = 0L;
            return;
        }

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemSword)) {
            lastRightClick = 0L;
            return;
        }

        long rightClickTime = System.currentTimeMillis();
        if (rightClickTime - lastRightClick <= 300L) {
            mc.thePlayer.sendChatMessage("/cd");
            lastRightClick = 0L;
            return;
        }

        lastRightClick = rightClickTime;
    }
}
