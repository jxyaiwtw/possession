package dev.muzu1.possession.mixin;

import dev.muzu1.possession.client.PossessionModClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void possession$hideExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        if (PossessionModClient.isPossessing()) {
            ci.cancel();
        }
    }
}
