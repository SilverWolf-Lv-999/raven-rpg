package keystrokesmod.module.impl.player;

import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class KeepSneak extends Module {
    public KeepSneak() {
        super("Keep Sneak", category.player);
    }

    @Override
    public void onDisable() {
        if (Utils.nullCheck() && !mc.thePlayer.isSneaking()) {
            PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
        }
    }

    @Override
    public void onUpdate() {
        PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
    }

    @SubscribeEvent
    public void e(PostMotionEvent event) {
        PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
    }
}
