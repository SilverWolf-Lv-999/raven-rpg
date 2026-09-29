package keystrokesmod.module.impl.player;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.RightClickMouseEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.StringListSetting;
import keystrokesmod.ui.tracking.TrackingManagerScreen;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class AutoTracking extends Module {
    public static final ButtonSetting manager = new ButtonSetting("Open Manager Screen", ()-> mc.displayGuiScreen(new TrackingManagerScreen()));
    private final StringListSetting trackedNames;
    private EntityLivingBase target;
    private boolean tracking;
    private BlockPos waypoint;

    public AutoTracking() {
        super("Auto Tracking", category.player);
        this.registerSetting(manager);
        this.registerSetting(trackedNames = new StringListSetting("Tracked names", "Entity name", 128));
    }

    public boolean addTrackedName(String name) {
        boolean added = trackedNames.addEntry(name);
        if (added) {
            target = null;
        }
        return added;
    }

    public boolean isTracking() {
        return tracking;
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
        if (!tracking) {
            target = null;
            waypoint = null;
        }
    }

    @Override
    public void onDisable() {
        setTracking(false);
    }

    @Override
    public void onUpdate() {
        if (!tracking || !Utils.nullCheck()) {
            target = null;
            waypoint = null;
            return;
        }
        if (!isValidTarget(target)) {
            target = findTarget();
            waypoint = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClick(RightClickMouseEvent event) {
        if (!Utils.nullCheck() || !mc.gameSettings.keyBindSneak.isKeyDown() || mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null) {
            return;
        }
        Entity entity = mc.objectMouseOver.entityHit;
        if (entity instanceof EntityLivingBase && addTrackedName(entity.getName())) {
            Utils.sendMessage("&7Added living entity to tracking: &b" + entity.getName());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerInput(PostPlayerInputEvent event) {
        if (!tracking || !isValidTarget(target)) {
            return;
        }
        if (waypoint == null || mc.thePlayer.getDistanceSq(waypoint.getX() + 0.5D, mc.thePlayer.posY, waypoint.getZ() + 0.5D) < 1.5D || !isWalkable(waypoint)) {
            waypoint = findWaypoint();
        }
        double deltaX = (waypoint == null ? target.posX : waypoint.getX() + 0.5D) - mc.thePlayer.posX;
        double deltaZ = (waypoint == null ? target.posZ : waypoint.getZ() + 0.5D) - mc.thePlayer.posZ;
        double angle = Math.atan2(deltaZ, deltaX) - Math.toRadians(mc.thePlayer.rotationYaw + 90.0F);
        mc.thePlayer.movementInput.moveForward = (float) MathHelper.clamp_double(Math.cos(angle), -1.0D, 1.0D);
        mc.thePlayer.movementInput.moveStrafe = (float) MathHelper.clamp_double(-Math.sin(angle), -1.0D, 1.0D);
        if (mc.thePlayer.onGround && (mc.thePlayer.isCollidedHorizontally || target.posY > mc.thePlayer.posY + 0.5D || isStepAheadBlocked())) {
            mc.thePlayer.movementInput.jump = true;
        }
    }

    private BlockPos findWaypoint() {
        BlockPos origin = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        BlockPos goal = new BlockPos(target.posX, target.posY, target.posZ);
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int radius = 1; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    BlockPos candidate = origin.add(x, 0, z);
                    if (!isWalkable(candidate)) {
                        continue;
                    }
                    double score = candidate.distanceSq(goal) + candidate.distanceSq(origin) * 0.15D;
                    if (isClearPath(candidate)) {
                        score -= 5.0D;
                    }
                    if (score < bestScore) {
                        best = candidate;
                        bestScore = score;
                    }
                }
            }
            if (best != null && isClearPath(best)) {
                return best;
            }
        }
        return best;
    }

    private boolean isWalkable(BlockPos pos) {
        return mc.theWorld.isAirBlock(pos) && mc.theWorld.isAirBlock(pos.up()) && !mc.theWorld.isAirBlock(pos.down());
    }

    private boolean isClearPath(BlockPos pos) {
        double dx = pos.getX() + 0.5D - mc.thePlayer.posX;
        double dz = pos.getZ() + 0.5D - mc.thePlayer.posZ;
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) * 2.0D));
        for (int i = 1; i <= steps; i++) {
            double x = mc.thePlayer.posX + dx * i / steps;
            double z = mc.thePlayer.posZ + dz * i / steps;
            if (!isWalkable(new BlockPos(x, mc.thePlayer.posY, z))) {
                return false;
            }
        }
        return true;
    }

    private boolean isStepAheadBlocked() {
        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double x = mc.thePlayer.posX - Math.sin(yaw) * 0.7D;
        double z = mc.thePlayer.posZ + Math.cos(yaw) * 0.7D;
        return !isWalkable(new BlockPos(x, mc.thePlayer.posY, z));
    }

    private EntityLivingBase findTarget() {
        Entity viewEntity = mc.getRenderViewEntity();
        if (viewEntity == null || trackedNames.getEntries().isEmpty()) {
            return null;
        }
        double renderDistance = mc.gameSettings.renderDistanceChunks * 16.0D;
        AxisAlignedBB renderRange = viewEntity.getEntityBoundingBox().expand(renderDistance, renderDistance, renderDistance);
        List<EntityLivingBase> entities = mc.theWorld.getEntitiesWithinAABB(EntityLivingBase.class, renderRange);
        EntityLivingBase closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (EntityLivingBase entity : entities) {
            if (!isValidTarget(entity)) {
                continue;
            }
            double distance = viewEntity.getDistanceSqToEntity(entity);
            if (distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity == null || entity == mc.thePlayer || entity.isDead || !entity.isEntityAlive() || !matchesName(entity)) {
            return false;
        }
        Entity viewEntity = mc.getRenderViewEntity();
        if (viewEntity == null) {
            return false;
        }
        double renderDistance = mc.gameSettings.renderDistanceChunks * 16.0D;
        return viewEntity.getDistanceSqToEntity(entity) <= renderDistance * renderDistance;
    }

    private boolean matchesName(EntityLivingBase entity) {
        String entityName = entity.getName();
        for (String trackedName : trackedNames.getEntries()) {
            if (trackedName.equalsIgnoreCase(entityName)) {
                return true;
            }
        }
        return false;
    }
}
