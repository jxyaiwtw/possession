package dev.muzu1.possession;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.item.PossessionItems;
import dev.muzu1.possession.net.PossessionPackets;
import dev.muzu1.possession.session.PossessionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PossessionMod implements ModInitializer {
    public static final String MOD_ID = "possession";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PossessionConfig.loadOrCreate();
        PossessionItems.register();
        PossessionPackets.registerServerReceivers();

        ServerTickEvents.END_SERVER_TICK.register(server -> PossessionManager.getInstance().tickServer(server));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, amount) ->
                PossessionManager.getInstance().allowDamage(entity, damageSource, amount));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) ->
                PossessionManager.getInstance().onLivingEntityDeath(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) ->
                PossessionManager.getInstance().onEntityUnload(entity));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                PossessionManager.getInstance().onPlayerChangedWorld(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                PossessionItems.grantSoulCharmAfterFirstDeath(newPlayer);
            }
        });

        LOGGER.info("Possession mod initialized");
    }
}
