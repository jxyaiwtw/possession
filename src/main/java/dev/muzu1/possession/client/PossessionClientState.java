package dev.muzu1.possession.client;

import dev.muzu1.possession.config.PossessionConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PossessionClientState {
    private boolean active;
    private int targetEntityId = -1;
    private float boostedMaxHealth;
    private String targetDisplayName = "";
    private String lastMessage = "";
    private long lastMessageAt = 0L;
    private float controlYaw;
    private float controlPitch;
    private int actionSuppressionTicks;

    public boolean isActive() {
        return active;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    public float getBoostedMaxHealth() {
        return boostedMaxHealth;
    }

    public String getTargetDisplayName() {
        return targetDisplayName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public float getControlYaw() {
        return controlYaw;
    }

    public float getControlPitch() {
        return controlPitch;
    }

    public boolean shouldSuppressActions() {
        return actionSuppressionTicks > 0;
    }

    public boolean canRequestAttach() {
        return !active;
    }

    public boolean shouldShowMessage() {
        return !lastMessage.isEmpty() && System.currentTimeMillis() - lastMessageAt < PossessionConfig.CLIENT.hudMessageDurationMs;
    }

    public void begin(int targetEntityId, float boostedMaxHealth, String targetDisplayName) {
        this.active = true;
        this.targetEntityId = targetEntityId;
        this.boostedMaxHealth = boostedMaxHealth;
        this.targetDisplayName = targetDisplayName;
        this.actionSuppressionTicks = 5;
    }

    public void setControlRotation(float yaw, float pitch) {
        this.controlYaw = yaw;
        this.controlPitch = pitch;
    }

    public void adjustControlRotation(float yawDelta, float pitchDelta) {
        this.controlYaw = wrapDegrees(this.controlYaw + yawDelta);
        this.controlPitch = clampPitch(this.controlPitch + pitchDelta);
    }

    public void clear() {
        this.active = false;
        this.targetEntityId = -1;
        this.boostedMaxHealth = 0.0F;
        this.targetDisplayName = "";
        this.controlYaw = 0.0F;
        this.controlPitch = 0.0F;
        this.actionSuppressionTicks = 0;
    }

    public void showMessage(String message) {
        this.lastMessage = message;
        this.lastMessageAt = System.currentTimeMillis();
    }

    public void onAttachRequested() {
        this.actionSuppressionTicks = Math.max(this.actionSuppressionTicks, 6);
    }

    public void onAttachRejected() {
    }

    public void tick() {
        if (actionSuppressionTicks > 0) {
            actionSuppressionTicks--;
        }
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) {
            value -= 360.0F;
        }
        if (value < -180.0F) {
            value += 360.0F;
        }
        return value;
    }

    private static float clampPitch(float value) {
        return Math.max(-90.0F, Math.min(90.0F, value));
    }
}
