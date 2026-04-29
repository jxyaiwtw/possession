package dev.muzu1.possession.form;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;

final class PossessionEffects {
    private PossessionEffects() {
    }

    static void around(LivingEntity entity, ParticleEffect particle, int count, double spread, double speed) {
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    particle,
                    entity.getX(),
                    entity.getBodyY(0.55D),
                    entity.getZ(),
                    count,
                    spread,
                    spread * 0.7D,
                    spread,
                    speed
            );
        }
    }
}
