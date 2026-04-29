package dev.muzu1.possession.form;

import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;

public interface PossessionActionBehavior {
    default int attackWindupTicks(LivingEntity target, PossessionControlState control) {
        return 0;
    }

    default int attackCooldownTicks(LivingEntity target, PossessionControlState control) {
        return 0;
    }

    default void onAttackQueued(LivingEntity attacker, int attackTargetEntityId, PossessionControlState control) {
    }

    default boolean handleAttack(LivingEntity attacker, int attackTargetEntityId, PossessionControlState control) {
        return false;
    }

    default boolean supportsAbility() {
        return false;
    }

    default boolean overridesAbilityHandling() {
        return false;
    }

    default int abilityCooldownTicks(LivingEntity target, PossessionControlState control) {
        return 0;
    }

    default boolean handleAbility(LivingEntity target, PossessionControlState control) {
        return false;
    }

    default boolean handleAbilityInput(LivingEntity target, PossessionControlState control) {
        return false;
    }

    default int useCooldownTicks(LivingEntity target, PossessionControlState control) {
        return 8;
    }

    default boolean handleUse(LivingEntity target, PossessionControlState control) {
        return false;
    }
}
