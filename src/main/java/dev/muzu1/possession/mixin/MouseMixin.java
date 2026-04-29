package dev.muzu1.possession.mixin;

import dev.muzu1.possession.client.PossessionModClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.client.util.SmoothUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow
    private MinecraftClient client;

    @Shadow
    private SmoothUtil cursorXSmoother;

    @Shadow
    private SmoothUtil cursorYSmoother;

    @Shadow
    private double cursorDeltaX;

    @Shadow
    private double cursorDeltaY;

    @Shadow
    private double lastMouseUpdateTime;

    @Shadow
    public abstract boolean isCursorLocked();

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void possession$updateLook(CallbackInfo ci) {
        if (!PossessionModClient.isPossessing()) {
            return;
        }

        double now = GlfwUtil.getTime();
        double frameDelta = now - this.lastMouseUpdateTime;
        this.lastMouseUpdateTime = now;

        if (!this.isCursorLocked() || !this.client.isWindowFocused()) {
            this.cursorDeltaX = 0.0D;
            this.cursorDeltaY = 0.0D;
            ci.cancel();
            return;
        }

        double sensitivity = ((Double) this.client.options.getMouseSensitivity().getValue()).doubleValue() * 0.6000000238418579D + 0.20000000298023224D;
        double cubicSensitivity = sensitivity * sensitivity * sensitivity;
        double scaledSensitivity = cubicSensitivity * 8.0D;

        double deltaX;
        double deltaY;
        if (this.client.options.smoothCameraEnabled) {
            deltaX = this.cursorXSmoother.smooth(this.cursorDeltaX * scaledSensitivity, frameDelta * scaledSensitivity);
            deltaY = this.cursorYSmoother.smooth(this.cursorDeltaY * scaledSensitivity, frameDelta * scaledSensitivity);
        } else {
            this.cursorXSmoother.clear();
            this.cursorYSmoother.clear();
            deltaX = this.cursorDeltaX * scaledSensitivity;
            deltaY = this.cursorDeltaY * scaledSensitivity;
        }

        this.cursorDeltaX = 0.0D;
        this.cursorDeltaY = 0.0D;

        int pitchSign = ((Boolean) this.client.options.getInvertYMouse().getValue()).booleanValue() ? -1 : 1;
        this.client.getTutorialManager().onUpdateMouse(deltaX, deltaY);
        PossessionModClient.adjustControlLook(deltaX, deltaY * pitchSign);
        ci.cancel();
    }
}
