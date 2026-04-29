package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FrogPossessionForm extends ConfigBackedPossessionForm {
    public static final FrogPossessionForm INSTANCE = new FrogPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.06D, null, null, 0.10D, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(1.0D, 0.08D);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback ACTIONS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback(0, 0, null, 20, 8);

    private final Map<UUID, NbtCompound> swallowedEntitiesByFrog = new HashMap<>();

    private FrogPossessionForm() {
        super("frog");
    }

    @Override
    public void applyExtraMovement(LivingEntity target, PossessionControlState control) {
    }

    @Override
    public void afterPlayerLikeJump(LivingEntity target, PossessionControlState control) {
        if (!control.sprint() || control.forward() <= 0.0F) {
            return;
        }

        PossessionConfig.FrogBehavior behavior = PossessionConfig.BEHAVIORS.frog;
        double radians = Math.toRadians(control.yaw());
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        Vec3d velocity = target.getVelocity();
        target.setVelocity(
                velocity.x + forwardX * behavior.sprintJumpBoost,
                Math.max(velocity.y, behavior.minSprintJumpVerticalVelocity),
                velocity.z + forwardZ * behavior.sprintJumpBoost
        );
        target.playSound(SoundEvents.ENTITY_FROG_LONG_JUMP, 0.8F, 1.0F);
    }

    @Override
    public boolean requiresJumpReleaseBetweenJumps(LivingEntity target) {
        return true;
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
        UUID frogUuid = target.getUuid();
        NbtCompound swallowedEntity = swallowedEntitiesByFrog.get(frogUuid);
        if (swallowedEntity != null) {
            return spitOut(target, swallowedEntity, true);
        }

        return swallowFocusedEntity(target, control);
    }

    @Override
    public void onPossessionEnded(LivingEntity target) {
        NbtCompound swallowedEntity = swallowedEntitiesByFrog.get(target.getUuid());
        if (swallowedEntity != null) {
            spitOut(target, swallowedEntity, true);
        }
    }

    private boolean swallowFocusedEntity(LivingEntity frog, PossessionControlState control) {
        Entity entity = frog.getWorld().getEntityById(control.focusTargetEntityId());
        if (!(entity instanceof LivingEntity victim) || !canSwallow(frog, victim)) {
            return false;
        }

        NbtCompound victimNbt = new NbtCompound();
        if (!victim.saveNbt(victimNbt)) {
            return false;
        }

        swallowedEntitiesByFrog.put(frog.getUuid(), victimNbt);
        victim.discard();
        frog.swingHand(Hand.MAIN_HAND);
        frog.playSound(SoundEvents.ENTITY_FROG_TONGUE, 1.0F, 1.0F);
        frog.playSound(SoundEvents.ENTITY_FROG_EAT, 1.0F, 1.0F);
        spawnEatParticles(frog);
        return true;
    }

    private boolean spitOut(LivingEntity frog, NbtCompound swallowedEntity, boolean removeFromStomach) {
        PossessionConfig.FrogBehavior behavior = PossessionConfig.BEHAVIORS.frog;
        Optional<Entity> restoredEntity = EntityType.getEntityFromNbt(swallowedEntity.copy(), frog.getWorld());
        if (restoredEntity.isEmpty()) {
            swallowedEntitiesByFrog.remove(frog.getUuid());
            return false;
        }

        Entity entity = restoredEntity.get();
        Vec3d look = frog.getRotationVec(1.0F).normalize();
        double x = frog.getX() + look.x * behavior.spitForwardOffset;
        double y = frog.getY() + behavior.spitUpOffset;
        double z = frog.getZ() + look.z * behavior.spitForwardOffset;
        entity.refreshPositionAndAngles(x, y, z, frog.getYaw(), frog.getPitch());
        entity.setVelocity(look.x * behavior.spitForwardSpeed, behavior.spitUpSpeed, look.z * behavior.spitForwardSpeed);

        if (entity instanceof LivingEntity living && living.getHealth() <= 0.0F) {
            living.setHealth(1.0F);
        }

        if (!frog.getWorld().spawnEntity(entity)) {
            return false;
        }

        if (removeFromStomach) {
            swallowedEntitiesByFrog.remove(frog.getUuid());
        }
        frog.swingHand(Hand.MAIN_HAND);
        frog.playSound(SoundEvents.ENTITY_FROG_EAT, 1.0F, 0.75F);
        spawnEatParticles(frog);
        return true;
    }

    private static boolean canSwallow(LivingEntity frog, LivingEntity victim) {
        if (victim == frog || victim instanceof PlayerEntity || !victim.isAlive() || victim.isRemoved()) {
            return false;
        }
        if (victim.hasPassengers() || victim.hasVehicle()) {
            return false;
        }
        if (victim.getType() == EntityType.ENDER_DRAGON || victim.getType() == EntityType.WITHER) {
            return false;
        }
        double swallowRange = PossessionConfig.BEHAVIORS.frog.swallowRange;
        return frog.squaredDistanceTo(victim) <= swallowRange * swallowRange;
    }

    private static void spawnEatParticles(LivingEntity frog) {
        if (frog.getWorld() instanceof ServerWorld serverWorld) {
            PossessionConfig.FrogBehavior behavior = PossessionConfig.BEHAVIORS.frog;
            serverWorld.spawnParticles(
                    ParticleTypes.POOF,
                    frog.getX(),
                    frog.getBodyY(0.55D),
                    frog.getZ(),
                    behavior.eatParticleCount,
                    behavior.eatParticleSpreadX,
                    behavior.eatParticleSpreadY,
                    behavior.eatParticleSpreadZ,
                    behavior.eatParticleVelocity
            );
        }
    }
}
