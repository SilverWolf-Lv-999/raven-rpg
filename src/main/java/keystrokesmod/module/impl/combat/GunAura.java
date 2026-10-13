package keystrokesmod.module.impl.combat;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.model.ItemStack;
import keystrokesmod.utility.rpg.RPGUtility;
import keystrokesmod.utility.rpg.gun.RPGGun;

public class GunAura extends Module {
    public final SliderSetting targetMode;

    private final String[] targets = {"ground", "target"};
    private RPGGun heldGun;

    public GunAura() {
        super("Gun Aura", category.combat);
        this.registerSetting(targetMode = new SliderSetting("Target Mode",0 ,targets));
    }

    @Override
    public void onUpdate() {
        heldGun = RPGUtility.getRPGGun(ItemStack.convert(mc.thePlayer.getHeldItem()));
    }

    @Override
    public String getInfo() {
        if (heldGun == null) {
            return "";
        }
        return heldGun.bullet + (heldGun.reloading ? "R" : "");
    }

    public RPGGun getHeldGun() {
        return heldGun;
    }
}
