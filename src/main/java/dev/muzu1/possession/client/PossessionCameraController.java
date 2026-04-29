package dev.muzu1.possession.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public final class PossessionCameraController {
    private final PossessionClientState state;
    private Perspective previousPerspective;
    private boolean returningCameraToPlayer;

    public PossessionCameraController(PossessionClientState state) {
        this.state = state;
    }

    public boolean shouldBlockCameraEntity(MinecraftClient client, Entity entity) {
        if (!state.isActive() || returningCameraToPlayer || client == null || client.world == null) {
            return false;
        }

        Entity target = client.world.getEntityById(state.getTargetEntityId());
        return target != null && entity != target;
    }

    public void rememberPerspective(MinecraftClient client) {
        previousPerspective = client.options.getPerspective();
    }

    public void beginPossessionCamera(MinecraftClient client, int targetEntityId) {
        Entity target = client.world != null ? client.world.getEntityById(targetEntityId) : null;
        if (target != null) {
            if (target instanceof LivingEntity livingTarget) {
                state.setControlRotation(livingTarget.getYaw(), livingTarget.getPitch());
            } else {
                state.setControlRotation(target.getYaw(), target.getPitch());
            }
            client.setCameraEntity(target);
        } else if (client.player != null) {
            state.setControlRotation(client.player.getYaw(), client.player.getPitch());
            client.setCameraEntity(client.player);
        }
    }

    public void sync(MinecraftClient client) {
        if (client == null) {
            return;
        }

        Entity target = client.world != null ? client.world.getEntityById(state.getTargetEntityId()) : null;
        if (target == null) {
            return;
        }

        if (client.getCameraEntity() != target) {
            client.setCameraEntity(target);
        }

        float yaw = state.getControlYaw();
        float pitch = state.getControlPitch();
        target.setYaw(yaw);
        target.setPitch(pitch);
        target.prevYaw = yaw;
        target.prevPitch = pitch;
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.setHeadYaw(yaw);
            livingTarget.setBodyYaw(yaw);
        }
    }

    public void adjustControlLook(double yawDelta, double pitchDelta) {
        if (!state.isActive()) {
            return;
        }

        state.adjustControlRotation((float) (yawDelta * 0.15D), (float) (pitchDelta * 0.15D));
        sync(MinecraftClient.getInstance());
    }

    public void detach(MinecraftClient client) {
        returningCameraToPlayer = true;
        try {
            if (client.player != null) {
                client.setCameraEntity(client.player);
            }
        } finally {
            returningCameraToPlayer = false;
        }
        restorePerspective(client);
    }

    public void togglePerspective(MinecraftClient client) {
        if (!state.isActive()) {
            return;
        }

        Perspective perspective = client.options.getPerspective();
        client.options.setPerspective(perspective.isFirstPerson() ? Perspective.THIRD_PERSON_BACK : Perspective.FIRST_PERSON);
    }

    private void restorePerspective(MinecraftClient client) {
        if (previousPerspective != null) {
            client.options.setPerspective(previousPerspective);
            previousPerspective = null;
        }
    }
}
