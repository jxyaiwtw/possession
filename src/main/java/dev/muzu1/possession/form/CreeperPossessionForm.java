package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;

public final class CreeperPossessionForm extends ConfigBackedPossessionForm {
    public static final CreeperPossessionForm INSTANCE = new CreeperPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.07D, null, 0.02D, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(0.0D, 0.05D);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback ACTIONS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback(0, 0, null, 80, 8);

    private CreeperPossessionForm() {
        super("creeper");
    }

    @Override
    public boolean handleAttack(LivingEntity attacker, int attackTargetEntityId, PossessionControlState control) {
        return true;
    }

    @Override
    public boolean supportsAbility() {
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

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback actionFallback() {
        return ACTIONS;
    }

    @Override
    public boolean handleAbility(LivingEntity target, PossessionControlState control) {
        PossessionConfig.CreeperBehavior behavior = PossessionConfig.BEHAVIORS.creeper;
        float power = control.sneak() ? behavior.sneakExplosionPower : behavior.explosionPower;
        PossessionEffects.around(target, ParticleTypes.LARGE_SMOKE, behavior.smokeParticleCount, behavior.particleSpread, behavior.particleVelocity);
        target.getWorld().createExplosion(target, target.getX(), target.getY(), target.getZ(), power, World.ExplosionSourceType.MOB);
        target.damage(target.getWorld().getDamageSources().explosion(target, target), target.getMaxHealth() * behavior.selfDamageMultiplier);
        return true;
    }
}
