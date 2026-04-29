package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;

public final class SquidPossessionForm extends ConfigBackedPossessionForm {
    public static final SquidPossessionForm INSTANCE = new SquidPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.0D, null, null, null, null, 0.12D, -0.02D, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(0.4D, 0.02D);

    private SquidPossessionForm() {
        super("squid");
    }

    @Override
    public void applyExtraMovement(LivingEntity target, PossessionControlState control) {
        if (target.isTouchingWater() && control.forward() != 0.0F) {
            PossessionConfig.SquidBehavior behavior = PossessionConfig.BEHAVIORS.squid;
            double radians = Math.toRadians(control.yaw());
            double pushX = -Math.sin(radians) * behavior.forwardWaterPush;
            double pushZ = Math.cos(radians) * behavior.forwardWaterPush;
            double pushY = control.sneak() ? behavior.downwardPush : behavior.upwardPush;
            target.addVelocity(pushX, pushY, pushZ);
        }
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
