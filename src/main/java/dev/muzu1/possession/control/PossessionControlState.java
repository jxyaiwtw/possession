package dev.muzu1.possession.control;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record PossessionControlState(
        float forward,
        float sideways,
        boolean jump,
        boolean sneak,
        boolean sprint,
        float yaw,
        float pitch,
        boolean ability,
        boolean use,
        boolean attack,
        int attackTargetEntityId,
        int focusTargetEntityId,
        boolean hasBlockTarget,
        BlockPos blockTargetPos,
        Direction blockTargetSide,
        boolean jumpPressed,
        boolean jumpReleased,
        int jumpHoldTicks,
        boolean abilityPressed,
        boolean abilityReleased,
        int abilityHoldTicks
) {
}
