package dev.muzu1.possession.form;

import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;

public interface PossessionMovementBehavior {
    default void afterPlayerLikeJump(LivingEntity target, PossessionControlState control) {
    }

    default boolean overridesJumpHandling() {
        return false;
    }

    default boolean handleJumpInput(LivingEntity target, PossessionControlState control) {
        return false;
    }

    default boolean requiresJumpReleaseBetweenJumps(LivingEntity target) {
        return false;
    }

    default void applyExtraMovement(LivingEntity target, PossessionControlState control) {
    }
}
