package dev.muzu1.possession.session;

import dev.muzu1.possession.mixin.MobEntityAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

final class PossessionTargetController {
    void applyPossessionState(LivingEntity target) {
        if (target instanceof MobEntity mob) {
            mob.setPersistent();
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setVelocity(0.0D, 0.0D, 0.0D);
            mob.setDespawnCounter(0);
            if (mob instanceof CreeperEntity creeper) {
                creeper.setFuseSpeed(-1);
            }
        }
    }

    void maintainPossessionState(ServerPlayerEntity player, LivingEntity target) {
        player.setAir(player.getMaxAir());
        if (isAquaticPossessionTarget(target) && (target.isTouchingWater() || target.isSubmergedInWater())) {
            target.setAir(target.getMaxAir());
        }

        if (target instanceof MobEntity mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setDespawnCounter(0);
            if (mob instanceof CreeperEntity creeper) {
                creeper.setFuseSpeed(-1);
            }
        }
    }

    void restoreBaselineState(LivingEntity target,
                              boolean baselinePersistent,
                              boolean baselineNoGravity,
                              boolean baselineNoClip) {
        target.setNoGravity(baselineNoGravity);
        target.noClip = baselineNoClip;
        if (target instanceof MobEntityAccessor accessor) {
            accessor.possession$setPersistent(baselinePersistent);
        }
    }

    static boolean isAquaticPossessionTarget(LivingEntity target) {
        EntityType<?> type = target.getType();
        return type == EntityType.COD
                || type == EntityType.SALMON
                || type == EntityType.TROPICAL_FISH
                || type == EntityType.PUFFERFISH
                || type == EntityType.SQUID
                || type == EntityType.GLOW_SQUID
                || type == EntityType.DOLPHIN
                || type == EntityType.GUARDIAN
                || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.AXOLOTL;
    }
}
