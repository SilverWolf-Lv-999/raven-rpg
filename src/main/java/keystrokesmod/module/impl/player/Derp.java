package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.RotationUtils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Derp extends Module {
    private final SliderSetting pitch = new SliderSetting("Pitch", 0.0F, -90.0F, 90F, 10F);
    private final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", 12.0F, 0.0F, 36F, 1F);
    private final ButtonSetting silent = new ButtonSetting("Silent", true);
    private final ButtonSetting serverMode = new ButtonSetting("serverMode", false);
    private float yaw;

    public Derp() {
        super("Derp", Module.category.player);
        this.registerSetting(this.pitch);
        this.registerSetting(this.rotationSpeed);
        this.registerSetting(this.silent);
        this.registerSetting(this.serverMode);
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
        if (this.silent.isToggled() || this.serverMode.isToggled()) {
            return;
        }
        mc.thePlayer.rotationYaw = RotationUtils.applyVanilla((float) (mc.thePlayer.rotationYaw + this.rotationSpeed.getInput()));
        mc.thePlayer.rotationPitch = RotationUtils.clampPitch((float) this.pitch.getInput());
        this.yaw = mc.thePlayer.rotationYaw;
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent event) {
        if (this.serverMode.isToggled() || !this.silent.isToggled()) {
            return;
        }
        this.yaw = RotationUtils.applyVanilla((float) (this.yaw + this.rotationSpeed.getInput()));
        event.setYaw(this.yaw);
        event.setPitch(RotationUtils.clampPitch((float) this.pitch.getInput()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacket(SendPacketEvent event) {
        if (!this.serverMode.isToggled() || !(event.getPacket() instanceof C03PacketPlayer)) {
            return;
        }
        C03PacketPlayer packet = (C03PacketPlayer) event.getPacket();
        this.yaw = RotationUtils.applyVanilla((float) (this.yaw + this.rotationSpeed.getInput()));
        float pitch = RotationUtils.clampPitch((float) this.pitch.getInput());
        event.setCanceled(true);
        if (packet.isMoving()) {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(packet.getPositionX(), packet.getPositionY(), packet.getPositionZ(), this.yaw, pitch, packet.isOnGround()));
        }
        else {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(this.yaw, pitch, packet.isOnGround()));
        }
    }
}
