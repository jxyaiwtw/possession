package dev.muzu1.possession.session;

import dev.muzu1.possession.PossessionMod;
import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.item.PossessionItems;
import dev.muzu1.possession.net.PossessionPackets;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PossessionManager {
    private static final PossessionManager INSTANCE = new PossessionManager();

    private final Map<UUID, PossessionSession> sessionsByPlayer = new HashMap<>();
    private final Map<UUID, UUID> playerByTarget = new HashMap<>();
    private final Map<UUID, DamageGraceWindow> damageGraceByTarget = new HashMap<>();

    private PossessionManager() {
    }

    public static PossessionManager getInstance() {
        return INSTANCE;
    }

    public void handleAttachRequest(ServerPlayerEntity player, int targetEntityId) {
        PossessionSession existingSession = sessionsByPlayer.get(player.getUuid());
        if (existingSession != null) {
            PossessionMod.LOGGER.warn("Recovered stale possession session before reattach: player={}", player.getUuid());
            recoverSessionForReattach(player.getServer(), existingSession);
        }

        if (!PossessionItems.hasSoulCharm(player)) {
            PossessionPackets.sendReject(player, "message.possession.reject.no_charm");
            return;
        }

        Entity entity = player.getWorld().getEntityById(targetEntityId);
        if (!(entity instanceof LivingEntity target)) {
            PossessionPackets.sendReject(player, "message.possession.reject.no_target");
            return;
        }

        if (target == player) {
            PossessionPackets.sendReject(player, "message.possession.reject.same_target");
            return;
        }

        if (!target.isAlive() || target.isRemoved()) {
            PossessionPackets.sendReject(player, "message.possession.reject.dead_or_removed");
            return;
        }

        if (targetByOtherPlayer(target)) {
            PossessionPackets.sendReject(player, "message.possession.reject.target_busy");
            return;
        }

        if (player.squaredDistanceTo(target) > PossessionRules.MAX_ATTACH_DISTANCE_SQUARED) {
            PossessionPackets.sendReject(player, "message.possession.reject.too_far");
            return;
        }

        if (!PossessionRules.canPossess(target)) {
            PossessionPackets.sendReject(player, "message.possession.reject.invalid_target");
            return;
        }

        if (!PossessionRules.canStartPossessingByHealth(target)) {
            PossessionPackets.sendReject(player, "message.possession.reject.monster_too_healthy");
            return;
        }

        PossessionSession session = PossessionSession.begin(player, target);
        reserveSession(session, player.getServer());

        try {
            activateReservedSession(session, player, target);
        } catch (RuntimeException exception) {
            rollbackReservedSession(session, player, target);
            PossessionMod.LOGGER.error("Failed to start possession: player={} target={} type={}", player.getUuid(), target.getUuid(), target.getType(), exception);
            PossessionPackets.sendReject(player, "message.possession.reject.invalid_target");
            return;
        }

        commitReservedSession(session, player, target);
    }

    public void handleDetachRequest(ServerPlayerEntity player, PossessionReason reason) {
        PossessionSession session = sessionsByPlayer.get(player.getUuid());
        if (session == null) {
            return;
        }
        closeSession(player.getServer(), session, reason, true);
    }

    public void handleControlUpdate(ServerPlayerEntity player,
                                    float forward,
                                    float sideways,
                                    boolean jump,
                                    boolean sneak,
                                    boolean sprint,
                                    float yaw,
                                    float pitch,
                                    boolean ability,
                                    boolean attack,
                                    boolean use,
                                    int focusTargetEntityId,
                                    int attackTargetEntityId,
                                    boolean hasBlockTarget,
                                    BlockPos blockTargetPos,
                                    Direction blockTargetSide) {
        PossessionSession session = sessionsByPlayer.get(player.getUuid());
        if (session == null) {
            return;
        }
        session.updateControl(forward, sideways, jump, sneak, sprint, yaw, pitch, ability, attack, use, focusTargetEntityId, attackTargetEntityId, hasBlockTarget, blockTargetPos, blockTargetSide);
    }

    public void onLivingEntityDeath(LivingEntity entity) {
        damageGraceByTarget.remove(entity.getUuid());
        if (entity instanceof ServerPlayerEntity player) {
            PossessionSession session = sessionsByPlayer.get(player.getUuid());
            if (session != null) {
                closeSession(player.getServer(), session, PossessionReason.PLAYER_DEATH, true);
            }
            return;
        }

        UUID playerUuid = playerByTarget.get(entity.getUuid());
        if (playerUuid != null) {
            PossessionSession session = sessionsByPlayer.get(playerUuid);
            if (session != null) {
                closeSession(entity.getWorld().getServer(), session, PossessionReason.TARGET_DEATH, true);
            }
        }
    }

    public void onEntityUnload(Entity entity) {
        if (entity instanceof ServerPlayerEntity player) {
            PossessionSession session = sessionsByPlayer.get(player.getUuid());
            if (session != null) {
                PossessionMod.LOGGER.info("Possessed player entity unloaded: player={}", player.getUuid());
            }
        }
    }

    public void onPlayerChangedWorld(ServerPlayerEntity player) {
        PossessionSession session = sessionsByPlayer.get(player.getUuid());
        if (session != null) {
            closeSession(player.getServer(), session, PossessionReason.WORLD_CHANGE, true);
        }
    }

    public void tickServer(MinecraftServer server) {
        damageGraceByTarget.entrySet().removeIf(entry -> entry.getValue().expiresAtServerTick() <= server.getTicks());
        for (PossessionSession session : new ArrayList<>(sessionsByPlayer.values())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(session.getPlayerUuid());
            if (player == null) {
                closeSession(server, session, PossessionReason.PLAYER_UNLOADED, true);
                continue;
            }

            if (session.isAttaching()) {
                continue;
            }

            if (!session.isActive()) {
                continue;
            }

            LivingEntity target = session.resolveTarget(player.getServer());
            if (target == null) {
                if (session.markTargetMissing()) {
                    closeSession(server, session, PossessionReason.TARGET_UNLOADED, true);
                }
                continue;
            }
            session.markTargetResolved();
            if (!target.isAlive() || target.isRemoved()) {
                closeSession(server, session, PossessionReason.TARGET_DEATH, true);
                continue;
            }

            if (player.getWorld() != target.getWorld()) {
                closeSession(server, session, PossessionReason.WORLD_CHANGE, true);
                continue;
            }

            if (!PossessionItems.hasSoulCharm(player)) {
                closeSession(server, session, PossessionReason.CHARM_MISSING, true);
                continue;
            }

            if (player.isRemoved() || !player.isAlive()) {
                closeSession(server, session, PossessionReason.PLAYER_DEATH, true);
                continue;
            }

            session.updateChunkTicket(target);
            session.maintainTargetTakeover(player, target);
            if (!session.wasAppliedThisTick(server.getTicks())) {
                session.applyControl(target, server.getTicks());
            }
        }
    }

    private boolean targetByOtherPlayer(LivingEntity target) {
        UUID playerUuid = playerByTarget.get(target.getUuid());
        return playerUuid != null;
    }

    private void reserveSession(PossessionSession session, MinecraftServer server) {
        sessionsByPlayer.put(session.getPlayerUuid(), session);
        playerByTarget.put(session.getTargetUuid(), session.getPlayerUuid());
        if (server != null) {
            damageGraceByTarget.put(session.getTargetUuid(), new DamageGraceWindow(session.getPlayerUuid(), server.getTicks() + PossessionConfig.GENERAL.possessionDamageGraceTicks));
        }
    }

    private void activateReservedSession(PossessionSession session, ServerPlayerEntity player, LivingEntity target) {
        session.applyPlayerBoost(player);
        session.onPossessionStarted(player, target);
        session.applyTargetBoost(target);
        session.applyTargetTakeover(target);
        session.markAttached();
    }

    private void commitReservedSession(PossessionSession session, ServerPlayerEntity player, LivingEntity target) {
        if (!session.isActive()) {
            throw new IllegalStateException("Cannot commit possession session in state " + session.getState());
        }
        PossessionPackets.sendAttachAccepted(player, target.getId(), session.getTargetBoostedMaxHealth(), target.getDisplayName().getString());
        player.sendMessage(Text.translatable("message.possession.attach_success"), true);
    }

    private void rollbackReservedSession(PossessionSession session, ServerPlayerEntity player, LivingEntity target) {
        session.markAttachFailed();
        clearSessionTracking(session);
        safelyRollbackFailedAttach(player, target, session);
    }

    private void closeSession(MinecraftServer server, PossessionSession session, PossessionReason reason, boolean notifyPlayer) {
        if (!session.beginDetaching()) {
            if (session.getState() == PossessionSessionState.FAILED) {
                clearSessionTracking(session);
            }
            return;
        }

        if (server == null) {
            session.releaseChunkTicket();
            clearSessionTracking(session);
            return;
        }

        ServerPlayerEntity player = session.resolvePlayer(server);
        LivingEntity target = session.resolveTarget(server);
        clearSessionTracking(session);

        if (target != null) {
            session.restoreTarget(target);
        } else {
            session.releaseChunkTicket();
        }

        if (player != null) {
            if (reason != PossessionReason.PLAYER_DEATH && target != null && target.getWorld() instanceof ServerWorld serverWorld) {
                player.teleport(serverWorld, target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
            }
            session.restorePlayer(player, reason);
            if (notifyPlayer && reason != PossessionReason.PLAYER_DEATH) {
                PossessionPackets.sendForceExit(player, reason.getTranslationKey());
                player.sendMessage(Text.translatable(reason.getTranslationKey()), true);
            }
        }

        PossessionMod.LOGGER.info("Forced possession exit: player={} reason={} targetPresent={}", session.getPlayerUuid(), reason, target != null);
    }

    private void recoverSessionForReattach(MinecraftServer server, PossessionSession session) {
        closeSession(server, session, PossessionReason.MANUAL, false);
    }

    private void safelyRollbackFailedAttach(ServerPlayerEntity player, LivingEntity target, PossessionSession session) {
        try {
            session.restoreTarget(target);
        } catch (RuntimeException rollbackException) {
            PossessionMod.LOGGER.error("Failed to rollback target after attach error: player={} target={}", player.getUuid(), target.getUuid(), rollbackException);
            session.releaseChunkTicket();
        }

        try {
            session.restorePlayer(player, PossessionReason.MANUAL);
        } catch (RuntimeException rollbackException) {
            PossessionMod.LOGGER.error("Failed to rollback player after attach error: player={} target={}", player.getUuid(), target.getUuid(), rollbackException);
        }
    }

    public boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        DamageGraceWindow graceWindow = damageGraceByTarget.get(entity.getUuid());
        if (graceWindow == null) {
            return true;
        }

        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity player && player.getUuid().equals(graceWindow.playerUuid())) {
            return false;
        }

        return true;
    }

    private void clearSessionTracking(PossessionSession session) {
        sessionsByPlayer.remove(session.getPlayerUuid());
        playerByTarget.remove(session.getTargetUuid());
        damageGraceByTarget.remove(session.getTargetUuid());
    }

    private record DamageGraceWindow(UUID playerUuid, int expiresAtServerTick) {
    }
}
