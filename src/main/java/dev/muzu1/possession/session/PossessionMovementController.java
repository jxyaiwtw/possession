package dev.muzu1.possession.session;

import dev.muzu1.possession.control.PossessionControlState;
import dev.muzu1.possession.form.PossessionMovementBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

final class PossessionMovementController {
    private static final float PLAYER_LIKE_MOVEMENT_SPEED_SCALE = 0.42F;
    private static final float PLAYER_LIKE_MIN_MOVEMENT_SPEED = 0.08F;
    private static final float PLAYER_LIKE_MAX_MOVEMENT_SPEED = 0.115F;
    private static final float INPUT_ACCELERATION_PER_TICK = 0.42F;
    private static final float INPUT_DECELERATION_PER_TICK = 0.55F;
    private static final float INPUT_DEAD_ZONE = 0.02F;
    private static final double WATER_ASCEND_SPEED = 0.16D;
    private static final double WATER_DESCEND_SPEED = -0.16D;
    private static final double WATER_VERTICAL_ACCELERATION_PER_TICK = 0.055D;
    private static final double AQUATIC_CONTROL_BLEND = 0.82D;
    private static final double AQUATIC_BASE_SPEED = 0.17D;
    private static final double AQUATIC_DOLPHIN_SPEED = 0.22D;
    private static final double AQUATIC_PITCH_VERTICAL_SCALE = 0.12D;
    private static final double FLY_DESCEND_SPEED = -0.18D;
    private static final double FLY_ASCEND_SPEED = 0.22D;
    private static final double FLY_VERTICAL_ACCELERATION_PER_TICK = 0.075D;
    private static final double FREE_FLIGHT_BASE_SPEED = 0.19D;
    private static final double FREE_FLIGHT_SPRINT_SPEED = 0.27D;
    private static final double PLAYER_LIKE_JUMP_VELOCITY = 0.42D;
    private static final double PLAYER_LIKE_SPRINT_JUMP_BOOST = 0.20D;

    private float smoothedForward;
    private float smoothedSideways;
    private boolean jumpRepeatReleased = true;

    void syncFacing(LivingEntity target, PossessionControlState control) {
        target.setYaw(control.yaw());
        target.setPitch(control.pitch());
        target.prevYaw = control.yaw();
        target.prevPitch = control.pitch();
        if (target instanceof MobEntity mob) {
            mob.setHeadYaw(control.yaw());
            mob.setBodyYaw(control.yaw());
        }
    }

    boolean applyMovement(LivingEntity target, PossessionControlState control, PossessionMovementBehavior form, boolean bufferedJump) {
        if (!control.jump()) {
            jumpRepeatReleased = true;
        }
        boolean hasHorizontalInput = Math.abs(control.forward()) > 0.01F || Math.abs(control.sideways()) > 0.01F;
        boolean hasVerticalInput = control.jump() || control.sneak();
        target.setSneaking(control.sneak());
        target.setSprinting(control.sprint());
        smoothMovementInput(control);

        float movementSpeed = MathHelper.clamp(
                (float) target.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * PLAYER_LIKE_MOVEMENT_SPEED_SCALE,
                PLAYER_LIKE_MIN_MOVEMENT_SPEED,
                PLAYER_LIKE_MAX_MOVEMENT_SPEED
        );
        if (control.sneak()) {
            movementSpeed *= 0.3F;
        }
        if (control.sprint()) {
            movementSpeed *= 1.3F;
        }
        target.setMovementSpeed(movementSpeed);

        if (target instanceof MobEntity mob) {
            mob.setMovementSpeed(movementSpeed);
            mob.setForwardSpeed(smoothedForward);
            mob.setSidewaysSpeed(smoothedSideways);
            mob.setUpwardSpeed(0.0F);
        }

        Vec3d movementInput = new Vec3d(smoothedSideways, 0.0D, smoothedForward);
        boolean inFluid = target.isTouchingWater() || target.isSubmergedInWater();
        boolean canAirControlVertical = supportsFreeFlight(target);
        boolean shouldPhaseThroughBlocks = target.getType() == EntityType.VEX;
        target.noClip = shouldPhaseThroughBlocks;
        if (canAirControlVertical && !inFluid) {
            target.setNoGravity(true);
        }
        if (canAirControlVertical) {
            target.fallDistance = 0.0F;
        }

        boolean jumpConsumed = false;
        if (form.overridesJumpHandling()) {
            jumpConsumed = form.handleJumpInput(target, control);
            if (jumpConsumed && form.requiresJumpReleaseBetweenJumps(target)) {
                jumpRepeatReleased = false;
            }
        }

        boolean jumpCanRepeat = !form.requiresJumpReleaseBetweenJumps(target) || jumpRepeatReleased;
        boolean shouldJump = !canAirControlVertical
                && !form.overridesJumpHandling()
                && jumpCanRepeat
                && (control.jump() || bufferedJump)
                && target.isOnGround();
        if (shouldJump) {
            performPlayerLikeJump(target, control, form);
            jumpConsumed = true;
            if (form.requiresJumpReleaseBetweenJumps(target)) {
                jumpRepeatReleased = false;
            }
        }

        if (!inFluid && !canAirControlVertical) {
            target.travel(movementInput);
        }

        Vec3d updatedVelocity = target.getVelocity();
        if (inFluid && PossessionTargetController.isAquaticPossessionTarget(target)) {
            applyAquaticVelocity(target, control);
            updatedVelocity = target.getVelocity();
        }

        if (inFluid) {
            double verticalVelocity = updatedVelocity.y * 0.86D;
            if (control.jump()) {
                verticalVelocity = approach(updatedVelocity.y * 0.86D, WATER_ASCEND_SPEED, WATER_VERTICAL_ACCELERATION_PER_TICK);
            } else if (control.sneak()) {
                verticalVelocity = approach(updatedVelocity.y * 0.86D, WATER_DESCEND_SPEED, WATER_VERTICAL_ACCELERATION_PER_TICK);
            }
            target.setVelocity(updatedVelocity.x, verticalVelocity, updatedVelocity.z);
        } else if (canAirControlVertical) {
            applyFreeFlightVelocity(target, control);
        }

        if (!hasHorizontalInput && !hasVerticalInput) {
            suppressAutonomousMotion(target, inFluid, canAirControlVertical);
        } else if (!hasHorizontalInput && target.isOnGround() && !inFluid && !canAirControlVertical) {
            dampGroundDrift(target);
        }

        form.applyExtraMovement(target, control);
        return jumpConsumed;
    }

    private void performPlayerLikeJump(LivingEntity target, PossessionControlState control, PossessionMovementBehavior form) {
        Vec3d velocity = target.getVelocity();
        double jumpVelocity = PLAYER_LIKE_JUMP_VELOCITY + target.getJumpBoostVelocityModifier();
        double x = velocity.x;
        double z = velocity.z;

        if (control.sprint()) {
            double radians = Math.toRadians(control.yaw());
            x += -Math.sin(radians) * PLAYER_LIKE_SPRINT_JUMP_BOOST;
            z += Math.cos(radians) * PLAYER_LIKE_SPRINT_JUMP_BOOST;
        }

        target.setVelocity(x, jumpVelocity, z);
        target.setOnGround(false);
        form.afterPlayerLikeJump(target, control);
        target.velocityDirty = true;
    }

    private void applyAquaticVelocity(LivingEntity target, PossessionControlState control) {
        double yawRadians = Math.toRadians(control.yaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3d left = new Vec3d(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));

        double vertical = 0.0D;
        if (control.jump()) {
            vertical += 1.0D;
        }
        if (control.sneak()) {
            vertical -= 1.0D;
        }
        if (smoothedForward != 0.0F) {
            vertical += -Math.sin(Math.toRadians(control.pitch())) * Math.abs(smoothedForward) * AQUATIC_PITCH_VERTICAL_SCALE;
        }

        Vec3d desired = forward.multiply(smoothedForward)
                .add(left.multiply(smoothedSideways))
                .add(0.0D, vertical, 0.0D);
        if (desired.lengthSquared() > 1.0D) {
            desired = desired.normalize();
        }

        double speed = target.getType() == EntityType.DOLPHIN ? AQUATIC_DOLPHIN_SPEED : AQUATIC_BASE_SPEED;
        if (control.sprint()) {
            speed *= 1.25D;
        }

        Vec3d current = target.getVelocity();
        Vec3d desiredVelocity = desired.multiply(speed);
        target.setVelocity(current.multiply(1.0D - AQUATIC_CONTROL_BLEND).add(desiredVelocity.multiply(AQUATIC_CONTROL_BLEND)));
        target.velocityDirty = true;
    }

    private void applyFreeFlightVelocity(LivingEntity target, PossessionControlState control) {
        double yawRadians = Math.toRadians(control.yaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3d left = new Vec3d(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3d desired = forward.multiply(smoothedForward).add(left.multiply(smoothedSideways));
        if (desired.lengthSquared() > 1.0D) {
            desired = desired.normalize();
        }

        double verticalVelocity = target.getVelocity().y;
        if (control.jump()) {
            verticalVelocity = approach(verticalVelocity, FLY_ASCEND_SPEED, FLY_VERTICAL_ACCELERATION_PER_TICK);
        } else if (control.sneak()) {
            verticalVelocity = approach(verticalVelocity, FLY_DESCEND_SPEED, FLY_VERTICAL_ACCELERATION_PER_TICK);
        } else {
            verticalVelocity = approach(verticalVelocity, 0.0D, FLY_VERTICAL_ACCELERATION_PER_TICK * 0.65D);
        }

        double speed = control.sprint() ? FREE_FLIGHT_SPRINT_SPEED : FREE_FLIGHT_BASE_SPEED;
        Vec3d horizontalVelocity = desired.multiply(speed);
        target.setVelocity(horizontalVelocity.x, verticalVelocity, horizontalVelocity.z);
        target.velocityDirty = true;
    }

    private void smoothMovementInput(PossessionControlState control) {
        float desiredForward = control.forward();
        float desiredSideways = control.sideways();
        float length = MathHelper.sqrt(desiredForward * desiredForward + desiredSideways * desiredSideways);
        if (length > 1.0F) {
            desiredForward /= length;
            desiredSideways /= length;
        }

        smoothedForward = approachInput(smoothedForward, desiredForward);
        smoothedSideways = approachInput(smoothedSideways, desiredSideways);
    }

    private static float approachInput(float current, float target) {
        float step = Math.abs(target) > Math.abs(current) ? INPUT_ACCELERATION_PER_TICK : INPUT_DECELERATION_PER_TICK;
        float value = approach(current, target, step);
        return Math.abs(value) < INPUT_DEAD_ZONE ? 0.0F : value;
    }

    private static float approach(float current, float target, float maxStep) {
        if (current < target) {
            return Math.min(current + maxStep, target);
        }
        return Math.max(current - maxStep, target);
    }

    private static double approach(double current, double target, double maxStep) {
        if (current < target) {
            return Math.min(current + maxStep, target);
        }
        return Math.max(current - maxStep, target);
    }

    private static void suppressAutonomousMotion(LivingEntity target, boolean inFluid, boolean canAirControlVertical) {
        Vec3d velocity = target.getVelocity();
        if (inFluid || canAirControlVertical) {
            double vertical = canAirControlVertical
                    ? (Math.abs(velocity.y) < 0.02D ? 0.0D : approach(velocity.y, 0.0D, 0.08D))
                    : velocity.y;
            target.setVelocity(velocity.x * 0.06D, vertical, velocity.z * 0.06D);
            target.velocityDirty = true;
            return;
        }

        dampGroundDrift(target);
    }

    private static void dampGroundDrift(LivingEntity target) {
        Vec3d velocity = target.getVelocity();
        target.setVelocity(velocity.x * 0.08D, velocity.y, velocity.z * 0.08D);
        target.velocityDirty = true;
    }

    private static boolean supportsFreeFlight(LivingEntity target) {
        EntityType<?> type = target.getType();
        return target.hasNoGravity()
                || type == EntityType.ALLAY
                || type == EntityType.BAT
                || type == EntityType.BEE
                || type == EntityType.BLAZE
                || type == EntityType.ENDER_DRAGON
                || type == EntityType.GHAST
                || type == EntityType.PARROT
                || type == EntityType.PHANTOM
                || type == EntityType.VEX
                || type == EntityType.WITHER;
    }
}
