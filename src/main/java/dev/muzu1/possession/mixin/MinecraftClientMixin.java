package dev.muzu1.possession.mixin;

import dev.muzu1.possession.client.PossessionModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "setCameraEntity", at = @At("HEAD"), cancellable = true)
    private void possession$keepPossessionCamera(Entity entity, CallbackInfo ci) {
        if (PossessionModClient.shouldBlockCameraEntity(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void possession$cancelAttack(CallbackInfoReturnable<Boolean> cir) {
        if (PossessionModClient.shouldSuppressVanillaActions()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void possession$cancelUse(CallbackInfo ci) {
        if (PossessionModClient.shouldSuppressVanillaActions()) {
            ci.cancel();
        }
    }
}
