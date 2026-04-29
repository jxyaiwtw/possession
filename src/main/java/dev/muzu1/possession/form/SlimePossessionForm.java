package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class SlimePossessionForm extends ConfigBackedPossessionForm {
    public static final SlimePossessionForm INSTANCE = new SlimePossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.04D, null, null, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(2.0D, 0.28D);

    private SlimePossessionForm() {
        super("slime");
    }

    @Override
    public void applyExtraMovement(LivingEntity target, PossessionControlState control) {
    }

    @Override
    public void afterPlayerLikeJump(LivingEntity target, PossessionControlState control) {
        PossessionConfig.SlimeBehavior behavior = PossessionConfig.BEHAVIORS.slime;
        Vec3d velocity = target.getVelocity();
        target.setVelocity(velocity.x, Math.max(velocity.y, behavior.minJumpVerticalVelocity), velocity.z);
        target.playSound(SoundEvents.ENTITY_SLIME_JUMP, behavior.jumpSoundVolume, behavior.jumpSoundPitch);
    }

    @Override
    public boolean requiresJumpReleaseBetweenJumps(LivingEntity target) {
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
}
