package keystrokesmod.module.impl.world;

import keystrokesmod.event.AttackEvent;
import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.TextSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class Disabler extends Module {
    public ButtonSetting startSprint;
    private ButtonSetting grimPlace;
    private ButtonSetting vulcanScaffold;
    private SliderSetting vulcanPacketTick;
    public ButtonSetting verusFly;
    public ButtonSetting verusCombat;
    private ButtonSetting onlyCombat;
    private ButtonSetting intaveFly;
    private ButtonSetting noRotationDisabler;
    private SliderSetting modifyMode;
    private SliderSetting offsetAmount;
    private ButtonSetting basicDisabler;
    private ButtonSetting cancelC00;
    private ButtonSetting cancelC0F;
    private ButtonSetting cancelC0A;
    private ButtonSetting cancelC0B;
    private ButtonSetting cancelC07;
    private ButtonSetting cancelC13;
    private ButtonSetting cancelC03;
    private ButtonSetting c03NoMove;
    private ButtonSetting watchdogMotion;
    private ButtonSetting notWhenStarAvailable;
    private ButtonSetting watchdogInventory;
    public ButtonSetting spigotSpam;
    public TextSetting message;
    private ButtonSetting chatDebug;
    private ButtonSetting notificationDebug;
    private ButtonSetting betaVerus;
    private ButtonSetting betaVerusSilentFlagApply;
    private SliderSetting betaVerusBufferSize;
    private SliderSetting betaVerusRepeatTimesValue;
    private SliderSetting betaVerusRepeatTimesFighting;
    private SliderSetting betaVerusFlagDelay;
    private ButtonSetting verusExperimental;
    private ButtonSetting verusExpVoidTP;
    private SliderSetting verusExpVoidTPDelay;
    private ButtonSetting miniblox;

    private final Queue<Packet<?>> intavePackets = new LinkedBlockingQueue<>();
    private final ArrayDeque<Packet<?>> betaVerusPacketBuffer = new ArrayDeque<>();
    private boolean shouldDelay;
    private boolean transaction;
    private boolean isOnCombat;
    private boolean betaVerus2Stat;
    private boolean betaVerusModified;
    private boolean execute;
    private boolean jump;
    private boolean c16;
    private boolean c0d;
    private int flags;
    private int airTicks;
    private int cancelNext;
    private long betaVerusLagTimer;
    private long lastVoidTP;

    public Disabler() {
        super("Disabler", category.world);

        this.registerSetting(startSprint = new ButtonSetting("StartSprint", true));
        this.registerSetting(grimPlace = new ButtonSetting("GrimPlace", false));
        this.registerSetting(vulcanScaffold = new ButtonSetting("VulcanScaffold", false));
        this.registerSetting(vulcanPacketTick = new SliderSetting("VulcanScaffoldPacketTick", 15, 1, 20, 1));
        this.registerSetting(verusFly = new ButtonSetting("VerusFly", false));
        this.registerSetting(verusCombat = new ButtonSetting("VerusCombat", false));
        this.registerSetting(onlyCombat = new ButtonSetting("OnlyCombat", true));
        this.registerSetting(intaveFly = new ButtonSetting("intaveFly", false));
        this.registerSetting(noRotationDisabler = new ButtonSetting("NoRotationDisabler", false));
        this.registerSetting(modifyMode = new SliderSetting("Mode", 1, new String[] {
                "ConvertNull", "Spoof", "Zero", "SpoofZero", "Negative", "OffsetYaw", "Invalid"
        }));
        this.registerSetting(offsetAmount = new SliderSetting("OffsetAmount", 6, -180, 180, 1));
        this.registerSetting(basicDisabler = new ButtonSetting("BasicDisabler", false));
        this.registerSetting(cancelC00 = new ButtonSetting("CancelC00", true));
        this.registerSetting(cancelC0F = new ButtonSetting("CancelC0F", true));
        this.registerSetting(cancelC0A = new ButtonSetting("CancelC0A", true));
        this.registerSetting(cancelC0B = new ButtonSetting("CancelC0B", true));
        this.registerSetting(cancelC07 = new ButtonSetting("CancelC07", true));
        this.registerSetting(cancelC13 = new ButtonSetting("CancelC13", true));
        this.registerSetting(cancelC03 = new ButtonSetting("CancelC03", true));
        this.registerSetting(c03NoMove = new ButtonSetting("C03-NoMove", true));
        this.registerSetting(watchdogMotion = new ButtonSetting("WatchdogMotion", false));
        this.registerSetting(notWhenStarAvailable = new ButtonSetting("NotWithStar", true));
        this.registerSetting(watchdogInventory = new ButtonSetting("WatchdogInventory", false));
        this.registerSetting(spigotSpam = new ButtonSetting("SpigotSpam", false));
        this.registerSetting(message = new TextSetting("Message", "/skill", "", 64));
        this.registerSetting(chatDebug = new ButtonSetting("ChatDebug", false));
        this.registerSetting(notificationDebug = new ButtonSetting("NotificationDebug", false));
        this.registerSetting(betaVerus = new ButtonSetting("VerusBeta", false));
        this.registerSetting(betaVerusSilentFlagApply = new ButtonSetting("SilentFlagApply", false));
        this.registerSetting(betaVerusBufferSize = new SliderSetting("BufferSize", 300, 0, 1000, 1));
        this.registerSetting(betaVerusRepeatTimesValue = new SliderSetting("RepeatTimes", 1, 1, 5, 1));
        this.registerSetting(betaVerusRepeatTimesFighting = new SliderSetting("BRepeatTimesFighting", 1, 1, 5, 1));
        this.registerSetting(betaVerusFlagDelay = new SliderSetting("FlagDelay", 40, 35, 60, 1));
        this.registerSetting(verusExperimental = new ButtonSetting("VerusExperimental", false));
        this.registerSetting(verusExpVoidTP = new ButtonSetting("ExpVoidTP", false));
        this.registerSetting(verusExpVoidTPDelay = new SliderSetting("ExpVoidTPDelay", "ms", 1000, 0, 30000, 1));
        this.registerSetting(miniblox = new ButtonSetting("MinibloxDesync", false));
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (mc.thePlayer == null) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (basicDisabler.isToggled()) {
            if (packet instanceof C00PacketKeepAlive && cancelC00.isToggled()) {
                event.setCanceled(true);
                debugMessage("Cancelled C00-KeepAlive");
            }
            else if (packet instanceof C0FPacketConfirmTransaction && cancelC0F.isToggled()) {
                event.setCanceled(true);
                debugMessage("Cancelled C0F-Transaction");
            }
            else if (packet instanceof C0APacketAnimation && cancelC0A.isToggled()) {
                event.setCanceled(true);
                debugMessage("Cancelled C0A-Swing");
            }
            else if (packet instanceof C0BPacketEntityAction && cancelC0B.isToggled()) {
                event.setCanceled(true);
                debugMessage("Cancelled C0B-Action");
            }
            else if (packet instanceof C07PacketPlayerDigging && cancelC07.isToggled()) {
                event.setCanceled(true);
                debugMessage("Cancelled C07-Digging");
            }
            else if (packet instanceof C13PacketPlayerAbilities && cancelC13.isToggled()) {
                event.setCanceled(true);
                debugMessage("Cancelled C13-Abilities");
            }
            else if (packet instanceof C03PacketPlayer
                    && !(packet instanceof C03PacketPlayer.C04PacketPlayerPosition)
                    && !(packet instanceof C03PacketPlayer.C05PacketPlayerLook)
                    && !(packet instanceof C03PacketPlayer.C06PacketPlayerPosLook)
                    && cancelC03.isToggled()
                    && (!c03NoMove.isToggled() || !Utils.isMoving())) {
                event.setCanceled(true);
                debugMessage("Cancelled C03-Flying");
            }
        }

        if (noRotationDisabler.isToggled() && packet instanceof C03PacketPlayer) {
            C03PacketPlayer playerPacket = (C03PacketPlayer) packet;
            int mode = (int) modifyMode.getInput();
            if (mode == 0) {
                event.setCanceled(true);
                if (playerPacket.isMoving()) {
                    PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                            playerPacket.getPositionX(),
                            playerPacket.getPositionY(),
                            playerPacket.getPositionZ(),
                            playerPacket.isOnGround()
                    ));
                }
                else {
                    PacketUtils.sendPacketNoEvent(new C03PacketPlayer(playerPacket.isOnGround()));
                }
            }
            else if (mode >= 1 && mode <= 5 && playerPacket.getRotating()) {
                float yaw = playerPacket.getYaw();
                float pitch = playerPacket.getPitch();
                if (mode == 1) {
                    yaw = mc.thePlayer.rotationYaw;
                    pitch = mc.thePlayer.rotationPitch;
                }
                else if (mode == 2) {
                    yaw = 0;
                    pitch = 0;
                }
                else if (mode == 4) {
                    yaw = -yaw;
                    pitch = -pitch;
                }
                else if (mode == 5) {
                    yaw += (float) offsetAmount.getInput();
                }

                event.setCanceled(true);
                sendPlayerPacket(playerPacket, playerPacket.getPositionX(), playerPacket.getPositionY(),
                        playerPacket.getPositionZ(), yaw, pitch, playerPacket.isOnGround());
            }
            else if (mode == 3) {
                event.setCanceled(true);
                sendPlayerPacket(playerPacket,
                        playerPacket.isMoving() ? playerPacket.getPositionX() : mc.thePlayer.posX,
                        playerPacket.isMoving() ? playerPacket.getPositionY() : mc.thePlayer.posY,
                        playerPacket.isMoving() ? playerPacket.getPositionZ() : mc.thePlayer.posZ,
                        0,
                        0,
                        playerPacket.isOnGround());
            }
        }

        if (grimPlace.isToggled() && packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement placement = (C08PacketPlayerBlockPlacement) packet;
            if (placement.getPlacedBlockDirection() >= 0 && placement.getPlacedBlockDirection() <= 5) {
                event.setCanceled(true);
                PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(
                        placement.getPosition(),
                        6 + placement.getPlacedBlockDirection() * 7,
                        placement.getStack(),
                        placement.getPlacedBlockOffsetX(),
                        placement.getPlacedBlockOffsetY(),
                        placement.getPlacedBlockOffsetZ()
                ));
                debugMessage("Modified Place Packet");
            }
        }

        if (betaVerus.isToggled()) {
            if (packet instanceof C0FPacketConfirmTransaction) {
                betaVerusPacketBuffer.add(packet);
                event.setCanceled(true);
                if (betaVerusPacketBuffer.size() > (int) betaVerusBufferSize.getInput()) {
                    if (!betaVerus2Stat) {
                        betaVerus2Stat = true;
                        Utils.sendMessage("AntiCheat is disabled.");
                    }
                    Packet<?> buffered = betaVerusPacketBuffer.poll();
                    for (int i = 0; i < betaVerusRepeatTimes(); i++) {
                        PacketUtils.sendPacketNoEvent(buffered);
                    }
                }
                debugMessage("Packet C0F IN BufferSize=" + betaVerusPacketBuffer.size());
            }
            else if (packet instanceof C03PacketPlayer
                    && mc.thePlayer.ticksExisted % (int) betaVerusFlagDelay.getInput() == 0
                    && mc.thePlayer.ticksExisted > betaVerusFlagDelay.getInput() + 1
                    && !betaVerusModified) {
                C03PacketPlayer playerPacket = (C03PacketPlayer) packet;
                betaVerusModified = true;
                event.setCanceled(true);
                sendPlayerPacket(playerPacket,
                        playerPacket.getPositionX(),
                        playerPacket.getPositionY() - 11.4514,
                        playerPacket.getPositionZ(),
                        playerPacket.getYaw(),
                        playerPacket.getPitch(),
                        false);
                debugMessage("Packet C03 -> BetaVerus Y offset");
            }
            if (mc.thePlayer.ticksExisted <= 7) {
                betaVerusLagTimer = System.currentTimeMillis();
                betaVerusPacketBuffer.clear();
            }
        }

        if (verusExperimental.isToggled() && verusExpVoidTP.isToggled()
                && packet instanceof C03PacketPlayer
                && mc.thePlayer.ticksExisted > 20
                && mc.thePlayer.posY > -64
                && lastVoidTP + (long) verusExpVoidTPDelay.getInput() < System.currentTimeMillis()) {
            lastVoidTP = System.currentTimeMillis();
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX, -48, mc.thePlayer.posZ, true
            ));
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false
            ));
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround
            ));
            cancelNext = 2;
            event.setCanceled(true);
            debugMessage("VerusExp VoidTP attempt");
        }
        if (miniblox.isToggled()
                && (packet instanceof C03PacketPlayer.C04PacketPlayerPosition
                || packet instanceof C03PacketPlayer.C05PacketPlayerLook)) {
            PacketUtils.sendPacketNoEvent(new C0CPacketInput(
                    mc.thePlayer.moveStrafing,
                    mc.thePlayer.moveForward,
                    mc.thePlayer.movementInput.jump,
                    mc.thePlayer.isSneaking()
            ));
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.thePlayer == null) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (watchdogMotion.isToggled()) {
            if (packet instanceof S07PacketRespawn) {
                flags = 0;
                execute = false;
                jump = true;
            }
            else if (packet instanceof S08PacketPlayerPosLook && ++flags >= 20) {
                execute = false;
                flags = 0;
            }
        }

        if (intaveFly.isToggled() && packet instanceof S08PacketPlayerPosLook && mc.thePlayer.capabilities.isFlying) {
            shouldDelay = true;
            debugMessage("Started Canceling IntaveFly");
        }
        if (intaveFly.isToggled() && packet instanceof S32PacketConfirmTransaction && shouldDelay) {
            event.setCanceled(true);
            intavePackets.add(packet);
        }

        if (verusCombat.isToggled() && packet instanceof S32PacketConfirmTransaction) {
            if (mc.thePlayer.ticksExisted <= 20) {
                isOnCombat = false;
                return;
            }
            if (onlyCombat.isToggled() && !isOnCombat) {
                return;
            }
            event.setCanceled(true);
            PacketUtils.sendPacketNoEvent(new C0FPacketConfirmTransaction(
                    transaction ? 1 : -1,
                    (short) (transaction ? -1 : 1),
                    transaction
            ));
            transaction = !transaction;
            isOnCombat = false;
        }

        if (verusExperimental.isToggled() && verusExpVoidTP.isToggled()
                && packet instanceof S08PacketPlayerPosLook && cancelNext > 0) {
            cancelNext--;
            event.setCanceled(true);
            debugMessage("VerusExp cancelled server position look");
        }

        if (betaVerus.isToggled() && betaVerusSilentFlagApply.isToggled()
                && packet instanceof S08PacketPlayerPosLook) {
            S08PacketPlayerPosLook positionLook = (S08PacketPlayerPosLook) packet;
            double x = positionLook.getX() - mc.thePlayer.posX;
            double y = positionLook.getY() - mc.thePlayer.posY;
            double z = positionLook.getZ() - mc.thePlayer.posZ;
            if (Math.sqrt(x * x + y * y + z * z) <= 8) {
                event.setCanceled(true);
                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(
                        positionLook.getX(),
                        positionLook.getY(),
                        positionLook.getZ(),
                        positionLook.getYaw(),
                        positionLook.getPitch(),
                        true
                ));
                debugMessage("Silent Flag");
            }
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!watchdogMotion.isToggled() || (notWhenStarAvailable.isToggled() && hasStar())) {
            return;
        }
        if (execute && airTicks >= 10) {
            if (airTicks % 2 == 0) {
                event.setPosX(event.getPosX() + 0.095);
            }
            mc.thePlayer.setVelocity(0, 0, 0);
        }
    }

    @SubscribeEvent
    public void onJump(JumpEvent event) {
        if (watchdogMotion.isToggled() && jump) {
            jump = false;
            execute = true;
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEvent event) {
        isOnCombat = true;
    }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load event) {
        isOnCombat = false;
        if (betaVerus.isToggled()) {
            betaVerus2Stat = false;
            betaVerusPacketBuffer.clear();
            betaVerusLagTimer = System.currentTimeMillis();
        }
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null) {
            return;
        }

        airTicks = mc.thePlayer.onGround ? 0 : airTicks + 1;

        if (watchdogMotion.isToggled() && jump) {
            mc.thePlayer.jump();
        }

        if (watchdogInventory.isToggled()) {
            c16 = false;
            c0d = false;
            if (mc.currentScreen instanceof GuiInventory) {
                int interval = mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 3 : 4;
                if (mc.thePlayer.ticksExisted % interval == 0) {
                    PacketUtils.sendPacketNoEvent(new C0DPacketCloseWindow());
                }
                else if (mc.thePlayer.ticksExisted % interval == 1) {
                    PacketUtils.sendPacketNoEvent(new C16PacketClientStatus(
                            C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT
                    ));
                }
            }
        }

        if (verusFly.isToggled()) {
            if (!isOnCombat && !mc.thePlayer.isDead) {
                BlockPos position = new BlockPos(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY + (mc.thePlayer.posY > 0 ? -255 : 255),
                        mc.thePlayer.posZ
                );
                PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(
                        position,
                        256,
                        new ItemStack(Items.water_bucket),
                        0,
                        (float) ThreadLocalRandom.current().nextDouble(0.5, 0.94),
                        0
                ));
            }
            else {
                isOnCombat = false;
            }
        }

        if (vulcanScaffold.isToggled()
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isInLava()
                && !mc.thePlayer.isDead
                && !mc.thePlayer.isOnLadder()
                && Utils.isMoving()
                && mc.thePlayer.ticksExisted % (int) vulcanPacketTick.getInput() == 0) {
            PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.START_SNEAKING
            ));
            PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.STOP_SNEAKING
            ));
        }

        if (betaVerus.isToggled()) {
            betaVerusModified = false;
            if (System.currentTimeMillis() - betaVerusLagTimer >= 490) {
                betaVerusLagTimer = System.currentTimeMillis();
                if (!betaVerusPacketBuffer.isEmpty()) {
                    Packet<?> packet = betaVerusPacketBuffer.poll();
                    for (int i = 0; i < betaVerusRepeatTimes(); i++) {
                        PacketUtils.sendPacketNoEvent(packet);
                    }
                    debugMessage("Packet Buffer Dump");
                }
                else {
                    debugMessage("Empty Packet Buffer");
                }
            }
        }
    }

    @Override
    public void onDisable() {
        shouldDelay = false;
        intavePackets.clear();
        transaction = false;
        betaVerus2Stat = false;
        betaVerusModified = false;
        betaVerusPacketBuffer.clear();
        execute = false;
        jump = false;
        c16 = false;
        c0d = false;
        flags = 0;
        airTicks = 0;
        cancelNext = 0;
        isOnCombat = false;
    }

    private int betaVerusRepeatTimes() {
        return (int) (isOnCombat ? betaVerusRepeatTimesFighting.getInput() : betaVerusRepeatTimesValue.getInput());
    }

    private boolean hasStar() {
        for (int slot = 36; slot <= 44; slot++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.inventorySlots.get(slot).getStack();
            if (stack != null && stack.getItem() == Items.nether_star) {
                return true;
            }
        }
        return false;
    }

    private void sendPlayerPacket(C03PacketPlayer packet, double x, double y, double z,
                                  float yaw, float pitch, boolean onGround) {
        if (packet.isMoving() && packet.getRotating()) {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(
                    x, y, z, yaw, pitch, onGround
            ));
        }
        else if (packet.isMoving()) {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
                    x, y, z, onGround
            ));
        }
        else if (packet.getRotating()) {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(
                    yaw, pitch, onGround
            ));
        }
        else {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer(onGround));
        }
    }

    private void debugMessage(String message) {
        if (chatDebug.isToggled() || notificationDebug.isToggled()) {
            Utils.sendMessage("&f" + message);
        }
    }
}
