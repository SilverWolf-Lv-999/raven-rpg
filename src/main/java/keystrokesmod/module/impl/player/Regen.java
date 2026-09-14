package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Regen extends Module {
    private final SliderSetting health;
    private final SliderSetting packetsPerTick;

    public Regen() {
        super("Regen", category.player);
        this.registerSetting(health = new SliderSetting("Health", 10.0, 0.0, 20.0, 1.0));
        this.registerSetting(packetsPerTick = new SliderSetting("Packets per tick", 5.0, 2.0, 20.0, 1.0));
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()
                || mc.getNetHandler() == null
                || mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount() > health.getInput()) {
            return;
        }

        for (int index = 0; index < (int) packetsPerTick.getInput(); index++) {
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    mc.thePlayer.rotationYaw,
                    mc.thePlayer.rotationPitch,
                    mc.thePlayer.onGround
            ));
        }
    }
}
