package keystrokesmod.module.impl.combat;

import keystrokesmod.event.KnockbackEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Velocity extends Module {
    public static SliderSetting horizontal;
    public static SliderSetting vertical;
    private SliderSetting chance;
    private ButtonSetting onlyWhileTargeting;
    private ButtonSetting disableS;
    public boolean disable;

    public Velocity() {
        super("Velocity", category.combat, 0);
        this.registerSetting(horizontal = new SliderSetting("Horizontal", "%", 90.0D, 0.0D, 100.0D, 1.0D));
        this.registerSetting(vertical = new SliderSetting("Vertical", "%", 100.0D, 0.0D, 100.0D, 1.0D));
        this.registerSetting(chance = new SliderSetting("Chance", "%", 100.0D, 0.0D, 100.0D, 1.0D));
        this.registerSetting(onlyWhileTargeting = new ButtonSetting("Only while targeting", false));
        this.registerSetting(disableS = new ButtonSetting("Disable while holding S", false));
        this.closetModule = true;
    }

    @Override
    public String getInfo() {
        return (int) horizontal.getInput() + "%" + " " + (int) vertical.getInput() + "%";
    }

    @SubscribeEvent
    public void onKnockback(KnockbackEvent event) {
        if (!event.isCanceled() && Utils.nullCheck() && !LongJump.stopVelocity && !disable) {
            if (ModuleManager.antiKnockback.isEnabled()) {
                return;
            }
            if (onlyWhileTargeting.isToggled() && (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null)) {
                return;
            }
            if (disableS.isToggled() && Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
                return;
            }
            if (chance.getInput() == 0) {
                return;
            }
            if (chance.getInput() != 100) {
                double ch = Math.random();
                if (ch >= chance.getInput() / 100.0D) {
                    return;
                }
            }
            event.setX(event.getX() * horizontal.getInput() / 100.0D);
            event.setY(event.getY() * vertical.getInput() / 100.0D);
            event.setZ(event.getZ() * horizontal.getInput() / 100.0D);
        }
    }
}
