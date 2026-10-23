package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PostMouseSelectionEvent;
import keystrokesmod.event.PreAttackEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.event.PreSlotScrollEvent;
import keystrokesmod.event.SlotUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AutoMining extends Module {
    private BlockPos targetPos;
    private Vec3 targetHitVec;
    private EnumFacing targetSide;
    private int targetSlot = -1;
    private int previousSlot = -1;
    private long nextScanTime;

    public AutoMining() {
        super("Auto Mining", category.player);
    }

    @Override
    public void onEnable() {
        targetPos = null;
        targetHitVec = null;
        targetSide = null;
        targetSlot = -1;
        previousSlot = -1;
        nextScanTime = 0L;
    }

    @Override
    public void onDisable() {
        restoreSlot();
        targetPos = null;
        targetHitVec = null;
        targetSide = null;
        targetSlot = -1;
        nextScanTime = 0L;
        if (Utils.nullCheck()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), Mouse.isButtonDown(0));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), Mouse.isButtonDown(1));
        }
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            targetPos = null;
            targetHitVec = null;
            targetSide = null;
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
            targetHitVec = null;
            targetSide = null;
            targetSlot = -1;
            restoreSlot();
            nextScanTime = now + 1000L;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), Mouse.isButtonDown(0));
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
        if (block == Blocks.air || block == Blocks.bedrock) {
            return;
        }

        if (targetSlot == -1) {
            targetSlot = findPickaxeSlot(block);
        }
        if (targetSlot == -1) {
            return;
        }

        if (mc.thePlayer.inventory.currentItem != targetSlot) {
            if (previousSlot == -1) {
                previousSlot = mc.thePlayer.inventory.currentItem;
            }
            mc.thePlayer.inventory.currentItem = targetSlot;
            ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        }

        Vec3 eye = mc.thePlayer.getPositionEyes(1.0F);
        targetHitVec = RotationUtils.closestPointOnAabb(BlockUtils.getBlockSelectionBox(targetPos), eye);
        targetSide = BlockUtils.facingFromBlockCenterToPoint(targetPos, targetHitVec);
        ((IAccessorPlayerControllerMP) mc.playerController).setBlockHitDelay(0);
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent event) {
        if (!shouldMineTarget()) {
            return;
        }
        float baseYaw = event.yaw != null ? event.yaw : RotationUtils.serverRotations[0];
        float basePitch = event.pitch != null ? event.pitch : RotationUtils.serverRotations[1];
        float[] rotations = RotationUtils.getRotationsToPoint(
            targetHitVec.xCoord,
            targetHitVec.yCoord,
            targetHitVec.zCoord,
            baseYaw,
            basePitch
        );
        event.setYaw(rotations[0]);
        event.setPitch(rotations[1]);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPostMouseSelection(PostMouseSelectionEvent event) {
        modifyMouseOver();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPrePlayerInteract(PrePlayerInteractEvent event) {
        if (targetPos == null || targetSlot == -1 || !Utils.nullCheck()) {
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }
        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        KeyBinding.setKeyBindState(attackKey, false);
        if (shouldMineTarget()) {
            KeyBinding.setKeyBindState(attackKey, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreAttack(PreAttackEvent event) {
        if (mc.currentScreen == null && targetPos != null && targetSlot != -1) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(MouseEvent event) {
        if (mc.currentScreen == null && targetPos != null && targetSlot != -1 && (event.button == 0 || event.button == 1)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSlotScroll(PreSlotScrollEvent event) {
        if (mc.currentScreen == null && targetPos != null && targetSlot != -1) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSlotUpdate(SlotUpdateEvent event) {
        if (mc.currentScreen == null && targetPos != null && targetSlot != -1) {
            event.setCanceled(true);
        }
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

    public boolean shouldMineTarget() {
        return isEnabled()
            && targetPos != null
            && targetSlot != -1
            && targetHitVec != null
            && targetSide != null
            && Utils.nullCheck()
            && mc.theWorld.getBlockState(targetPos).getBlock() != Blocks.air
            && mc.theWorld.getBlockState(targetPos).getBlock() != Blocks.bedrock;
    }

    private void modifyMouseOver() {
        if (!shouldMineTarget() || mc.getRenderViewEntity() == null) {
            return;
        }
        mc.objectMouseOver = new MovingObjectPosition(targetHitVec, targetSide, targetPos);
        mc.pointedEntity = null;
        EntityRenderer renderer = mc.entityRenderer;
        if (renderer instanceof IAccessorEntityRenderer) {
            ((IAccessorEntityRenderer) renderer).setPointedEntity(null);
        }
    }

    private void restoreSlot() {
        if (previousSlot != -1 && Utils.nullCheck() && mc.thePlayer.inventory.currentItem != previousSlot) {
            mc.thePlayer.inventory.currentItem = previousSlot;
            ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        }
        previousSlot = -1;
    }
}
