package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;

abstract class ConfigBackedPossessionForm implements PossessionForm {
    private static final PossessionConfig.FormProfile.ActionFallback DEFAULT_ACTION_FALLBACK =
            new PossessionConfig.FormProfile.ActionFallback(0, 0, null, 0, 8);

    private final String profileKey;

    protected ConfigBackedPossessionForm(String profileKey) {
        this.profileKey = profileKey;
    }

    protected final PossessionConfig.FormProfile profile() {
        return PossessionConfig.FORMS.profile(profileKey);
    }

    protected abstract PossessionConfig.FormProfile.MovementFallback movementFallback();

    protected abstract PossessionConfig.FormProfile.StatFallback statFallback();

    protected PossessionConfig.FormProfile.ActionFallback actionFallback() {
        return DEFAULT_ACTION_FALLBACK;
    }

    @Override
    public double movementSpeedBonus(LivingEntity target, PossessionControlState control) {
        return profile().resolveMovementSpeedBonus(target, control, movementFallback());
    }

    @Override
    public double attackDamageBonus(LivingEntity target) {
        return profile().resolveAttackDamageBonus(statFallback());
    }

    @Override
    public double knockbackResistanceBonus(LivingEntity target) {
        return profile().resolveKnockbackResistanceBonus(statFallback());
    }

    @Override
    public int attackWindupTicks(LivingEntity target, PossessionControlState control) {
        return profile().resolveAttackWindupTicks(actionFallback());
    }

    @Override
    public int attackCooldownTicks(LivingEntity target, PossessionControlState control) {
        return profile().resolveAttackCooldownTicks(control, actionFallback());
    }

    @Override
    public int abilityCooldownTicks(LivingEntity target, PossessionControlState control) {
        return profile().resolveAbilityCooldownTicks(actionFallback());
    }

    @Override
    public int useCooldownTicks(LivingEntity target, PossessionControlState control) {
        return profile().resolveUseCooldownTicks(actionFallback());
    }
}
