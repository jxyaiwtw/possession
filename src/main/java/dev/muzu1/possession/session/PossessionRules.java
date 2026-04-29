package dev.muzu1.possession.session;

import dev.muzu1.possession.config.PossessionConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;

public final class PossessionRules {
    public static final float MIN_REMAINING_HEALTH = PossessionConfig.GENERAL.minRemainingHealth;
    public static final double MAX_ATTACH_DISTANCE = PossessionConfig.GENERAL.maxAttachDistance;
    public static final double MAX_ATTACH_DISTANCE_SQUARED = MAX_ATTACH_DISTANCE * MAX_ATTACH_DISTANCE;
    public static final float MONSTER_POSSESSION_HEALTH_RATIO = PossessionConfig.GENERAL.monsterPossessionHealthRatio;
    public static final float TARGET_DEATH_BACKLASH_RATIO = PossessionConfig.GENERAL.targetDeathBacklashRatio;

    private PossessionRules() {
    }

    public static boolean canPossess(LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved() && !(entity instanceof PlayerEntity);
    }

    public static boolean canStartPossessingByHealth(LivingEntity entity) {
        return !(entity instanceof Monster)
                || entity.getHealth() <= entity.getMaxHealth() * MONSTER_POSSESSION_HEALTH_RATIO;
    }

    public static float calculateTargetBoostedMaxHealth(float baseMaxHealth) {
        float bonus = Math.max(PossessionConfig.GENERAL.targetHealthBonusFlat, baseMaxHealth * PossessionConfig.GENERAL.targetHealthBonusRatio);
        return baseMaxHealth + bonus;
    }
}
