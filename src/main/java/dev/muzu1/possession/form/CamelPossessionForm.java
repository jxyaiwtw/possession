package dev.muzu1.possession.form;

import dev.muzu1.possession.config.PossessionConfig;
import dev.muzu1.possession.control.PossessionControlState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

public final class CamelPossessionForm extends ConfigBackedPossessionForm {
    public static final CamelPossessionForm INSTANCE = new CamelPossessionForm();
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.08D, null, null, null, null, null, null, null, null);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(1.2D, 0.18D);

    private CamelPossessionForm() {
        super("camel");
    }

    @Override
    public void applyExtraMovement(LivingEntity target, PossessionControlState control) {
        BlockPos below = target.getBlockPos().down();
        BlockState state = target.getWorld().getBlockState(below);
        if ((state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND)) && control.forward() != 0.0F) {
            double radians = Math.toRadians(control.yaw());
            double sandBoost = PossessionConfig.BEHAVIORS.camel.sandBoost;
            double boostX = -Math.sin(radians) * sandBoost;
            double boostZ = Math.cos(radians) * sandBoost;
            target.addVelocity(boostX, 0.0D, boostZ);
        }
    }

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback movementFallback() {
        return MOVEMENT;
    }

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback statFallback() {
        return STATS;
    }
}
