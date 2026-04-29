package dev.muzu1.possession.form;

import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;

public final class DefaultPossessionForm extends ConfigBackedPossessionForm {
    public static final DefaultPossessionForm INSTANCE = new DefaultPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.05D, null, null, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(2.0D, 0.2D);

    private DefaultPossessionForm() {
        super("default");
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
