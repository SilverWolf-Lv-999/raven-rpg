package keystrokesmod.module.impl.movement;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class HClip extends Module {
    public final SliderSetting distance;
    public final ButtonSetting sendMessage;

    public HClip() {
        super("H Clip", category.movement);
        this.registerSetting(distance = new SliderSetting("Distance", 3.0, -20.0, 20.0, 0.5));
        this.registerSetting(sendMessage = new ButtonSetting("Send message", true));
    }

    @Override
    public void onEnable() {
        double distance = this.distance.getInput();
        if (distance != 0.0) {
            double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
            double newX = mc.thePlayer.posX - Math.sin(yaw) * distance;
            double newZ = mc.thePlayer.posZ + Math.cos(yaw) * distance;
            mc.thePlayer.setPosition(newX, mc.thePlayer.posY, newZ);
            if (this.sendMessage.isToggled()) {
                Utils.sendMessage("&7Teleported to x: " + newX + ", z: " + newZ + " (&b" + distance + "&7 blocks).");
            }
        }
        this.disable();
    }
}
