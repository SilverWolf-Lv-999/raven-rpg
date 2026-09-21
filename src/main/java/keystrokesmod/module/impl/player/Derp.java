package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Derp extends Module {
    private final SliderSetting pitch = new SliderSetting("Pitch", 0.0F, -90.0F, 90F, 10F);
    private final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", 12.0F, 0.0F, 36F, 1F);
    private final ButtonSetting silent = new ButtonSetting("Silent", true);
    private float yaw;

    public Derp() {
        super("Derp", Module.category.player);
        this.registerSetting(this.pitch);
        this.registerSetting(this.rotationSpeed);
        this.registerSetting(this.silent);
    }

    @Override
    public void onEnable() {
        this.yaw = mc.thePlayer.rotationYaw;
    }

    @Override
    public void onDisable() {
        this.yaw = mc.thePlayer.rotationYaw;
    }

    @Override
    public void onUpdate() {
        if (this.silent.isToggled()) {
            return;
        }
        mc.thePlayer.rotationYaw = RotationUtils.applyVanilla((float) (mc.thePlayer.rotationYaw + this.rotationSpeed.getInput()));
        mc.thePlayer.rotationPitch = RotationUtils.clampPitch((float) this.pitch.getInput());
        this.yaw = mc.thePlayer.rotationYaw;
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent event) {
        if (!this.silent.isToggled()) {
            return;
        }
        this.yaw = RotationUtils.applyVanilla((float) (this.yaw + this.rotationSpeed.getInput()));
        event.setYaw(this.yaw);
        event.setPitch(RotationUtils.clampPitch((float) this.pitch.getInput()));
    }
}
