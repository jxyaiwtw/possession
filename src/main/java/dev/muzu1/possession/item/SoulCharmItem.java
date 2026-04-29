package dev.muzu1.possession.item;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public final class SoulCharmItem extends TrinketItem {
    public SoulCharmItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return "chest".equals(slot.inventory().getSlotType().getGroup())
                && "necklace".equals(slot.inventory().getSlotType().getName());
    }
}
