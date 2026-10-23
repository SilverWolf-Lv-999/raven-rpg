package keystrokesmod.module.impl.player;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.Module;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoMining extends Module {
    public AutoMining() {
        super("Auto Mining", category.player);
    }

    @SubscribeEvent
    public void pMove(PrePlayerInputEvent event) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) {
            return;
        }

    }
}
