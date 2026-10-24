package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PlayerMoveEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.event.StepHeightEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Step extends Module {
    private static final String[] MODES = new String[]{"Vanilla", "AAC", "Jump", "NCP", "MotionNCP", "OldNCP", "LAAC", "AAC3.3.4", "Spartan", "Rewinside", "BlocksMCTimer"};
    private final SliderSetting mode;
    private final SliderSetting height;
    private final SliderSetting jumpHeight;
    private final SliderSetting delay;
    private boolean aacStep;
    private boolean stepPending;
    private boolean oldNcpPending;
    private boolean spartanSwitch;
    private double stepX;
    private double stepY;
    private double stepZ;
    private long lastStepTime;
    private int stepConfirmTicks;
    private int ncpNextStep;
    private int blocksTimerPhase;
    private long lastChestScan;
    private boolean chestNearby;

    public Step() {
        super("Step", category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", 3, MODES));
        this.registerSetting(height = new SliderSetting("Height", 1.0D, 0.6D, 10.0D, 0.1D));
        this.registerSetting(jumpHeight = new SliderSetting("JumpHeight", 0.42D, 0.37D, 0.42D, 0.01D));
        this.registerSetting(delay = new SliderSetting("Delay", 0.0D, 0.0D, 500.0D, 1.0D));
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            if (this.blocksTimerPhase != 0) {
                ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
                this.blocksTimerPhase = 0;
            }
            return;
        }

        String currentMode = getMode();
        if (!currentMode.equals("BlocksMCTimer") && this.blocksTimerPhase != 0) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
            this.blocksTimerPhase = 0;
        }
        if (currentMode.equals("BlocksMCTimer") && this.blocksTimerPhase != 0) {
            updateBlocksMCTimer();
            return;
        }

        if (mc.thePlayer.isRiding() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()
                || mc.thePlayer.isInWeb || !Utils.isMoving()) {
            this.aacStep = false;
            return;
        }

        if (currentMode.equals("Jump")) {
            if (mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround
                    && !mc.gameSettings.keyBindJump.isKeyDown()) {
                fakeJump();
                mc.thePlayer.motionY = this.jumpHeight.getInput();
            }
            return;
        }

        if (currentMode.equals("LAAC")) {
            if (mc.thePlayer.isCollidedHorizontally) {
                if (mc.thePlayer.onGround && canStepAfterDelay()) {
                    fakeJump();
                    mc.thePlayer.motionY += 0.620000001490116D;
                    double direction = Utils.getDirection();
                    mc.thePlayer.motionX -= Math.sin(direction) * 0.2D;
                    mc.thePlayer.motionZ += Math.cos(direction) * 0.2D;
                    this.lastStepTime = System.currentTimeMillis();
                }
                mc.thePlayer.onGround = true;
            } else {
                this.stepPending = false;
            }
            return;
        }

        if (currentMode.equals("AAC3.3.4")) {
            if (mc.thePlayer.isCollidedHorizontally) {
                if (mc.thePlayer.onGround && canStep()) {
                    mc.thePlayer.motionX *= 1.26D;
                    mc.thePlayer.motionZ *= 1.26D;
                    mc.thePlayer.jump();
                    this.aacStep = true;
                }
                if (this.aacStep) {
                    mc.thePlayer.motionY -= 0.015D;
                    if (!mc.thePlayer.isUsingItem() && mc.thePlayer.moveStrafing == 0.0F) {
                        mc.thePlayer.jumpMovementFactor = 0.3F;
                    }
                }
            } else {
                this.aacStep = false;
            }
            return;
        }

        if (currentMode.equals("BlocksMCTimer")) {
            updateBlocksMCTimer();
        }
    }

    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!getMode().equals("MotionNCP") || mc.thePlayer == null
                || !mc.thePlayer.isCollidedHorizontally || mc.gameSettings.keyBindJump.isKeyDown()) {
            return;
        }

        if (mc.thePlayer.onGround && canStep()) {
            fakeJump();
            mc.thePlayer.motionY = 0.0D;
            event.y = 0.41999998688698D;
            this.ncpNextStep = 1;
        } else if (this.ncpNextStep == 1) {
            event.y = 0.33319999363422D;
            this.ncpNextStep = 2;
        } else if (this.ncpNextStep == 2) {
            double direction = Utils.getDirection();
            event.y = 0.24813599859094704D;
            event.x = -Math.sin(direction) * 0.7D;
            event.z = Math.cos(direction) * 0.7D;
            this.ncpNextStep = 0;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (mc.thePlayer == null || !this.stepPending) {
            return;
        }

        if (event.getPosY() - this.stepY > 0.6D) {
            String stepMode = getMode();
            if (stepMode.equals("NCP") || stepMode.equals("AAC")) {
                fakeJump();
                sendStepPackets(false);
                this.lastStepTime = System.currentTimeMillis();
            } else if (stepMode.equals("Rewinside")) {
                fakeJump();
                sendStepPackets(true);
                this.lastStepTime = System.currentTimeMillis();
            } else if (stepMode.equals("Spartan")) {
                fakeJump();
                if (this.spartanSwitch) {
                    sendStepPackets(true);
                } else {
                    PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                            this.stepX, this.stepY + 0.6D, this.stepZ, false));
                }
                this.spartanSwitch = !this.spartanSwitch;
                this.lastStepTime = System.currentTimeMillis();
            } else if (stepMode.equals("OldNCP")) {
                this.oldNcpPending = true;
            }
            this.stepPending = false;
            return;
        }

        if (--this.stepConfirmTicks <= 0) {
            this.stepPending = false;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (this.oldNcpPending && getMode().equals("OldNCP")
                && event.getPacket() instanceof C03PacketPlayer) {
            ((C03PacketPlayer) event.getPacket()).y += 0.07D;
            this.oldNcpPending = false;
        }
    }

    @SubscribeEvent
    public void onStepHeight(StepHeightEvent event) {
        if (mc.thePlayer == null || event.entity != mc.thePlayer
                || mc.thePlayer.capabilities.isFlying || mc.thePlayer.isRiding()
                || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) {
            return;
        }

        if (usesHeight() && mc.thePlayer.onGround && canStepAfterDelay()) {
            event.stepHeight = (float) this.height.getInput();
            if (event.stepHeight > 0.6F) {
                this.stepX = mc.thePlayer.posX;
                this.stepY = mc.thePlayer.posY;
                this.stepZ = mc.thePlayer.posZ;
                this.stepPending = true;
                this.stepConfirmTicks = 3;
            }
        } else {
            event.stepHeight = 0.6F;
        }
    }

    private boolean usesHeight() {
        String currentMode = getMode();
        return currentMode.equals("Vanilla") || currentMode.equals("AAC") || currentMode.equals("NCP")
                || currentMode.equals("OldNCP") || currentMode.equals("Spartan") || currentMode.equals("Rewinside");
    }

    private boolean canStepAfterDelay() {
        return System.currentTimeMillis() - this.lastStepTime >= (long) this.delay.getInput();
    }

    private boolean canStep() {
        if (mc.thePlayer.isSneaking() || mc.gameSettings.keyBindJump.isKeyDown()) {
            return false;
        }

        double direction = Utils.getDirection();
        for (int offset = -10; offset <= 10; offset++) {
            double angle = direction + Math.toRadians(offset * 8.0D);
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().offset(
                    -Math.sin(angle) * 0.2D,
                    1.001335979112147D,
                    Math.cos(angle) * 0.2D);
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, box).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void updateBlocksMCTimer() {
        if (this.blocksTimerPhase == 1) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 0.2F;
            this.blocksTimerPhase = 2;
            return;
        }
        if (this.blocksTimerPhase == 2) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 4.0F;
            this.blocksTimerPhase = 3;
            return;
        }
        if (this.blocksTimerPhase == 3) {
            Utils.setSpeed(0.27D, false);
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
            this.blocksTimerPhase = 0;
            return;
        }
        if (!mc.thePlayer.onGround || !mc.thePlayer.isCollidedHorizontally || !canStep()) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
            return;
        }

        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        long now = System.currentTimeMillis();
        if (this.lastChestScan == 0L || now - this.lastChestScan >= 500L) {
            this.chestNearby = false;
            for (int x = -2; x <= 2 && !this.chestNearby; x++) {
                for (int y = -2; y <= 2 && !this.chestNearby; y++) {
                    for (int z = -2; z <= 2; z++) {
                        Block block = BlockUtils.getBlock(playerPos.add(x, y, z));
                        if (block == Blocks.chest || block == Blocks.ender_chest || block == Blocks.trapped_chest) {
                            this.chestNearby = true;
                            break;
                        }
                    }
                }
            }
            this.lastChestScan = now;
        }
        if (this.chestNearby) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
            return;
        }

        fakeJump();
        mc.thePlayer.jump();
        ((IAccessorMinecraft) mc).getTimer().timerSpeed = 5.0F;
        this.blocksTimerPhase = 1;
    }

    private void sendStepPackets(boolean thirdPacket) {
        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                this.stepX, this.stepY + 0.41999998688698D, this.stepZ, false));
        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                this.stepX, this.stepY + 0.7531999805212D, this.stepZ, false));
        if (thirdPacket) {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                    this.stepX, this.stepY + 1.001335979112147D, this.stepZ, false));
        }
    }

    private void fakeJump() {
        mc.thePlayer.isAirBorne = true;
        mc.thePlayer.addStat(StatList.jumpStat, 1);
    }

    private String getMode() {
        return MODES[(int) this.mode.getInput()];
    }

    @Override
    public void guiUpdate() {
        this.height.setVisible(usesHeight(), this);
        this.jumpHeight.setVisible(getMode().equals("Jump"), this);
    }

    @Override
    public String getInfo() {
        return getMode();
    }

    @Override
    public void onEnable() {
        this.lastStepTime = System.currentTimeMillis();
        this.aacStep = false;
        this.stepPending = false;
        this.oldNcpPending = false;
        this.spartanSwitch = false;
        this.ncpNextStep = 0;
        this.blocksTimerPhase = 0;
        this.lastChestScan = 0L;
        this.chestNearby = false;
    }

    @Override
    public void onDisable() {
        this.aacStep = false;
        this.stepPending = false;
        this.oldNcpPending = false;
        this.ncpNextStep = 0;
        this.lastChestScan = 0L;
        this.chestNearby = false;
        if (this.blocksTimerPhase != 0) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
            this.blocksTimerPhase = 0;
        }
        if (mc.thePlayer != null) {
            mc.thePlayer.stepHeight = 0.6F;
        }
    }
}
