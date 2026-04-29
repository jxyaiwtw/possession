package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public final class SkeletonPossessionForm extends ConfigBackedPossessionForm {
    public static final SkeletonPossessionForm INSTANCE = new SkeletonPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.06D, null, null, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(0.0D, 0.04D);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback ACTIONS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback(0, 0, null, 24, 8);

    private SkeletonPossessionForm() {
        super("skeleton");
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
    public boolean handleAbility(LivingEntity attacker, PossessionControlState control) {
        PossessionConfig.SkeletonBehavior behavior = PossessionConfig.BEHAVIORS.skeleton;
        ArrowEntity arrow = new ArrowEntity(attacker.getWorld(), attacker);
        arrow.setPos(attacker.getX(), attacker.getEyeY() - 0.1D, attacker.getZ());
        arrow.setVelocity(attacker, control.pitch(), control.yaw(), 0.0F, behavior.arrowSpeed, behavior.arrowDivergence);
        arrow.setDamage((float) Math.max(behavior.minArrowDamage, attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)));

        attacker.getWorld().spawnEntity(arrow);
        PossessionEffects.around(attacker, ParticleTypes.CRIT, behavior.critParticleCount, behavior.particleSpread, behavior.particleVelocity);
        attacker.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, behavior.soundVolume, behavior.soundPitch);
        attacker.swingHand(Hand.MAIN_HAND);
        return true;
    }
}
