package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.StepHeightEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Step extends Module {
    private final SliderSetting mode;
    private final SliderSetting height;
    private final SliderSetting delay;
    private boolean aacStep;
    private boolean stepPending;
    private double stepX;
    private double stepY;
    private double stepZ;
    private long lastStepTime;
    private int stepConfirmTicks;

    public Step() {
        super("Step", category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", 0, new String[]{"Vanilla", "AAC"}));
        this.registerSetting(height = new SliderSetting("Height", 1.0D, 0.6D, 10.0D, 0.1D));
        this.registerSetting(delay = new SliderSetting("Delay", 0.0D, 0.0D, 500.0D, 1.0D));
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (mode.getInput() != 1 || mc.thePlayer == null) {
            this.aacStep = false;
            return;
        }

        if (mc.thePlayer.isRiding() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()
                || mc.thePlayer.isInWeb || !Utils.isMoving() || !mc.thePlayer.isCollidedHorizontally) {
            this.aacStep = false;
            return;
        }

        long now = System.currentTimeMillis();
        if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()
                && now - this.lastStepTime >= (long) delay.getInput()) {
            double direction = Utils.getDirection();
            boolean canStep = true;
            for (int offset = -10; offset <= 10; offset++) {
                double angle = direction + Math.toRadians(offset * 8.0D);
                AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().offset(
                        -Math.sin(angle) * 0.2D,
                        1.001335979112147D,
                        Math.cos(angle) * 0.2D);
                if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, box).isEmpty()) {
                    canStep = false;
                    break;
                }
            }

            if (canStep) {
                this.stepX = mc.thePlayer.posX;
                this.stepY = mc.thePlayer.posY;
                this.stepZ = mc.thePlayer.posZ;
                this.stepPending = true;
                this.stepConfirmTicks = 3;
                mc.thePlayer.motionX *= 1.26D;
                mc.thePlayer.motionZ *= 1.26D;
                mc.thePlayer.jump();
                this.aacStep = true;
                this.lastStepTime = now;
            }
        }

        if (this.aacStep) {
            mc.thePlayer.motionY -= 0.015D;
            if (!mc.thePlayer.isUsingItem() && mc.thePlayer.moveStrafing == 0.0F) {
                mc.thePlayer.jumpMovementFactor = 0.3F;
            }
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (mode.getInput() != 1 || mc.thePlayer == null || !this.stepPending) {
            return;
        }

        if (event.getPosY() - this.stepY > 0.6D) {
            mc.thePlayer.isAirBorne = true;
            mc.thePlayer.addStat(StatList.jumpStat, 1);
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                    this.stepX, this.stepY + 0.41999998688698D, this.stepZ, false));
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                    this.stepX, this.stepY + 0.7531999805212D, this.stepZ, false));
            this.stepPending = false;
            return;
        }

        if (--this.stepConfirmTicks <= 0) {
            this.stepPending = false;
        }
    }

    @SubscribeEvent
    public void onStepHeight(StepHeightEvent event) {
        if (mc.thePlayer == null || event.entity != mc.thePlayer
                || mc.thePlayer.capabilities.isFlying || mc.thePlayer.isRiding()
                || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) {
            return;
        }

        event.stepHeight = (float) height.getInput();
    }

    @Override
    public String getInfo() {
        return mode.getOptions()[(int) mode.getInput()];
    }

    @Override
    public void onEnable() {
        this.lastStepTime = System.currentTimeMillis();
        this.aacStep = false;
        this.stepPending = false;
    }

    @Override
    public void onDisable() {
        this.aacStep = false;
        this.stepPending = false;
    }
}