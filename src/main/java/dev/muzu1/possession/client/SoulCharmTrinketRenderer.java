package dev.muzu1.possession.client;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import dev.muzu1.possession.PossessionMod;
import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.item.PossessionItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.entity.LivingEntity;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public final class SoulCharmTrinketRenderer implements TrinketRenderer {
    private static final SoulCharmTrinketRenderer INSTANCE = new SoulCharmTrinketRenderer();
    private static final Identifier MODEL_ID = PossessionMod.id("models/trinket/soul_charm.obj");
    private static final Identifier TEXTURE_ID = PossessionMod.id("textures/trinket/soul_charm_model.png");
    private static ObjMesh cachedMesh;
    private static boolean loadingFailed;

    private SoulCharmTrinketRenderer() {
    }

    public static void register() {
        TrinketRendererRegistry.registerRenderer(PossessionItems.SOUL_CHARM, INSTANCE);
    }

    public static void clearCache() {
        cachedMesh = null;
        loadingFailed = false;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void render(ItemStack stack,
                       SlotReference slotReference,
                       EntityModel<? extends LivingEntity> contextModel,
                       MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light,
                       LivingEntity entity,
                       float limbAngle,
                       float limbDistance,
                       float tickDelta,
                       float animationProgress,
                       float headYaw,
                       float headPitch) {
        if (!(entity instanceof AbstractClientPlayerEntity player) || !(contextModel instanceof PlayerEntityModel<?> playerModel)) {
            return;
        }

        ObjMesh mesh = mesh();
        if (mesh == null) {
            return;
        }

        matrices.push();
        TrinketRenderer.followBodyRotations(entity, (PlayerEntityModel) playerModel);
        TrinketRenderer.translateToChest(matrices, (PlayerEntityModel) playerModel, player);

        // Keep the charm near the upper chest instead of sagging toward the waist.
        matrices.translate(
                PossessionConfig.CLIENT.soulCharmOffsetX,
                PossessionConfig.CLIENT.soulCharmOffsetY,
                PossessionConfig.CLIENT.soulCharmOffsetZ
        );
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrices.scale(
                PossessionConfig.CLIENT.soulCharmModelScale,
                PossessionConfig.CLIENT.soulCharmModelScale,
                PossessionConfig.CLIENT.soulCharmModelScale
        );

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE_ID));
        mesh.render(matrices, vertexConsumer, light);
        matrices.pop();
    }

    private static ObjMesh mesh() {
        if (cachedMesh != null || loadingFailed) {
            return cachedMesh;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            loadingFailed = true;
            return null;
        }

        try {
            cachedMesh = ObjMesh.load(client.getResourceManager(), MODEL_ID);
        } catch (IOException exception) {
            PossessionMod.LOGGER.error("Failed to load Soul Charm trinket model {}", MODEL_ID, exception);
            loadingFailed = true;
        }
        return cachedMesh;
    }
}
