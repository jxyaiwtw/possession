package dev.muzu1.possession.session;

import dev.muzu1.possession.control.PossessionControlState;
import dev.muzu1.possession.form.PossessionActionBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Hand;

final class PossessionActionController {
    private PossessionControlState pendingAttackControl;
    private int pendingAttackTargetEntityId;
    private int pendingAttackWindupTicksRemaining;
    private int attackCooldownTicksRemaining;
    private int useCooldownTicksRemaining;
    private int abilityCooldownTicksRemaining;

    PossessionActionController() {
        this.pendingAttackControl = null;
        this.pendingAttackTargetEntityId = -1;
        this.pendingAttackWindupTicksRemaining = 0;
        this.attackCooldownTicksRemaining = 0;
        this.useCooldownTicksRemaining = 0;
        this.abilityCooldownTicksRemaining = 0;
    }

    void tick(LivingEntity target, PossessionControlState control, PossessionActionBehavior form) {
        if (attackCooldownTicksRemaining > 0) {
            attackCooldownTicksRemaining--;
        }
        if (useCooldownTicksRemaining > 0) {
            useCooldownTicksRemaining--;
        }
        if (abilityCooldownTicksRemaining > 0) {
            abilityCooldownTicksRemaining--;
        }
        if (pendingAttackWindupTicksRemaining > 0) {
            pendingAttackWindupTicksRemaining--;
            if (pendingAttackWindupTicksRemaining <= 0) {
                finishPendingAttack(target, form);
            }
        }

        if (form.supportsAbility() && abilityCooldownTicksRemaining <= 0) {
            boolean abilityTriggered;
            if (form.overridesAbilityHandling()) {
                abilityTriggered = form.handleAbilityInput(target, control);
            } else {
                abilityTriggered = control.abilityPressed() && form.handleAbility(target, control);
            }

            if (abilityTriggered) {
                abilityCooldownTicksRemaining = Math.max(0, form.abilityCooldownTicks(target, control));
            }
        }

        if (control.use() && useCooldownTicksRemaining <= 0 && form.handleUse(target, control)) {
            useCooldownTicksRemaining = Math.max(0, form.useCooldownTicks(target, control));
        }

        if (control.attack() && attackCooldownTicksRemaining <= 0 && pendingAttackWindupTicksRemaining <= 0) {
            boolean handled = beginAttack(target, control, form);
            if (handled && pendingAttackWindupTicksRemaining <= 0) {
                attackCooldownTicksRemaining = Math.max(0, form.attackCooldownTicks(target, control));
            }
        }
    }

    private boolean beginAttack(LivingEntity attacker, PossessionControlState control, PossessionActionBehavior form) {
        int windupTicks = Math.max(0, form.attackWindupTicks(attacker, control));
        if (windupTicks > 0) {
            pendingAttackControl = control;
            pendingAttackTargetEntityId = control.attackTargetEntityId();
            pendingAttackWindupTicksRemaining = windupTicks;
            form.onAttackQueued(attacker, control.attackTargetEntityId(), control);
            return true;
        }

        return executeAttack(attacker, control.attackTargetEntityId(), control, form);
    }

    private boolean finishPendingAttack(LivingEntity attacker, PossessionActionBehavior form) {
        if (pendingAttackControl == null || pendingAttackTargetEntityId < 0) {
            clearPendingAttack();
            return false;
        }

        PossessionControlState control = pendingAttackControl;
        int targetEntityId = pendingAttackTargetEntityId;
        clearPendingAttack();

        boolean handled = executeAttack(attacker, targetEntityId, control, form);
        if (handled) {
            attackCooldownTicksRemaining = Math.max(0, form.attackCooldownTicks(attacker, control));
        }
        return handled;
    }

    private void clearPendingAttack() {
        pendingAttackControl = null;
        pendingAttackTargetEntityId = -1;
        pendingAttackWindupTicksRemaining = 0;
    }

    private boolean executeAttack(LivingEntity attacker, int targetEntityId, PossessionControlState control, PossessionActionBehavior form) {
        if (!form.handleAttack(attacker, targetEntityId, control)) {
            return performAttack(attacker, targetEntityId);
        }
        return true;
    }

    private boolean performAttack(LivingEntity attacker, int targetEntityId) {
        if (targetEntityId < 0) {
            return false;
        }

        Entity targetEntity = attacker.getWorld().getEntityById(targetEntityId);
        if (!(targetEntity instanceof LivingEntity livingTarget)) {
            return false;
        }

        if (livingTarget == attacker || !livingTarget.isAlive() || livingTarget.isRemoved()) {
            return false;
        }

        if (attacker.squaredDistanceTo(livingTarget) > 36.0D) {
            return false;
        }

        EntityAttributeInstance attackDamage = attacker.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackDamage == null || attackDamage.getValue() <= 0.0D) {
            return false;
        }

        if (attacker instanceof MobEntity mob) {
            return mob.tryAttack(livingTarget);
        }

        float damage = (float) Math.max(1.0D, attackDamage.getValue());
        livingTarget.damage(attacker.getWorld().getDamageSources().mobAttack(attacker), damage);
        attacker.swingHand(Hand.MAIN_HAND);
        return true;
    }
}
