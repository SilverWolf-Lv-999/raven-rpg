package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import net.minecraft.block.BlockSign;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;

public class AutoWalk extends Module {
    private boolean pendingWalk;
    private boolean walking;
    private boolean stoppedAtBeacon;
    private BlockPos lastScanPosition;
    private long walkStartTime;
    private final Set<BlockPos> interactedSigns = new HashSet<>();

    public AutoWalk() {
        super("Auto Walk", category.movement);
    }

    @Override
    public void onEnable() {
        this.pendingWalk = false;
        this.walking = false;
        this.stoppedAtBeacon = false;
        this.lastScanPosition = null;
        this.walkStartTime = 0L;
        this.interactedSigns.clear();
    }

    @Override
    public void onDisable() {
        this.pendingWalk = false;
        this.walking = false;
        this.stoppedAtBeacon = false;
        this.lastScanPosition = null;
        this.walkStartTime = 0L;
        this.interactedSigns.clear();
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof S07PacketRespawn) {
            this.pendingWalk = true;
            this.stoppedAtBeacon = false;
            this.lastScanPosition = null;
            this.interactedSigns.clear();
        }
    }

    @Override
    public void onUpdate() {
        if (!this.pendingWalk || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        this.pendingWalk = false;
        this.walking = true;
        this.stoppedAtBeacon = false;
        this.walkStartTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!this.walking || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (System.currentTimeMillis() - this.walkStartTime < 1000L) {
            return;
        }

        if (this.hasBeaconBelow()) {
            this.walking = false;
            this.stoppedAtBeacon = true;
            mc.thePlayer.motionX = 0.0D;
            mc.thePlayer.motionZ = 0.0D;
            return;
        }

        BlockPos scanPosition = new BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY,
                mc.thePlayer.posZ
        );
        if (scanPosition.equals(this.lastScanPosition)) {
            return;
        }
        this.lastScanPosition = scanPosition;

        BlockPos signPosition = this.findSignAhead();
        if (signPosition == null) {
            return;
        }

        this.interactedSigns.add(signPosition);
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
                signPosition,
                1,
                heldItem == null ? null : heldItem.copy(),
                0.5F,
                1.0F,
                0.5F
        ));
    }

    @SubscribeEvent
    public void onPrePlayerInput(PrePlayerInputEvent event) {
        if (this.walking) {
            event.setForward(1.0F);
            event.setStrafe(0.0F);
            event.setJump(false);
            event.setSneak(false);
        }
        else if (this.stoppedAtBeacon) {
            event.setForward(0.0F);
            event.setStrafe(0.0F);
            event.setJump(false);
            event.setSneak(false);
        }
    }

    private boolean hasBeaconBelow() {
        BlockPos feet = new BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY,
                mc.thePlayer.posZ
        );
        for (int depth = 1; depth <= 3; depth++) {
            if (mc.theWorld.getBlockState(feet.down(depth)).getBlock() == Blocks.beacon) {
                return true;
            }
        }
        return false;
    }

    private BlockPos findSignAhead() {
        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        int centerX = MathHelper.floor_double(mc.thePlayer.posX);
        int centerY = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY);
        int centerZ = MathHelper.floor_double(mc.thePlayer.posZ);
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int y = centerY - 1; y <= centerY + 2; y++) {
                for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                    BlockPos position = new BlockPos(x, y, z);
                    if (this.interactedSigns.contains(position)
                            || !(mc.theWorld.getBlockState(position).getBlock() instanceof BlockSign)) {
                        continue;
                    }

                    double offsetX = x + 0.5D - mc.thePlayer.posX;
                    double offsetZ = z + 0.5D - mc.thePlayer.posZ;
                    double forward = offsetX * forwardX + offsetZ * forwardZ;
                    double lateral = offsetX * forwardZ - offsetZ * forwardX;
                    if (forward < 0.0D || forward > 5.0D || Math.abs(lateral) > 1.5D) {
                        continue;
                    }

                    double distance = offsetX * offsetX
                            + (y + 0.5D - mc.thePlayer.posY) * (y + 0.5D - mc.thePlayer.posY)
                            + offsetZ * offsetZ;
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = position;
                    }
                }
            }
        }
        return nearest;
    }
}
