package dev.muzu1.possession.session;

import dev.muzu1.possession.PossessionMod;
import dev.muzu1.possession.control.PossessionControlState;
import dev.muzu1.possession.control.PossessionControlTracker;
import dev.muzu1.possession.form.PossessionForm;
import dev.muzu1.possession.form.PossessionFormRegistry;
import dev.muzu1.possession.mixin.MobEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.UUID;

public final class PossessionSession {
    private static final int TARGET_MISSING_GRACE_TICKS = 30;
    private static final int JUMP_BUFFER_TICKS = 4;

    private final UUID playerUuid;
    private final UUID targetUuid;
    private final PossessionForm form;
    private final PossessionControlTracker controlTracker;
    private final PossessionMovementController movementController;
    private final PossessionActionController actionController;
    private final PossessionTargetController targetController;
    private final PossessionLifecycleController lifecycleController;
    private PossessionSessionState state;
    private int jumpBufferTicksRemaining;
    private int lastAppliedServerTick;
    private int missingTargetTicks;

    private PossessionSession(UUID playerUuid,
                             UUID targetUuid,
                             float playerBaselineHealth,
                             float playerBaselineMaxHealth,
                             boolean playerBaselineInvulnerable,
                             boolean playerBaselineInvisible,
                             float targetBaselineHealth,
                             float targetBaselineMaxHealth,
                             float targetBoostedMaxHealth,
                             float playerDeathPenalty,
                             boolean targetBaselinePersistent,
                             boolean targetBaselineNoGravity,
                             boolean targetBaselineNoClip,
                             PossessionForm form) {
        this.playerUuid = playerUuid;
        this.targetUuid = targetUuid;
        this.form = form;
        this.controlTracker = new PossessionControlTracker();
        this.movementController = new PossessionMovementController();
        this.actionController = new PossessionActionController();
        this.targetController = new PossessionTargetController();
        this.lifecycleController = new PossessionLifecycleController(
                playerBaselineHealth,
                playerBaselineMaxHealth,
                playerBaselineInvulnerable,
                playerBaselineInvisible,
                targetBaselineHealth,
                targetBaselineMaxHealth,
                targetBoostedMaxHealth,
                playerDeathPenalty,
                targetBaselinePersistent,
                targetBaselineNoGravity,
                targetBaselineNoClip,
                form,
                form,
                targetController
        );
        this.state = PossessionSessionState.ATTACHING;
        this.jumpBufferTicksRemaining = 0;
        this.lastAppliedServerTick = Integer.MIN_VALUE;
        this.missingTargetTicks = 0;
    }

    public boolean markTargetResolved() {
        this.missingTargetTicks = 0;
        return true;
    }

    public boolean markTargetMissing() {
        this.missingTargetTicks++;
        return this.missingTargetTicks > TARGET_MISSING_GRACE_TICKS;
    }

    public static PossessionSession begin(ServerPlayerEntity player, LivingEntity target) {
        float playerBaselineHealth = player.getHealth();
        float playerBaselineMaxHealth = player.getMaxHealth();
        boolean playerBaselineInvulnerable = player.isInvulnerable();
        boolean playerBaselineInvisible = player.isInvisible();
        float targetBaselineHealth = target.getHealth();
        float targetBaselineMaxHealth = target.getMaxHealth();
        float targetBoostedMaxHealth = PossessionRules.calculateTargetBoostedMaxHealth(targetBaselineMaxHealth);
        float playerDeathPenalty = targetBoostedMaxHealth * PossessionRules.TARGET_DEATH_BACKLASH_RATIO;
        playerDeathPenalty = Math.min(playerDeathPenalty, Math.max(0.0F, playerBaselineHealth - PossessionRules.MIN_REMAINING_HEALTH));
        boolean targetBaselinePersistent = target instanceof MobEntityAccessor accessor
                && accessor.possession$isPersistent();
        boolean targetBaselineNoGravity = target.hasNoGravity();
        boolean targetBaselineNoClip = target.noClip;
        PossessionForm form = PossessionFormRegistry.resolve(target);

        return new PossessionSession(
                player.getUuid(),
                target.getUuid(),
                playerBaselineHealth,
                playerBaselineMaxHealth,
                playerBaselineInvulnerable,
                playerBaselineInvisible,
                targetBaselineHealth,
                targetBaselineMaxHealth,
                targetBoostedMaxHealth,
                playerDeathPenalty,
                targetBaselinePersistent,
                targetBaselineNoGravity,
                targetBaselineNoClip,
                form
        );
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public PossessionSessionState getState() {
        return state;
    }

    public boolean isActive() {
        return state == PossessionSessionState.ACTIVE;
    }

    public boolean isAttaching() {
        return state == PossessionSessionState.ATTACHING;
    }

    public boolean canAcceptControl() {
        return state == PossessionSessionState.ACTIVE;
    }

    public void markAttached() {
        if (state != PossessionSessionState.ATTACHING) {
            throw new IllegalStateException("Cannot activate possession session from state " + state);
        }
        state = PossessionSessionState.ACTIVE;
    }

    public void markAttachFailed() {
        state = PossessionSessionState.FAILED;
    }

    public boolean beginDetaching() {
        if (state == PossessionSessionState.DETACHING || state == PossessionSessionState.FAILED) {
            return false;
        }
        state = PossessionSessionState.DETACHING;
        return true;
    }

    public float getTargetBoostedMaxHealth() {
        return lifecycleController.getTargetBoostedMaxHealth();
    }

    public float getPlayerPossessionHealth() {
        return lifecycleController.getTargetBoostedMaxHealth();
    }

    public void updateControl(float forward,
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
        if (!canAcceptControl()) {
            return;
        }
        controlTracker.update(
                forward,
                sideways,
                jump,
                sneak,
                sprint,
                yaw,
                pitch,
                ability,
                attack,
                use,
                focusTargetEntityId,
                attackTargetEntityId,
                hasBlockTarget,
                blockTargetPos,
                blockTargetSide
        );
        if (controlTracker.jumpPressedThisTick()) {
            this.jumpBufferTicksRemaining = JUMP_BUFFER_TICKS;
        }
    }

    public boolean wasAppliedThisTick(int serverTick) {
        return lastAppliedServerTick == serverTick;
    }

    public ServerPlayerEntity resolvePlayer(MinecraftServer server) {
        return server.getPlayerManager().getPlayer(playerUuid);
    }

    public LivingEntity resolveTarget(MinecraftServer server) {
        for (var world : server.getWorlds()) {
            Entity entity = world.getEntity(targetUuid);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    public void applyPlayerBoost(ServerPlayerEntity player) {
        lifecycleController.applyPlayerBoost(player);
    }

    public void onPossessionStarted(ServerPlayerEntity player, LivingEntity target) {
        lifecycleController.onPossessionStarted(player, target);
    }

    public void applyTargetBoost(LivingEntity target) {
        lifecycleController.applyTargetBoost(target, currentControlState());
    }

    public void restorePlayer(ServerPlayerEntity player, PossessionReason reason) {
        lifecycleController.restorePlayer(player, reason);
    }

    public void restoreTarget(LivingEntity target) {
        lifecycleController.restoreTarget(target);
    }

    public void applyTargetTakeover(LivingEntity target) {
        targetController.applyPossessionState(target);
    }

    public void updateChunkTicket(LivingEntity target) {
        lifecycleController.updateChunkTicket(target);
    }

    public void releaseChunkTicket() {
        lifecycleController.releaseChunkTicket();
    }

    public void maintainTargetTakeover(ServerPlayerEntity player, LivingEntity target) {
        targetController.maintainPossessionState(player, target);
    }

    public void applyControl(LivingEntity target, int serverTick) {
        if (!canAcceptControl()) {
            return;
        }
        lastAppliedServerTick = serverTick;
        PossessionControlState control = currentControlState();
        if (jumpBufferTicksRemaining > 0) {
            jumpBufferTicksRemaining--;
        }
        lifecycleController.updateTargetMovementSpeedModifier(target, control);
        movementController.syncFacing(target, control);
        boolean jumpConsumed = movementController.applyMovement(target, control, form, jumpBufferTicksRemaining > 0);
        if (jumpConsumed) {
            jumpBufferTicksRemaining = 0;
        }
        actionController.tick(target, control, form);
        controlTracker.finishTick();
    }

    private PossessionControlState currentControlState() {
        return controlTracker.snapshot();
    }
}
