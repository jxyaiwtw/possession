package dev.muzu1.possession.control;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class PossessionControlTracker {
    private float forward;
    private float sideways;
    private boolean jump;
    private boolean sneak;
    private boolean sprint;
    private float yaw;
    private float pitch;
    private boolean ability;
    private boolean use;
    private int focusTargetEntityId;
    private boolean hasBlockTarget;
    private BlockPos blockTargetPos;
    private Direction blockTargetSide;
    private boolean jumpPressedThisTick;
    private boolean jumpReleasedThisTick;
    private int jumpHoldTicks;
    private boolean abilityPressedThisTick;
    private boolean abilityReleasedThisTick;
    private int abilityHoldTicks;
    private boolean attack;
    private int attackTargetEntityId;

    public PossessionControlTracker() {
        this.blockTargetPos = BlockPos.ORIGIN;
        this.blockTargetSide = Direction.UP;
        this.attackTargetEntityId = -1;
    }

    public void update(float forward,
                       float sideways,
                       boolean jump,
                       boolean sneak,
                       boolean sprint,
                       float yaw,
                       float pitch,
                       boolean ability,
                       boolean attack,
                       boolean use,
                       int focusTargetEntityId,
                       int attackTargetEntityId,
                       boolean hasBlockTarget,
                       BlockPos blockTargetPos,
                       Direction blockTargetSide) {
        boolean previousJump = this.jump;
        boolean previousAbility = this.ability;

        this.forward = MathHelper.clamp(forward, -1.0F, 1.0F);
        this.sideways = MathHelper.clamp(sideways, -1.0F, 1.0F);
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = sprint;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ability = ability;
        this.use = use;
        this.focusTargetEntityId = focusTargetEntityId;
        this.hasBlockTarget = hasBlockTarget;
        this.blockTargetPos = blockTargetPos;
        this.blockTargetSide = blockTargetSide;
        this.attack = attack;
        this.attackTargetEntityId = attack ? attackTargetEntityId : -1;

        this.jumpPressedThisTick = jump && !previousJump;
        this.jumpReleasedThisTick = !jump && previousJump;
        if (jump) {
            this.jumpHoldTicks = previousJump ? this.jumpHoldTicks + 1 : 1;
        } else if (!previousJump) {
            this.jumpHoldTicks = 0;
        }

        this.abilityPressedThisTick = ability && !previousAbility;
        this.abilityReleasedThisTick = !ability && previousAbility;
        if (ability) {
            this.abilityHoldTicks = previousAbility ? this.abilityHoldTicks + 1 : 1;
        } else if (!previousAbility) {
            this.abilityHoldTicks = 0;
        }
    }

    public PossessionControlState snapshot() {
        return new PossessionControlState(
                forward,
                sideways,
                jump,
                sneak,
                sprint,
                yaw,
                pitch,
                ability,
                use,
                attack,
                attackTargetEntityId,
                focusTargetEntityId,
                hasBlockTarget,
                blockTargetPos,
                blockTargetSide,
                jumpPressedThisTick,
                jumpReleasedThisTick,
                jumpHoldTicks,
                abilityPressedThisTick,
                abilityReleasedThisTick,
                abilityHoldTicks
        );
    }

    public boolean jumpPressedThisTick() {
        return jumpPressedThisTick;
    }

    public boolean attack() {
        return attack;
    }

    public int attackTargetEntityId() {
        return attackTargetEntityId;
    }

    public void finishTick() {
        use = false;
        attack = false;
        attackTargetEntityId = -1;
        jumpPressedThisTick = false;
        jumpReleasedThisTick = false;
        abilityPressedThisTick = false;
        abilityReleasedThisTick = false;
        if (!jump) {
            jumpHoldTicks = 0;
        }
        if (!ability) {
            abilityHoldTicks = 0;
        }
    }
}
