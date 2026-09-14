package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Regen extends Module {
    private final GroupSetting modeGroup;
    private final SliderSetting mode;
    private final SliderSetting speed;
    private final SliderSetting vanillaTimer;
    private final SliderSetting spartanPackets;
    private final SliderSetting spartanTimer;

    private final GroupSetting conditionsGroup;
    private final SliderSetting delay;
    private final SliderSetting health;
    private final SliderSetting food;

    private final GroupSetting pausesGroup;
    private final ButtonSetting noAir;
    private final ButtonSetting noMove;
    private final ButtonSetting pauseOnDamage;
    private final ButtonSetting potionEffect;

    private final GroupSetting effectsGroup;
    private final ButtonSetting fire;
    private final ButtonSetting badEffects;
    private final SliderSetting maximumSpeed;

    private long lastBurstTime;
    private boolean resetTimer;

    public Regen() {
        super("Regen", category.player);

        this.registerSetting(modeGroup = new GroupSetting("Mode"));
        this.registerSetting(mode = new SliderSetting(modeGroup, "Mode", 0, new String[]{"Vanilla", "Spartan"}));
        this.registerSetting(speed = new SliderSetting(modeGroup, "Speed", 100.0, 1.0, 100.0, 1.0));
        this.registerSetting(vanillaTimer = new SliderSetting(modeGroup, "VanillaTimer", 1.0, 0.1, 10.0, 0.1));
        this.registerSetting(spartanPackets = new SliderSetting(modeGroup, "SpartanPackets", 9.0, 1.0, 50.0, 1.0));
        this.registerSetting(spartanTimer = new SliderSetting(modeGroup, "SpartanTimer", 0.45, 0.1, 2.0, 0.01));

        this.registerSetting(conditionsGroup = new GroupSetting("Conditions"));
        this.registerSetting(delay = new SliderSetting(conditionsGroup, "Delay", "ms", 0.0, 0.0, 10000.0, 1.0));
        this.registerSetting(health = new SliderSetting(conditionsGroup, "Health", 18.0, 0.0, 20.0, 1.0));
        this.registerSetting(food = new SliderSetting(conditionsGroup, "Food", 18.0, 0.0, 20.0, 1.0));

        this.registerSetting(pausesGroup = new GroupSetting("Pauses"));
        this.registerSetting(noAir = new ButtonSetting(pausesGroup, "NoAir", false));
        this.registerSetting(noMove = new ButtonSetting(pausesGroup, "NoMove", false));
        this.registerSetting(pauseOnDamage = new ButtonSetting(pausesGroup, "PauseOnDamage", false));
        this.registerSetting(potionEffect = new ButtonSetting(pausesGroup, "PotionEffect", false));

        this.registerSetting(effectsGroup = new GroupSetting("Effects"));
        this.registerSetting(fire = new ButtonSetting(effectsGroup, "Fire", false));
        this.registerSetting(badEffects = new ButtonSetting(effectsGroup, "BadEffects", false));
        this.registerSetting(maximumSpeed = new SliderSetting(effectsGroup, "MaximumSpeed", 100.0, 5.0, 200.0, 1.0));
    }

    @Override
    public void guiUpdate() {
        boolean vanilla = mode.getInput() == 0.0;
        boolean effects = fire.isToggled() || badEffects.isToggled();
        speed.setVisible(vanilla, this);
        vanillaTimer.setVisible(vanilla, this);
        spartanPackets.setVisible(!vanilla, this);
        spartanTimer.setVisible(!vanilla, this);
        maximumSpeed.setVisible(effects, this);
    }

    @Override
    public String getInfo() {
        return mode.getOptions()[(int) mode.getInput()];
    }

    @Override
    public void onDisable() {
        Utils.resetTimer();
        lastBurstTime = 0L;
        resetTimer = false;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (resetTimer) {
            Utils.resetTimer();
            resetTimer = false;
        }

        if (!Utils.nullCheck() || mc.getNetHandler() == null) {
            return;
        }

        boolean lowHealth = mc.thePlayer.getHealth() < health.getInput();
        boolean onFire = fire.isToggled()
                && mc.thePlayer.isBurning()
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isWet();
        boolean badEffect = false;
        int maximumEffectDuration = 0;

        if (badEffects.isToggled()) {
            for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
                int potionId = effect.getPotionID();
                if (effect.getDuration() > 0
                        && (potionId == Potion.poison.id
                        || potionId == Potion.wither.id
                        || potionId == Potion.weakness.id
                        || potionId == Potion.moveSlowdown.id
                        || potionId == Potion.digSlowdown.id
                        || potionId == Potion.blindness.id
                        || potionId == Potion.confusion.id
                        || potionId == Potion.hunger.id)) {
                    badEffect = true;
                    maximumEffectDuration = Math.max(maximumEffectDuration, effect.getDuration());
                }
            }
        }

        if (!mc.playerController.getCurrentGameType().isSurvivalOrAdventure()
                || (noAir.isToggled() && !mc.thePlayer.onGround)
                || (noMove.isToggled() && Utils.isMoving())
                || (pauseOnDamage.isToggled() && mc.thePlayer.hurtTime > 0)
                || mc.thePlayer.getFoodStats().getFoodLevel() <= food.getInput()
                || !mc.thePlayer.isEntityAlive()
                || (!lowHealth && !onFire && !badEffect)
                || (potionEffect.isToggled() && !mc.thePlayer.isPotionActive(Potion.regeneration))) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBurstTime < (long) delay.getInput()) {
            return;
        }

        if (mode.getInput() == 0.0) {
            int packets = lowHealth ? (int) speed.getInput() : 0;
            if (onFire) {
                packets = Math.max(packets, 9);
            }
            if (badEffect) {
                packets = Math.max(packets, Math.min(maximumEffectDuration / 20, (int) maximumSpeed.getInput()));
            }

            if (packets > 0) {
                if (vanillaTimer.getInput() != 1.0) {
                    ((IAccessorMinecraft) mc).getTimer().timerSpeed = (float) vanillaTimer.getInput();
                    resetTimer = true;
                }
                for (int index = 0; index < packets; index++) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
                }
            }
        }
        else if (!Utils.isMoving() && mc.thePlayer.onGround) {
            for (int index = 0; index < (int) spartanPackets.getInput(); index++) {
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));
            }
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = (float) spartanTimer.getInput();
            resetTimer = true;
        }

        lastBurstTime = currentTime;
    }
}
