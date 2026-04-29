package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public final class SpiderPossessionForm extends ConfigBackedPossessionForm {
    public static final SpiderPossessionForm INSTANCE = new SpiderPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.09D, null, null, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(1.0D, 0.10D);

    private SpiderPossessionForm() {
        super("spider");
    }

    @Override
    public void applyExtraMovement(LivingEntity target, PossessionControlState control) {
        if (control.jump() && target.horizontalCollision) {
            PossessionConfig.SpiderBehavior behavior = PossessionConfig.BEHAVIORS.spider;
            Vec3d velocity = target.getVelocity();
            target.setVelocity(
                    velocity.x * behavior.wallClimbHorizontalDamping,
                    Math.max(velocity.y, behavior.wallClimbVerticalVelocity),
                    velocity.z * behavior.wallClimbHorizontalDamping
            );
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
