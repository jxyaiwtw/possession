package dev.muzu1.possession.mixin;

import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    @Accessor("persistent")
    boolean possession$isPersistent();

    @Accessor("persistent")
    void possession$setPersistent(boolean persistent);
}
