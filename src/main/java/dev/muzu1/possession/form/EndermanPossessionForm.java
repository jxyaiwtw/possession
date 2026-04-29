package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class EndermanPossessionForm extends ConfigBackedPossessionForm {
    public static final EndermanPossessionForm INSTANCE = new EndermanPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.06D, 0.10D, null, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(2.8D, 0.12D);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback ACTIONS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback(0, 0, null, 16, 10);

    private EndermanPossessionForm() {
        super("enderman");
    }

    @Override
    public boolean handleAttack(LivingEntity attacker, int attackTargetEntityId, PossessionControlState control) {
        if (attacker instanceof EndermanEntity enderman && control.hasBlockTarget()) {
            return pickUpBlock(enderman, control.blockTargetPos());
        }
        return false;
    }

    @Override
    public boolean handleUse(LivingEntity target, PossessionControlState control) {
        if (!(target instanceof EndermanEntity enderman) || !control.hasBlockTarget()) {
            return false;
        }
        return placeCarriedBlock(enderman, control.blockTargetPos().offset(control.blockTargetSide()));
    }

    @Override
    public boolean supportsAbility() {
        return true;
    }

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback movementFallback() {
        return MOVEMENT;
    }

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback statFallback() {
        return STATS;
    }

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback actionFallback() {
        return ACTIONS;
    }

    @Override
    public boolean handleAbility(LivingEntity target, PossessionControlState control) {
        PossessionConfig.EndermanBehavior behavior = PossessionConfig.BEHAVIORS.enderman;
        Vec3d look = safeTeleportLook(control);
        Vec3d destination = target.getPos().add(look.multiply(behavior.teleportDistance));
        Vec3d safeDestination = findSafeTeleportDestination(target, destination);
        if (safeDestination == null) {
            return false;
        }
        PossessionEffects.around(target, ParticleTypes.PORTAL, behavior.portalParticleCount, behavior.portalParticleSpread, behavior.portalParticleVelocity);
        target.requestTeleport(safeDestination.x, safeDestination.y, safeDestination.z);
        target.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, behavior.teleportSoundVolume, behavior.teleportSoundPitch);
        PossessionEffects.around(target, ParticleTypes.PORTAL, behavior.portalParticleCount, behavior.portalParticleSpread, behavior.portalParticleVelocity);
        return true;
    }

    private static Vec3d safeTeleportLook(PossessionControlState control) {
        PossessionConfig.EndermanBehavior behavior = PossessionConfig.BEHAVIORS.enderman;
        float clampedPitch = MathHelper.clamp(control.pitch(), behavior.minTeleportPitch, behavior.maxTeleportPitch);
        double yawRadians = Math.toRadians(control.yaw());
        double pitchRadians = Math.toRadians(clampedPitch);
        double x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double y = -Math.sin(pitchRadians);
        double z = Math.cos(yawRadians) * Math.cos(pitchRadians);
        return new Vec3d(x, y, z).normalize();
    }

    private static Vec3d findSafeTeleportDestination(LivingEntity target, Vec3d desired) {
        PossessionConfig.EndermanBehavior behavior = PossessionConfig.BEHAVIORS.enderman;
        World world = target.getWorld();
        BlockPos desiredPos = BlockPos.ofFloored(desired);
        for (int horizontalRadius = 0; horizontalRadius <= behavior.searchHorizontalRadius; horizontalRadius++) {
            for (int xOffset = -horizontalRadius; xOffset <= horizontalRadius; xOffset++) {
                for (int zOffset = -horizontalRadius; zOffset <= horizontalRadius; zOffset++) {
                    Vec3d candidate = findSafeTeleportHeight(target, world, desiredPos.add(xOffset, 0, zOffset));
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static Vec3d findSafeTeleportHeight(LivingEntity target, World world, BlockPos basePos) {
        PossessionConfig.EndermanBehavior behavior = PossessionConfig.BEHAVIORS.enderman;
        for (int yOffset = behavior.searchUp; yOffset >= -behavior.searchDown; yOffset--) {
            Vec3d candidate = new Vec3d(basePos.getX() + 0.5D, basePos.getY() + yOffset, basePos.getZ() + 0.5D);
            BlockPos supportPos = BlockPos.ofFloored(candidate).down();
            if (!world.getBlockState(supportPos).isSolidBlock(world, supportPos)) {
                continue;
            }

            Vec3d offset = candidate.subtract(target.getPos());
            if (world.isSpaceEmpty(target, target.getBoundingBox().offset(offset))) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean pickUpBlock(EndermanEntity enderman, BlockPos pos) {
        PossessionConfig.EndermanBehavior behavior = PossessionConfig.BEHAVIORS.enderman;
        if (enderman.getCarriedBlock() != null) {
            return false;
        }

        BlockState state = enderman.getWorld().getBlockState(pos);
        if (state.isAir() || state.getHardness(enderman.getWorld(), pos) < 0.0F) {
            return false;
        }

        enderman.setCarriedBlock(state);
        enderman.getWorld().breakBlock(pos, false, enderman);
        enderman.playSound(SoundEvents.ENTITY_ENDERMAN_SCREAM, behavior.pickupSoundVolume, behavior.pickupSoundPitch);
        PossessionEffects.around(enderman, ParticleTypes.REVERSE_PORTAL, behavior.blockParticleCount, behavior.blockParticleSpread, behavior.blockParticleVelocity);
        return true;
    }

    private static boolean placeCarriedBlock(EndermanEntity enderman, BlockPos pos) {
        PossessionConfig.EndermanBehavior behavior = PossessionConfig.BEHAVIORS.enderman;
        BlockState carried = enderman.getCarriedBlock();
        if (carried == null || !enderman.getWorld().isAir(pos) || !carried.canPlaceAt(enderman.getWorld(), pos)) {
            return false;
        }

        if (!enderman.getWorld().setBlockState(pos, carried, Block.NOTIFY_ALL)) {
            return false;
        }

        enderman.setCarriedBlock(null);
        enderman.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, behavior.placeSoundVolume, behavior.placeSoundPitch);
        PossessionEffects.around(enderman, ParticleTypes.PORTAL, behavior.blockParticleCount, behavior.blockParticleSpread, behavior.blockParticleVelocity);
        return true;
    }
}
