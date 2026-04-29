package dev.muzu1.possession.form;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public final class PossessionFormRegistry {
    private static final PossessionFormDefinition DEFAULT = new PossessionFormDefinition("default", DefaultPossessionForm.INSTANCE);
    private static final Map<EntityType<?>, PossessionFormDefinition> DEFINITIONS = new HashMap<>();

    static {
        register(new PossessionFormDefinition("creeper", CreeperPossessionForm.INSTANCE), EntityType.CREEPER);
        register(new PossessionFormDefinition("skeleton", SkeletonPossessionForm.INSTANCE), EntityType.SKELETON);
        register(new PossessionFormDefinition("zombie", ZombiePossessionForm.INSTANCE), EntityType.ZOMBIE);
        register(new PossessionFormDefinition("frog", FrogPossessionForm.INSTANCE), EntityType.FROG);
        register(new PossessionFormDefinition("dolphin", DolphinPossessionForm.INSTANCE), EntityType.DOLPHIN);
        register(new PossessionFormDefinition("squid", SquidPossessionForm.INSTANCE), EntityType.SQUID, EntityType.GLOW_SQUID);
        register(new PossessionFormDefinition("slime", SlimePossessionForm.INSTANCE), EntityType.SLIME, EntityType.MAGMA_CUBE);
        register(new PossessionFormDefinition("spider", SpiderPossessionForm.INSTANCE), EntityType.SPIDER, EntityType.CAVE_SPIDER);
        register(new PossessionFormDefinition("camel", CamelPossessionForm.INSTANCE), EntityType.CAMEL);
        register(new PossessionFormDefinition("goat", GoatPossessionForm.INSTANCE), EntityType.GOAT);
        register(new PossessionFormDefinition("enderman", EndermanPossessionForm.INSTANCE), EntityType.ENDERMAN);
        register(new PossessionFormDefinition("wolf", WolfPossessionForm.INSTANCE), EntityType.WOLF);
    }

    private PossessionFormRegistry() {
    }

    public static PossessionForm resolve(LivingEntity entity) {
        return resolveDefinition(entity).form();
    }

    public static String resolveKey(LivingEntity entity) {
        return resolveDefinition(entity).key();
    }

    private static PossessionFormDefinition resolveDefinition(LivingEntity entity) {
        return DEFINITIONS.getOrDefault(entity.getType(), DEFAULT);
    }

    private static void register(PossessionFormDefinition definition, EntityType<?>... entityTypes) {
        for (EntityType<?> entityType : entityTypes) {
            DEFINITIONS.put(entityType, definition);
        }
    }
}
