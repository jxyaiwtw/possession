package dev.muzu1.possession.client;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.PossessionMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public final class PossessionHud {
    private final PossessionClientState state;

    public PossessionHud(PossessionClientState state) {
        this.state = state;
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) {
            return;
        }

        int x = PossessionConfig.CLIENT.hudX;
        int y = PossessionConfig.CLIENT.hudY;
        int lineHeight = PossessionConfig.CLIENT.hudLineHeight;

        if (state.isActive()) {
            Entity target = client.world != null ? client.world.getEntityById(state.getTargetEntityId()) : null;
            String healthText = String.format("%.1f / %.1f",
                    target instanceof LivingEntity living ? living.getHealth() : 0.0F,
                    target instanceof LivingEntity living ? living.getMaxHealth() : state.getBoostedMaxHealth()
            );

            context.drawTextWithShadow(client.textRenderer,
                    Text.translatable("hud.possession.active", state.getTargetDisplayName()),
                    x, y, 0xFFFFFF);
            y += lineHeight;

            context.drawTextWithShadow(client.textRenderer,
                    Text.translatable("hud.possession.health", healthText),
                    x, y, 0xFFFFFF);
            y += lineHeight;

            context.drawTextWithShadow(client.textRenderer,
                    Text.translatable("hud.possession.exit", PossessionModClient.getDetachKeyName()),
                    x, y, 0xFFFFFF);
            y += lineHeight;

            if (PossessionModClient.possessedTargetHasAbility(client)) {
                context.drawTextWithShadow(client.textRenderer,
                        Text.translatable("hud.possession.ability", PossessionModClient.getAbilityKeyName()),
                        x, y, 0xFFFFFF);
                y += lineHeight;
            }
        }

        if (state.shouldShowMessage()) {
            context.drawTextWithShadow(client.textRenderer, Text.literal(state.getLastMessage()).formatted(Formatting.YELLOW), x, y, 0xFFFFFF);
        }
    }
}
