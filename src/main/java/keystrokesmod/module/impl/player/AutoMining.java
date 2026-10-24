package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public class AutoMining extends Module {
    private BlockPos targetPos;
    private int targetSlot = -1;
    private int previousSlot = -1;
    private long nextScanTime;

    public AutoMining() {
        super("Auto Mining", category.player);
    }

    @Override
    public void onEnable() {
        targetPos = null;
        targetSlot = -1;
        previousSlot = -1;
        nextScanTime = 0L;
    }

    @Override
    public void onDisable() {
        restoreSlot();
        targetPos = null;
        targetSlot = -1;
        nextScanTime = 0L;
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            targetPos = null;
            targetSlot = -1;
            previousSlot = -1;
            return;
        }
        if (!mc.thePlayer.capabilities.allowEdit) {
            return;
        }

        long now = System.currentTimeMillis();
        if (targetPos != null && mc.theWorld.getBlockState(targetPos).getBlock() == Blocks.bedrock) {
            targetPos = null;
            targetSlot = -1;
            nextScanTime = 0L;
        }

        if (targetPos == null) {
            if (now < nextScanTime) {
                return;
            }
            targetPos = findTarget();
            if (targetPos == null) {
                restoreSlot();
                nextScanTime = now + 1000L;
                return;
            }
        }

        Block block = mc.theWorld.getBlockState(targetPos).getBlock();
        if (targetSlot == -1) {
            targetSlot = findPickaxeSlot(block);
        }
        if (targetSlot == -1) {
            return;
        }

        if (previousSlot == -1) {
            previousSlot = mc.thePlayer.inventory.currentItem;
        }
        if (mc.thePlayer.inventory.currentItem != targetSlot) {
            mc.thePlayer.inventory.currentItem = targetSlot;
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(targetSlot));
        }

        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(
            C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
            targetPos,
            EnumFacing.UP
        ));
    }

    private BlockPos findTarget() {
        BlockPos origin = new BlockPos(mc.thePlayer);
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0F);
        BlockPos closest = null;
        double closestDistance = 36.0D;

        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos pos = origin.add(x, y, z);
                    if (!mc.theWorld.isBlockLoaded(pos)) {
                        continue;
                    }
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if (!isMineableOre(block)) {
                        continue;
                    }
                    double distance = eye.squareDistanceTo(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
                    if (distance <= closestDistance) {
                        closest = pos;
                        closestDistance = distance;
                    }
                }
            }
        }
        return closest;
    }

    private boolean isMineableOre(Block block) {
        return block == Blocks.coal_ore
            || block == Blocks.iron_ore
            || block == Blocks.gold_ore
            || block == Blocks.redstone_ore
            || block == Blocks.lit_redstone_ore
            || block == Blocks.lapis_ore
            || block == Blocks.diamond_ore
            || block == Blocks.emerald_ore;
    }

    private int findPickaxeSlot(Block block) {
        int bestSlot = -1;
        float bestSpeed = 0.0F;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null || !(stack.getItem() instanceof ItemPickaxe)) {
                continue;
            }
            float speed = Utils.getEfficiency(stack, block);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private void restoreSlot() {
        if (previousSlot != -1 && Utils.nullCheck() && mc.thePlayer.inventory.currentItem != previousSlot) {
            mc.thePlayer.inventory.currentItem = previousSlot;
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(previousSlot));
        }
        previousSlot = -1;
    }
}
