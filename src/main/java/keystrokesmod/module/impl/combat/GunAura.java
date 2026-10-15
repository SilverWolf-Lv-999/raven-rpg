package keystrokesmod.module.impl.combat;

import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.model.ItemStack;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.rpg.RPGUtility;
import keystrokesmod.utility.rpg.gun.RPGGun;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GunAura extends Module {
    public final SliderSetting targetMode;

    private final SliderSetting mode;
    private final SliderSetting fov;
    private final SliderSetting range;
    private final SliderSetting sortMode;
    private final SliderSetting switchDelay;
    private final SliderSetting switchTargets;
    private final SliderSetting fireDelay;
    private final ButtonSetting targetInvis;
    private final ButtonSetting attackMobs;
    private final ButtonSetting targetArmorStand;
    private final ButtonSetting targetPlayer;
    private final ButtonSetting hitThroughWalls;
    private final ButtonSetting hitThroughEntities;
    private final ButtonSetting ignoreTeammates;
    private final ButtonSetting prioritizeEnemies;
    private final ButtonSetting targetDead;

    private final String[] targetModes = {"ground", "target"};
    private final String[] modes = {"Single", "Switch"};
    private final String[] sortModes = {"Distance", "Health", "Hurt time", "Yaw"};
    private RPGGun heldGun;
    private EntityLivingBase target;
    private long selectedAt;
    private long nextFireTime;
    private int switchIndex;
    private boolean fireAfterMotion;

    public GunAura() {
        super("Gun Aura", category.combat);
        this.registerSetting(targetMode = new SliderSetting("Targets", 0, targetModes));
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(fov = new SliderSetting("FOV", "°", 360.0, 30.0, 360.0, 4.0));
        this.registerSetting(range = new SliderSetting("Range", 4.5, 3.0, 50.0, 0.05));
        this.registerSetting(sortMode = new SliderSetting("Sort mode", 0, sortModes));
        this.registerSetting(switchDelay = new SliderSetting("Switch delay", "ms", 200.0, 50.0, 1000.0, 25.0));
        this.registerSetting(switchTargets = new SliderSetting("Switch targets", 3.0, 1.0, 10.0, 1.0));
        this.registerSetting(fireDelay = new SliderSetting("Fire delay", "ms", 100.0, 25.0, 1000.0, 5.0));
        this.registerSetting(targetInvis = new ButtonSetting("Target invis", true));
        this.registerSetting(attackMobs = new ButtonSetting("Attack mobs", false));
        this.registerSetting(targetArmorStand = new ButtonSetting("Target armor stand", false));
        this.registerSetting(targetPlayer = new ButtonSetting("Target player", false));
        this.registerSetting(hitThroughWalls = new ButtonSetting("Hit through walls", false));
        this.registerSetting(hitThroughEntities = new ButtonSetting("Hit through entities", false));
        this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(prioritizeEnemies = new ButtonSetting("Prioritize enemies", false));
        this.registerSetting(targetDead = new ButtonSetting("Target dead", false));
    }

    @Override
    public void onEnable() {
        target = null;
        selectedAt = 0L;
        nextFireTime = 0L;
        switchIndex = 0;
        fireAfterMotion = false;
    }

    @Override
    public void onDisable() {
        target = null;
        fireAfterMotion = false;
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            heldGun = null;
            return;
        }
        heldGun = RPGUtility.getRPGGun(ItemStack.convert(mc.thePlayer.getHeldItem()));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreMotion(PreMotionEvent event) {
        fireAfterMotion = false;
        if (!Utils.nullCheck()) {
            target = null;
            return;
        }
        heldGun = RPGUtility.getRPGGun(ItemStack.convert(mc.thePlayer.getHeldItem()));
        if (heldGun == null || heldGun.bullet <= 0 || heldGun.reloading || mc.thePlayer.isDead) {
            target = null;
            return;
        }
        if ((int) targetMode.getInput() == 0) {
            target = null;
            event.setPitch(90.0F);
        } else {
            List<TargetData> candidates = findTargets();
            target = selectTarget(candidates);
            if (target == null) {
                return;
            }
            float[] rotations = RotationUtils.getRotationsWithBackup(target, 100.0D, 0.0D, event.getYaw(), event.getPitch(), range.getInput(), hitThroughWalls.isToggled(), hitThroughEntities.isToggled());
            if (rotations == null) {
                return;
            }
            rotations[1] = RotationUtils.clampPitch(rotations[1] - 3.0F);
            event.setRotations(rotations[0], rotations[1]);
        }
        if (System.currentTimeMillis() >= nextFireTime) {
            fireAfterMotion = true;
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent event) {
        if (!fireAfterMotion || !Utils.nullCheck()) {
            return;
        }
        fireAfterMotion = false;
        heldGun = RPGUtility.getRPGGun(ItemStack.convert(mc.thePlayer.getHeldItem()));
        if (heldGun == null || heldGun.bullet <= 0 || heldGun.reloading) {
            return;
        }
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        nextFireTime = System.currentTimeMillis() + (long) fireDelay.getInput();
    }

    private List<TargetData> findTargets() {
        List<TargetData> candidates = new ArrayList<>();
        double maxRange = range.getInput();
        float fovValue = (float) fov.getInput();
        for (Entity entity : mc.theWorld.getEntitiesWithinAABB(Entity.class, mc.thePlayer.getEntityBoundingBox().expand(maxRange, maxRange, maxRange))) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer) {
                continue;
            }
            EntityLivingBase livingEntity = (EntityLivingBase) entity;
            if (!targetDead.isToggled() && (livingEntity.isDead || livingEntity.deathTime != 0)) {
                continue;
            }
            if (entity instanceof EntityPlayer) {
                if (!targetPlayer.isToggled() || Utils.isFriended((EntityPlayer) entity) || AntiBot.isBot(entity)) {
                    continue;
                }
                if (ignoreTeammates.isToggled() && Utils.isTeammate(entity)) {
                    continue;
                }
            } else if (entity instanceof EntityArmorStand) {
                if (!targetArmorStand.isToggled()) {
                    continue;
                }
            } else if (!attackMobs.isToggled()) {
                continue;
            }
            if (entity.isInvisible() && !targetInvis.isToggled()) {
                continue;
            }
            if (fovValue != 360.0F && !Utils.inFov(fovValue, entity)) {
                continue;
            }
            double distance = RotationUtils.distanceFromEyeToClosestOnAABB(entity);
            if (distance > maxRange || !RotationUtils.hasValidAimPoint(entity, 100.0D, 0.0D, maxRange, hitThroughWalls.isToggled(), hitThroughEntities.isToggled())) {
                continue;
            }
            candidates.add(new TargetData(livingEntity, distance, livingEntity.getHealth(), livingEntity.hurtTime, RotationUtils.distanceFromYaw(entity, false), entity instanceof EntityPlayer && Utils.isEnemy((EntityPlayer) entity)));
        }
        if (prioritizeEnemies.isToggled()) {
            boolean hasEnemy = false;
            for (TargetData candidate : candidates) {
                if (candidate.enemy) {
                    hasEnemy = true;
                    break;
                }
            }
            if (hasEnemy) {
                candidates.removeIf(candidate -> !candidate.enemy);
            }
        }
        candidates.sort(getComparator());
        return candidates;
    }

    private EntityLivingBase selectTarget(List<TargetData> candidates) {
        if (candidates.isEmpty()) {
            switchIndex = 0;
            selectedAt = 0L;
            return null;
        }
        if ((int) mode.getInput() == 0) {
            switchIndex = 0;
            selectedAt = System.currentTimeMillis();
            return candidates.get(0).entity;
        }
        int limit = Math.min(candidates.size(), Math.max(1, (int) switchTargets.getInput()));
        long now = System.currentTimeMillis();
        boolean currentStillAvailable = target != null && switchIndex < limit && candidates.get(switchIndex).entity == target;
        if (!currentStillAvailable) {
            switchIndex = 0;
            selectedAt = now;
        } else if (now - selectedAt >= (long) switchDelay.getInput()) {
            switchIndex = (switchIndex + 1) % limit;
            selectedAt = now;
        }
        return candidates.get(switchIndex).entity;
    }

    private Comparator<TargetData> getComparator() {
        switch ((int) sortMode.getInput()) {
            case 1:
                return Comparator.comparingDouble(candidate -> candidate.health);
            case 2:
                return Comparator.comparingInt(candidate -> candidate.hurtTime);
            case 3:
                return Comparator.comparingDouble(candidate -> candidate.yaw);
            default:
                return Comparator.comparingDouble(candidate -> candidate.distance);
        }
    }

    @Override
    public String getInfo() {
        if (heldGun == null) {
            return "";
        }
        return heldGun.bullet + (heldGun.reloading ? "R" : "");
    }

    public RPGGun getHeldGun() {
        return heldGun;
    }

    private static final class TargetData {
        private final EntityLivingBase entity;
        private final double distance;
        private final float health;
        private final int hurtTime;
        private final double yaw;
        private final boolean enemy;

        private TargetData(EntityLivingBase entity, double distance, float health, int hurtTime, double yaw, boolean enemy) {
            this.entity = entity;
            this.distance = distance;
            this.health = health;
            this.hurtTime = hurtTime;
            this.yaw = yaw;
            this.enemy = enemy;
        }
    }
}
