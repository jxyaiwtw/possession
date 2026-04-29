package dev.muzu1.possession.form;

import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;

public interface PossessionStatBehavior {
    double movementSpeedBonus(LivingEntity target, PossessionControlState control);

    double attackDamageBonus(LivingEntity target);

    double knockbackResistanceBonus(LivingEntity target);
}
