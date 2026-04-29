package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GoatPossessionForm extends ConfigBackedPossessionForm {
    public static final GoatPossessionForm INSTANCE = new GoatPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.06D, 0.12D, null, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(1.8D, 0.22D);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback ACTIONS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback(0, 0, null, 6, 8);
    private final Map<UUID, RamState> activeRams = new HashMap<>();

    private GoatPossessionForm() {
        super("goat");
    }

    @Override
    public void applyExtraMovement(LivingEntity target, PossessionControlState control) {
        tickActiveRam(target);
    }

    @Override
    public void afterPlayerLikeJump(LivingEntity target, PossessionControlState control) {
        if (!control.sprint() || control.forward() <= 0.0F) {
            return;
        }

        double radians = Math.toRadians(control.yaw());
        Vec3d velocity = target.getVelocity();
        target.setVelocity(
                velocity.x - Math.sin(radians) * 0.22D,
                Math.max(velocity.y, 0.45D),
                velocity.z + Math.cos(radians) * 0.22D
        );
    }

    @Override
    public boolean overridesJumpHandling() {
        return true;
    }

    @Override
    public boolean handleJumpInput(LivingEntity target, PossessionControlState control) {
        if (!control.jumpReleased() || control.jumpHoldTicks() <= 0 || !target.isOnGround()) {
            return false;
        }

        double chargeRatio = getChargeRatio(control.jumpHoldTicks());
        Vec3d direction = movementDirection(control);
        Vec3d velocity = target.getVelocity();
        double verticalVelocity = MathHelper.lerp(chargeRatio, 0.42D, PossessionConfig.GOAT.maxJumpVerticalVelocity) + target.getJumpBoostVelocityModifier();
        double horizontalVelocity = MathHelper.lerp(chargeRatio, PossessionConfig.GOAT.minJumpHorizontalVelocity, PossessionConfig.GOAT.maxJumpHorizontalVelocity);

        target.setVelocity(
                velocity.x * 0.12D + direction.x * horizontalVelocity,
                verticalVelocity,
                velocity.z * 0.12D + direction.z * horizontalVelocity
        );
        target.setOnGround(false);
        target.velocityDirty = true;
        target.swingHand(Hand.MAIN_HAND);
        target.playSound(SoundEvents.ENTITY_GOAT_LONG_JUMP, 1.0F, 0.9F + (float) chargeRatio * 0.2F);
        PossessionEffects.around(target, ParticleTypes.CLOUD, 12 + (int) Math.round(chargeRatio * 14.0D), 0.4D, 0.03D);
        return true;
    }

    @Override
    public boolean supportsAbility() {
        return true;
    }

    @Override
    public boolean overridesAbilityHandling() {
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
    public boolean handleAbilityInput(LivingEntity target, PossessionControlState control) {
        if (control.abilityPressed()) {
            target.playSound(SoundEvents.ENTITY_GOAT_PREPARE_RAM, 0.9F, 1.0F);
        }

        if (!control.abilityReleased() || control.abilityHoldTicks() <= 0 || !target.isOnGround()) {
            return false;
        }

        double chargeRatio = getAbilityChargeRatio(control.abilityHoldTicks());
        Vec3d direction = facingDirection(control.yaw());
        double ramDistance = MathHelper.lerp(chargeRatio, 0.0D, PossessionConfig.GOAT.maxRamDistance);
        double ramSpeed = MathHelper.lerp(chargeRatio, PossessionConfig.GOAT.minRamSpeed, PossessionConfig.GOAT.maxRamSpeed);
        float damage = Math.max(1.0F, (float) (chargeRatio * PossessionConfig.GOAT.maxRamDamage));
        Vec3d velocity = target.getVelocity();

        target.setVelocity(direction.x * ramSpeed, Math.max(velocity.y, PossessionConfig.GOAT.ramVerticalLift), direction.z * ramSpeed);
        target.velocityDirty = true;
        target.swingHand(Hand.MAIN_HAND);
        target.playSound(SoundEvents.ENTITY_GOAT_RAM_IMPACT, 1.0F, 0.92F + (float) chargeRatio * 0.16F);
        PossessionEffects.around(target, ParticleTypes.CLOUD, 16 + (int) Math.round(chargeRatio * 20.0D), 0.55D, 0.04D);
        activeRams.put(target.getUuid(), new RamState(
                direction,
                damage,
                chargeRatio,
                ramSpeed,
                MathHelper.ceil(MathHelper.lerp(chargeRatio, PossessionConfig.GOAT.ramMinTicks, PossessionConfig.GOAT.ramMaxTicks)),
                ramDistance,
                target.getPos(),
                ramDistance
        ));
        return true;
    }

    @Override
    public void onPossessionEnded(LivingEntity target) {
        finishRam(target);
    }

    private void tickActiveRam(LivingEntity goat) {
        RamState ramState = activeRams.get(goat.getUuid());
        if (ramState == null) {
            return;
        }

        double traveledDistance = horizontalDistance(goat.getPos(), ramState.startPos);
        if (ramState.remainingTicks <= 0 || ramState.remainingDistance <= 0.0D || traveledDistance >= ramState.maxDistance || !goat.isAlive() || goat.isRemoved()) {
            finishRam(goat);
            return;
        }

        Vec3d velocity = goat.getVelocity();
        goat.setVelocity(
                ramState.direction.x * ramState.speed,
                Math.max(velocity.y, PossessionConfig.GOAT.ramVerticalLift),
                ramState.direction.z * ramState.speed
        );
        goat.velocityDirty = true;
        Vec3d activeVelocity = goat.getVelocity();
        double travelDistance = Math.max(PossessionConfig.GOAT.ramMinTrace, Math.min(PossessionConfig.GOAT.ramMaxTrace, activeVelocity.horizontalLength() + 0.2D));
        double traceDistance = Math.min(travelDistance, ramState.remainingDistance);
        LivingEntity victim = findRamVictim(goat, ramState.direction, traceDistance);
        if (victim != null) {
            victim.damage(goat.getWorld().getDamageSources().mobAttack(goat), ramState.damage);
            victim.takeKnockback(0.8D + ramState.chargeRatio * 1.4D, -ramState.direction.x, -ramState.direction.z);
            goat.playSound(SoundEvents.ENTITY_GOAT_RAM_IMPACT, 1.0F, 0.85F + (float) ramState.chargeRatio * 0.2F);
            PossessionEffects.around(victim, ParticleTypes.CLOUD, 12 + (int) Math.round(ramState.chargeRatio * 10.0D), 0.45D, 0.03D);
            finishRam(goat);
            return;
        }

        if (ramState.remainingTicks <= 1 || activeVelocity.horizontalLengthSquared() < 0.04D) {
            finishRam(goat);
            return;
        }

        activeRams.put(goat.getUuid(), ramState.next(traceDistance));
    }

    private void finishRam(LivingEntity goat) {
        activeRams.remove(goat.getUuid());
        Vec3d velocity = goat.getVelocity();
        goat.setVelocity(velocity.x * 0.12D, Math.min(velocity.y, 0.08D), velocity.z * 0.12D);
        goat.velocityDirty = true;
    }

    private static double horizontalDistance(Vec3d from, Vec3d to) {
        double dx = from.x - to.x;
        double dz = from.z - to.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static LivingEntity findRamVictim(LivingEntity goat, Vec3d direction, double distance) {
        if (distance <= 0.0D) {
            return null;
        }

        Vec3d start = goat.getPos();
        Vec3d end = start.add(direction.multiply(distance));
        Box ramArea = goat.getBoundingBox().stretch(direction.multiply(distance)).expand(PossessionConfig.GOAT.ramHitExpand);
        double bestDistance = Double.MAX_VALUE;
        LivingEntity bestVictim = null;
        for (Entity entity : goat.getWorld().getOtherEntities(goat, ramArea, candidate -> candidate instanceof LivingEntity living
                && living.isAlive()
                && !living.isRemoved())) {
            if (!(entity instanceof LivingEntity victim)) {
                continue;
            }

            if (!victim.getBoundingBox().expand(0.35D).raycast(start, end).isPresent()) {
                continue;
            }

            double victimDistance = goat.squaredDistanceTo(victim);
            if (victimDistance < bestDistance) {
                bestDistance = victimDistance;
                bestVictim = victim;
            }
        }
        return bestVictim;
    }

    private static Vec3d movementDirection(PossessionControlState control) {
        double yawRadians = Math.toRadians(control.yaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3d left = new Vec3d(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3d direction = forward.multiply(control.forward()).add(left.multiply(control.sideways()));
        if (direction.lengthSquared() < 1.0E-4D) {
            return forward;
        }
        return direction.normalize();
    }

    private static Vec3d facingDirection(float yaw) {
        double yawRadians = Math.toRadians(yaw);
        return new Vec3d(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians)).normalize();
    }

    private static double getChargeRatio(int holdTicks) {
        return MathHelper.clamp(holdTicks / (double) PossessionConfig.GOAT.maxChargeTicks, 0.0D, 1.0D);
    }

    private static double getAbilityChargeRatio(int holdTicks) {
        double linearRatio = getChargeRatio(holdTicks);
        return 1.0D - Math.pow(1.0D - linearRatio, 3.0D);
    }

    private record RamState(Vec3d direction,
                            float damage,
                            double chargeRatio,
                            double speed,
                            int remainingTicks,
                            double remainingDistance,
                            Vec3d startPos,
                            double maxDistance) {
        private RamState next(double tracedDistance) {
            return new RamState(direction, damage, chargeRatio, speed, remainingTicks - 1, Math.max(0.0D, remainingDistance - tracedDistance), startPos, maxDistance);
        }
    }
}
