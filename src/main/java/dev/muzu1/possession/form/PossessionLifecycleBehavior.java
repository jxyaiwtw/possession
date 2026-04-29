package dev.muzu1.possession.form;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public interface PossessionLifecycleBehavior {
    default void onPossessionEnded(LivingEntity target) {
    }

    default void onPossessionStarted(ServerPlayerEntity player, LivingEntity target) {
    }
}
