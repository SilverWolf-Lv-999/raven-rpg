package keystrokesmod.mixin.impl.world;

import java.util.List;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.NoSlowBreak;
import net.minecraft.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import keystrokesmod.event.CollisionEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock {
    @Shadow
    public abstract AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state);

    @Overwrite
    public void addCollisionBoxesToList(World worldIn, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        AxisAlignedBB axisalignedbb = this.getCollisionBoundingBox(worldIn, pos, state);
        CollisionEvent event = new CollisionEvent(pos, (Block)(Object)this, axisalignedbb);
        MinecraftForge.EVENT_BUS.post(event);
        axisalignedbb = event.boundingBox;
        if (axisalignedbb != null && mask.intersectsWith(axisalignedbb)) {
            list.add(axisalignedbb);
        }
    }

    @Inject(method = "getPlayerRelativeBlockHardness", at = @At("RETURN"), cancellable = true)
    private void removeBreakSlowdown(EntityPlayer playerIn, World worldIn, BlockPos pos, CallbackInfoReturnable<Float> callbackInfo) {
        NoSlowBreak noSlowBreak = ModuleManager.noSlowBreak;
        if (noSlowBreak == null) {
            return;
        }

        float hardness = callbackInfo.getReturnValue();
        if (noSlowBreak.shouldRemoveUnderwater()
                && playerIn.isInsideOfMaterial(Material.water)
                && !EnchantmentHelper.getAquaAffinityModifier(playerIn)) {
            hardness *= 5.0F;
        }
        if (noSlowBreak.shouldRemoveOnAir() && !playerIn.onGround) {
            hardness *= 5.0F;
        }
        if (noSlowBreak.shouldRemoveMiningFatigue() && playerIn.isPotionActive(Potion.digSlowdown)) {
            int amplifier = playerIn.getActivePotionEffect(Potion.digSlowdown).getAmplifier();
            float slowdown = amplifier == 0 ? 0.3F : amplifier == 1 ? 0.09F : amplifier == 2 ? 0.0027F : 0.00081F;
            hardness /= slowdown;
        }
        callbackInfo.setReturnValue(hardness);
    }
}
