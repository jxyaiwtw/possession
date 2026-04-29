package dev.muzu1.possession.item;

import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import dev.muzu1.possession.PossessionMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class PossessionItems {
    private static final String SOUL_CHARM_GRANT_STATE_ID = PossessionMod.MOD_ID + "_soul_charm_grants";

    public static final Item SOUL_CHARM = new SoulCharmItem(new Item.Settings().maxCount(1));

    private PossessionItems() {
    }

    public static void register() {
        Registry.register(Registries.ITEM, PossessionMod.id("soul_charm"), SOUL_CHARM);
        TrinketsApi.registerTrinket(SOUL_CHARM, (TrinketItem) SOUL_CHARM);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(SOUL_CHARM));
    }

    public static boolean hasSoulCharm(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity)
                .map(component -> component.isEquipped(SOUL_CHARM))
                .orElse(false);
    }

    public static void grantSoulCharmAfterFirstDeath(ServerPlayerEntity player) {
        SoulCharmGrantState state = player.getServer().getOverworld().getPersistentStateManager()
                .getOrCreate(SoulCharmGrantState::fromNbt, SoulCharmGrantState::new, SOUL_CHARM_GRANT_STATE_ID);
        if (!state.markGranted(player.getUuid())) {
            return;
        }

        ItemStack stack = new ItemStack(SOUL_CHARM);
        if (!player.getInventory().contains(stack) && !hasSoulCharm(player)) {
            boolean inserted = player.giveItemStack(stack);
            if (!inserted && !stack.isEmpty()) {
                player.dropItem(stack, false);
            }
        }
        player.sendMessage(Text.translatable("message.possession.soul_charm_granted"), false);
    }
}
