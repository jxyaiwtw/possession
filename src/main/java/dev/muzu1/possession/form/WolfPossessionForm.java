package dev.muzu1.possession.form;

import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;

public final class WolfPossessionForm extends ConfigBackedPossessionForm {
    public static final WolfPossessionForm INSTANCE = new WolfPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.10D, null, null, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(2.5D, 0.12D);

    private WolfPossessionForm() {
        super("wolf");
    }

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback movementFallback() {
        return MOVEMENT;
    }

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback statFallback() {
        return STATS;
    }
}
