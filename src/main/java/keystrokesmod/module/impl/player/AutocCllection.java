package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AutocCllection extends Module {
    private List<BlockPos> targetPositions;
    private BlockPos scanOrigin;

    public static final ButtonSetting swing = new ButtonSetting("swing", false);

    public AutocCllection() {
        super("Auto Collection", category.player);
        this.registerSetting(swing);
    }

    @Override
    public void onEnable() {
        this.targetPositions = new ArrayList<>();
        this.scanOrigin = null;
    }

    @Override
    public void onDisable() {
        this.targetPositions = null;
        this.scanOrigin = null;
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            this.targetPositions.clear();
            this.scanOrigin = null;
            return;
        }

        BlockPos origin = new BlockPos(mc.thePlayer);
        if (this.scanOrigin == null || !this.scanOrigin.equals(origin)) {
            this.targetPositions = this.findTargets(origin);
            this.scanOrigin = origin;
        }

        Iterator<BlockPos> iterator = this.targetPositions.iterator();
        while (iterator.hasNext()) {
            BlockPos position = iterator.next();
            if (!this.isCollectionBlock(position) || mc.thePlayer.getDistanceSq(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D) > 36.0D) {
                iterator.remove();
            }
        }

        if (this.targetPositions.isEmpty()) {
            return;
        }

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (BlockPos position : this.targetPositions) {
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
                position,
                EnumFacing.UP.getIndex(),
                heldItem == null ? null : heldItem.copy(),
                0.5F,
                1.0F,
                0.5F
            ));
        }
        if (swing.isToggled())
            mc.thePlayer.swingItem();
    }

    private List<BlockPos> findTargets(BlockPos origin) {
        List<BlockPos> targets = new ArrayList<>();

        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos position = origin.add(x, y, z);
                    if (!mc.theWorld.isBlockLoaded(position) || !this.isCollectionBlock(position)) {
                        continue;
                    }

                    double distance = mc.thePlayer.getDistanceSq(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
                    if (distance <= 36.0D) {
                        targets.add(position);
                    }
                }
            }
        }
        return targets;
    }

    private boolean isCollectionBlock(BlockPos position) {
        if (!mc.theWorld.isBlockLoaded(position)) {
            return false;
        }

        IBlockState state = mc.theWorld.getBlockState(position);
        Block block = state.getBlock();
        return block == Blocks.cauldron
            || block == Blocks.redstone_ore
            || block == Blocks.lit_redstone_ore
            || block == Blocks.quartz_ore
            || (block == Blocks.red_flower && block.getMetaFromState(state) == 0);
    }
}
