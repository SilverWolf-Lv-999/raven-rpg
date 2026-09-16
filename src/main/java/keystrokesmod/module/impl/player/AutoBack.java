package keystrokesmod.module.impl.player;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoBack extends Module {
    private boolean pendingBack;

    public AutoBack() {
        super("Auto Back", category.player);
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof S07PacketRespawn) {
            pendingBack = true;
        }
    }

    @Override
    public void onUpdate() {
        if (!pendingBack || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        pendingBack = false;
        mc.thePlayer.sendChatMessage("/back");
    }

    @Override
    public void onDisable() {
        pendingBack = false;
    }
}
