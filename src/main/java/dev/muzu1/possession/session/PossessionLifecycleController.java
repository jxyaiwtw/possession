package dev.muzu1.possession.session;

import dev.muzu1.possession.PossessionMod;
import dev.muzu1.possession.control.PossessionControlState;
import dev.muzu1.possession.form.PossessionLifecycleBehavior;
import dev.muzu1.possession.form.PossessionStatBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

final class PossessionLifecycleController {
    private static final int POSSESSION_CHUNK_TICKET_RADIUS = 2;
    private static final UUID PLAYER_MAX_HEALTH_MODIFIER_ID = UUID.fromString("fbf805f4-4f49-45d0-8d87-3524f5b9cd5f");
    private static final UUID TARGET_MAX_HEALTH_MODIFIER_ID = UUID.fromString("4fc6f0cb-5b63-4f68-9d0c-5bc570087c10");
    private static final UUID TARGET_MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("0772b0a9-1e76-4ec7-9f54-ea14f0b7de70");
    private static final UUID TARGET_ATTACK_DAMAGE_MODIFIER_ID = UUID.fromString("1fb5c5a0-8a3e-4d4f-9b7a-3cbe09a3da7f");
    private static final UUID TARGET_KNOCKBACK_RESISTANCE_MODIFIER_ID = UUID.fromString("ed3d22c3-b2f8-48c0-a1a1-1f56dce5aa4d");

    private final float playerBaselineHealth;
    private final float playerBaselineMaxHealth;
    private final boolean playerBaselineInvulnerable;
    private final boolean playerBaselineInvisible;
    private final float targetBaselineHealth;
    private final float targetBaselineMaxHealth;
    private final float targetBoostedMaxHealth;
    private final float playerDeathPenalty;
    private final boolean targetBaselinePersistent;
    private final boolean targetBaselineNoGravity;
    private final boolean targetBaselineNoClip;
    private final PossessionStatBehavior statBehavior;
    private final PossessionLifecycleBehavior lifecycleBehavior;
    private final PossessionTargetController targetController;
    private ServerWorld chunkTicketWorld;
    private ChunkPos chunkTicketPos;
    private double lastTargetMovementSpeedBonus;

    PossessionLifecycleController(float playerBaselineHealth,
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
                                  PossessionStatBehavior statBehavior,
                                  PossessionLifecycleBehavior lifecycleBehavior,
                                  PossessionTargetController targetController) {
        this.playerBaselineHealth = playerBaselineHealth;
        this.playerBaselineMaxHealth = playerBaselineMaxHealth;
        this.playerBaselineInvulnerable = playerBaselineInvulnerable;
        this.playerBaselineInvisible = playerBaselineInvisible;
        this.targetBaselineHealth = targetBaselineHealth;
        this.targetBaselineMaxHealth = targetBaselineMaxHealth;
        this.targetBoostedMaxHealth = targetBoostedMaxHealth;
        this.playerDeathPenalty = playerDeathPenalty;
        this.targetBaselinePersistent = targetBaselinePersistent;
        this.targetBaselineNoGravity = targetBaselineNoGravity;
        this.targetBaselineNoClip = targetBaselineNoClip;
        this.statBehavior = statBehavior;
        this.lifecycleBehavior = lifecycleBehavior;
        this.targetController = targetController;
        this.chunkTicketWorld = null;
        this.chunkTicketPos = null;
        this.lastTargetMovementSpeedBonus = Double.NaN;
    }

    float getTargetBoostedMaxHealth() {
        return targetBoostedMaxHealth;
    }

    void applyPlayerBoost(ServerPlayerEntity player) {
        setMaxHealthModifier(player, PLAYER_MAX_HEALTH_MODIFIER_ID, targetBoostedMaxHealth - playerBaselineMaxHealth);
        player.setHealth(MathHelper.clamp(targetBoostedMaxHealth, PossessionRules.MIN_REMAINING_HEALTH, player.getMaxHealth()));
        player.setInvulnerable(true);
        player.setInvisible(true);
    }

    void onPossessionStarted(ServerPlayerEntity player, LivingEntity target) {
        lifecycleBehavior.onPossessionStarted(player, target);
    }

    void applyTargetBoost(LivingEntity target, PossessionControlState control) {
        setMaxHealthModifier(target, TARGET_MAX_HEALTH_MODIFIER_ID, targetBoostedMaxHealth - targetBaselineMaxHealth);
        target.setHealth(scaleHealth(targetBaselineHealth, targetBaselineMaxHealth, targetBoostedMaxHealth));
        updateTargetMovementSpeedModifier(target, control);
        setConditionalModifier(target, EntityAttributes.GENERIC_ATTACK_DAMAGE, TARGET_ATTACK_DAMAGE_MODIFIER_ID, statBehavior.attackDamageBonus(target));
        setConditionalModifier(target, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, TARGET_KNOCKBACK_RESISTANCE_MODIFIER_ID, statBehavior.knockbackResistanceBonus(target));
    }

    void updateTargetMovementSpeedModifier(LivingEntity target, PossessionControlState control) {
        double amount = statBehavior.movementSpeedBonus(target, control);
        if (Double.compare(amount, lastTargetMovementSpeedBonus) == 0) {
            return;
        }

        setConditionalModifier(target, EntityAttributes.GENERIC_MOVEMENT_SPEED, TARGET_MOVEMENT_SPEED_MODIFIER_ID, amount);
        lastTargetMovementSpeedBonus = amount;
    }

    void restorePlayer(ServerPlayerEntity player, PossessionReason reason) {
        removeModifier(player, EntityAttributes.GENERIC_MAX_HEALTH, PLAYER_MAX_HEALTH_MODIFIER_ID);
        player.setInvulnerable(playerBaselineInvulnerable);
        player.setInvisible(playerBaselineInvisible);
        if (reason == PossessionReason.PLAYER_DEATH) {
            return;
        }

        float restoredHealth = switch (reason) {
            case TARGET_DEATH ->
                    Math.max(PossessionRules.MIN_REMAINING_HEALTH, playerBaselineHealth - playerDeathPenalty);
            case MANUAL, TARGET_UNLOADED, PLAYER_DEATH, PLAYER_UNLOADED, WORLD_CHANGE, CHARM_MISSING, TOO_FAR -> playerBaselineHealth;
        };
        player.setHealth(MathHelper.clamp(restoredHealth, PossessionRules.MIN_REMAINING_HEALTH, player.getMaxHealth()));
    }

    void restoreTarget(LivingEntity target) {
        releaseChunkTicket();
        float boostedHealth = target.getHealth();
        float boostedMaxHealth = target.getMaxHealth();
        removeModifier(target, EntityAttributes.GENERIC_MAX_HEALTH, TARGET_MAX_HEALTH_MODIFIER_ID);
        removeModifier(target, EntityAttributes.GENERIC_MOVEMENT_SPEED, TARGET_MOVEMENT_SPEED_MODIFIER_ID);
        removeModifier(target, EntityAttributes.GENERIC_ATTACK_DAMAGE, TARGET_ATTACK_DAMAGE_MODIFIER_ID);
        removeModifier(target, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, TARGET_KNOCKBACK_RESISTANCE_MODIFIER_ID);
        lastTargetMovementSpeedBonus = Double.NaN;
        targetController.restoreBaselineState(target, targetBaselinePersistent, targetBaselineNoGravity, targetBaselineNoClip);

        lifecycleBehavior.onPossessionEnded(target);

        if (!target.isAlive() || target.isRemoved()) {
            return;
        }

        float restoredHealth = scaleHealth(boostedHealth, boostedMaxHealth, targetBaselineMaxHealth);
        target.setHealth(MathHelper.clamp(restoredHealth, 1.0F, target.getMaxHealth()));
    }

    void updateChunkTicket(LivingEntity target) {
        if (!(target.getWorld() instanceof ServerWorld serverWorld)) {
            releaseChunkTicket();
            return;
        }

        ChunkPos currentChunkPos = new ChunkPos(target.getBlockPos());
        if (serverWorld == chunkTicketWorld && currentChunkPos.equals(chunkTicketPos)) {
            return;
        }

        releaseChunkTicket();
        serverWorld.getChunkManager().addTicket(ChunkTicketType.UNKNOWN, currentChunkPos, POSSESSION_CHUNK_TICKET_RADIUS, currentChunkPos);
        chunkTicketWorld = serverWorld;
        chunkTicketPos = currentChunkPos;
    }

    void releaseChunkTicket() {
        if (chunkTicketWorld != null && chunkTicketPos != null) {
            chunkTicketWorld.getChunkManager().removeTicket(ChunkTicketType.UNKNOWN, chunkTicketPos, POSSESSION_CHUNK_TICKET_RADIUS, chunkTicketPos);
        }
        chunkTicketWorld = null;
        chunkTicketPos = null;
    }

    private static float scaleHealth(float currentHealth, float currentMaxHealth, float newMaxHealth) {
        if (currentMaxHealth <= 0.0F) {
            return MathHelper.clamp(currentHealth, 0.1F, newMaxHealth);
        }
        float healthRatio = MathHelper.clamp(currentHealth / currentMaxHealth, 0.0F, 1.0F);
        return MathHelper.clamp(healthRatio * newMaxHealth, 0.1F, newMaxHealth);
    }

    private static void setMaxHealthModifier(LivingEntity entity, UUID modifierId, double amount) {
        setConditionalModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, modifierId, amount);
    }

    private static void setConditionalModifier(LivingEntity entity, EntityAttribute attribute, UUID modifierId, double amount) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(modifierId);
        if (amount != 0.0D) {
            instance.addTemporaryModifier(new EntityAttributeModifier(modifierId, PossessionMod.MOD_ID + ":" + attribute.getTranslationKey(), amount, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeModifier(LivingEntity entity, EntityAttribute attribute, UUID modifierId) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.removeModifier(modifierId);
        }
    }
}
