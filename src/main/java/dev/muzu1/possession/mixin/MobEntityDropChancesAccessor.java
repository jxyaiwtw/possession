package dev.muzu1.possession.mixin;

import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEntity.class)
public interface MobEntityDropChancesAccessor {
    @Accessor("handDropChances")
    float[] possession$getHandDropChances();

    @Accessor("armorDropChances")
    float[] possession$getArmorDropChances();
}
