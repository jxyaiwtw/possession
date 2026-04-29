package dev.muzu1.possession.client;

import dev.muzu1.possession.form.PossessionFormRegistry;
import dev.muzu1.possession.net.PossessionPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class PossessionModClient implements ClientModInitializer {
    private static KeyBinding attachKey;
    private static KeyBinding detachKey;
    private static KeyBinding abilityKey;
    private static KeyBinding cameraKey;
    private static final PossessionClientState STATE = new PossessionClientState();
    private static final PossessionCameraController CAMERA = new PossessionCameraController(STATE);
    private static boolean attachKeyHeld;
    private static boolean detachKeyHeld;
    private static boolean cameraKeyHeld;

    public static boolean isPossessing() {
        return STATE.isActive();
    }

    public static boolean shouldSuppressVanillaActions() {
        return STATE.isActive() || STATE.shouldSuppressActions();
    }

    public static boolean shouldBlockCameraEntity(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return CAMERA.shouldBlockCameraEntity(client, entity);
    }

    public static String getDetachKeyName() {
        return detachKey == null ? "K" : detachKey.getBoundKeyLocalizedText().getString();
    }

    public static String getAbilityKeyName() {
        return abilityKey == null ? "R" : abilityKey.getBoundKeyLocalizedText().getString();
    }

    public static boolean possessedTargetHasAbility(MinecraftClient client) {
        if (!STATE.isActive() || client == null || client.world == null) {
            return false;
        }

        Entity target = client.world.getEntityById(STATE.getTargetEntityId());
        return target instanceof LivingEntity livingTarget
                && PossessionFormRegistry.resolve(livingTarget).supportsAbility();
    }

    @Override
    public void onInitializeClient() {
        attachKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.possession.attach",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.possession"
        ));

        detachKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.possession.detach",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.possession"
        ));

        abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.possession.ability",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.possession"
        ));

        cameraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.possession.camera",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.possession"
        ));

        SoulCharmTrinketRenderer.register();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return dev.muzu1.possession.PossessionMod.id("soul_charm_trinket_reload");
            }

            @Override
            public void reload(ResourceManager manager) {
                SoulCharmTrinketRenderer.clearCache();
            }
        });

        registerNetworkHandlers();
        new PossessionHud(STATE).register();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (STATE.isActive()) {
                CAMERA.sync(client);
            }
        });
        WorldRenderEvents.START.register(context -> {
            if (STATE.isActive()) {
                CAMERA.sync(MinecraftClient.getInstance());
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
    }

    private void registerNetworkHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(PossessionPackets.S2C_ATTACH_ACCEPTED, (client, handler, buf, responseSender) -> {
            int targetEntityId = buf.readInt();
            float boostedMaxHealth = buf.readFloat();
            String displayName = buf.readString(32767);
            client.execute(() -> {
                STATE.begin(targetEntityId, boostedMaxHealth, displayName);
                CAMERA.beginPossessionCamera(client, targetEntityId);
                STATE.showMessage(Text.translatable("message.possession.attach_success").getString());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PossessionPackets.S2C_ATTACH_REJECTED, (client, handler, buf, responseSender) -> {
            String reason = buf.readString(32767);
            client.execute(() -> {
                STATE.onAttachRejected();
                STATE.showMessage(Text.translatable(reason).getString());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PossessionPackets.S2C_FORCE_EXIT, (client, handler, buf, responseSender) -> {
            String reason = buf.readString(32767);
            client.execute(() -> {
                CAMERA.detach(client);
                STATE.clear();
                STATE.showMessage(Text.translatable(reason).getString());
            });
        });
    }

    private void onEndClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            syncKeyHeldState();
            return;
        }

        if (STATE.isActive() && !client.player.isAlive()) {
            CAMERA.detach(client);
            STATE.clear();
            syncKeyHeldState();
            return;
        }

        STATE.tick();

        if (isFreshPress(attachKey, attachKeyHeld)) {
            requestAttach(client);
        }

        if (isFreshPress(detachKey, detachKeyHeld)) {
            requestDetach(client);
        }

        if (isFreshPress(cameraKey, cameraKeyHeld)) {
            CAMERA.togglePerspective(client);
        }

        if (STATE.isActive()) {
            CAMERA.sync(client);
            sendControlUpdate(client);
        }

        syncKeyHeldState();
    }

    private void requestAttach(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (STATE.isActive()) {
            STATE.showMessage(Text.translatable("message.possession.reject.already_active").getString());
            return;
        }

        if (!STATE.canRequestAttach()) {
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult hitResult)) {
            STATE.showMessage(Text.translatable("message.possession.reject.no_target").getString());
            return;
        }

        Entity target = hitResult.getEntity();
        if (!(target instanceof LivingEntity) || target instanceof PlayerEntity) {
            STATE.showMessage(Text.translatable("message.possession.reject.invalid_target").getString());
            return;
        }

        CAMERA.rememberPerspective(client);
        STATE.onAttachRequested();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(target.getId());
        ClientPlayNetworking.send(PossessionPackets.C2S_ATTACH_REQUEST, buf);
    }

    private void requestDetach(MinecraftClient client) {
        if (!STATE.isActive()) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(PossessionPackets.C2S_DETACH_REQUEST, buf);
        CAMERA.detach(client);
        STATE.clear();
    }

    private void sendControlUpdate(MinecraftClient client) {
        float forward = 0.0F;
        if (client.options.forwardKey.isPressed()) {
            forward += 1.0F;
        }
        if (client.options.backKey.isPressed()) {
            forward -= 1.0F;
        }

        float sideways = 0.0F;
        if (client.options.rightKey.isPressed()) {
            sideways -= 1.0F;
        }
        if (client.options.leftKey.isPressed()) {
            sideways += 1.0F;
        }

        boolean suppressActions = STATE.shouldSuppressActions();
        boolean attack = !suppressActions && client.options.attackKey.isPressed();
        boolean use = !suppressActions && client.options.useKey.isPressed();
        int focusTargetId = -1;
        if (client.crosshairTarget instanceof EntityHitResult hitResult) {
            focusTargetId = hitResult.getEntity().getId();
        }
        int attackTargetId = attack ? focusTargetId : -1;
        boolean ability = !suppressActions && possessedTargetHasAbility(client) && abilityKey.isPressed();
        boolean hasBlockTarget = client.crosshairTarget instanceof BlockHitResult;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(forward);
        buf.writeFloat(sideways);
        buf.writeBoolean(client.options.jumpKey.isPressed());
        buf.writeBoolean(client.options.sneakKey.isPressed());
        buf.writeBoolean(client.options.sprintKey.isPressed());
        buf.writeFloat(STATE.getControlYaw());
        buf.writeFloat(STATE.getControlPitch());
        buf.writeBoolean(ability);
        buf.writeBoolean(attack);
        buf.writeBoolean(use);
        buf.writeInt(focusTargetId);
        buf.writeInt(attackTargetId);
        buf.writeBoolean(hasBlockTarget);
        if (hasBlockTarget) {
            BlockHitResult blockHitResult = (BlockHitResult) client.crosshairTarget;
            buf.writeBlockPos(blockHitResult.getBlockPos());
            buf.writeEnumConstant(blockHitResult.getSide());
        }
        ClientPlayNetworking.send(PossessionPackets.C2S_CONTROL_UPDATE, buf);
    }

    public static void adjustControlLook(double yawDelta, double pitchDelta) {
        CAMERA.adjustControlLook(yawDelta, pitchDelta);
    }

    private static boolean isFreshPress(KeyBinding keyBinding, boolean wasHeld) {
        return keyBinding != null && keyBinding.isPressed() && !wasHeld;
    }

    private static void syncKeyHeldState() {
        attachKeyHeld = attachKey != null && attachKey.isPressed();
        detachKeyHeld = detachKey != null && detachKey.isPressed();
        cameraKeyHeld = cameraKey != null && cameraKey.isPressed();
    }
}
