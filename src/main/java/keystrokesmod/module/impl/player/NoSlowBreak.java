package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;

public class NoSlowBreak extends Module {
    private final ButtonSetting miningFatigue;
    private final ButtonSetting onAir;
    private final ButtonSetting underwater;

    public NoSlowBreak() {
        super("NoSlowBreak", category.player);
        this.registerSetting(miningFatigue = new ButtonSetting("MiningFatigue", true));
        this.registerSetting(onAir = new ButtonSetting("OnAir", true));
        this.registerSetting(underwater = new ButtonSetting("Underwater", true));
    }

    public boolean shouldRemoveMiningFatigue() {
        return this.isEnabled() && miningFatigue.isToggled();
    }

    public boolean shouldRemoveOnAir() {
        return this.isEnabled() && onAir.isToggled();
    }

    public boolean shouldRemoveUnderwater() {
        return this.isEnabled() && underwater.isToggled();
    }
}
