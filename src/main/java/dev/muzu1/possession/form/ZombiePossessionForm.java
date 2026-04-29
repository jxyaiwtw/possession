package dev.muzu1.possession.form;

import com.google.common.collect.Multimap;
import dev.muzu1.possession.control.PossessionControlState;
import dev.muzu1.possession.mixin.MobEntityDropChancesAccessor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class ZombiePossessionForm extends ConfigBackedPossessionForm {
    public static final ZombiePossessionForm INSTANCE = new ZombiePossessionForm();
    private static final EquipmentSlot[] INHERITED_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback MOVEMENT =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.MovementFallback(0.04D, null, null, null, null, null, null, 0.08D, 0.5F);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback STATS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.StatFallback(0.0D, 0.15D);
    private static final dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback ACTIONS =
            new dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback(2, 10, 12, 0, 8);

    private final Map<UUID, EquipmentSnapshot> snapshots = new java.util.HashMap<>();
    private final Map<UUID, Double> inheritedWeaponDamageBonuses = new java.util.HashMap<>();
    private final Map<UUID, EquipmentLoadout> pendingEquipmentLoads = new java.util.HashMap<>();

    private ZombiePossessionForm() {
        super("zombie");
    }

    @Override
    public double movementSpeedBonus(LivingEntity target, PossessionControlState control) {
        return profile().resolveMovementSpeedBonus(target, control, MOVEMENT);
    }

    @Override
    public double attackDamageBonus(LivingEntity target) {
        return profile().resolveAttackDamageBonus(STATS) + inheritedWeaponDamageBonuses.getOrDefault(target.getUuid(), 0.0D);
    }

    @Override
    public int attackCooldownTicks(LivingEntity target, PossessionControlState control) {
        return profile().resolveAttackCooldownTicks(control, ACTIONS);
    }

    @Override
    public void onPossessionStarted(ServerPlayerEntity player, LivingEntity target) {
        snapshots.put(target.getUuid(), EquipmentSnapshot.capture(target));
        inheritedWeaponDamageBonuses.put(target.getUuid(), getMainHandAttackDamageBonus(player));
        pendingEquipmentLoads.put(target.getUuid(), EquipmentLoadout.fromPlayer(player));
    }

    @Override
    public void applyExtraMovement(LivingEntity target, PossessionControlState control) {
        EquipmentLoadout pendingLoadout = pendingEquipmentLoads.remove(target.getUuid());
        if (pendingLoadout != null) {
            pendingLoadout.apply(target);
        }
    }

    @Override
    public void onPossessionEnded(LivingEntity target) {
        EquipmentSnapshot snapshot = snapshots.remove(target.getUuid());
        inheritedWeaponDamageBonuses.remove(target.getUuid());
        pendingEquipmentLoads.remove(target.getUuid());
        if (snapshot != null) {
            snapshot.restore(target);
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

    @Override
    protected dev.muzu1.possession.config.PossessionConfig.FormProfile.ActionFallback actionFallback() {
        return ACTIONS;
    }

    private static void inheritPlayerEquipment(EquipmentLoadout loadout, LivingEntity target) {
        for (EquipmentSlot slot : INHERITED_SLOTS) {
            target.equipStack(slot, loadout.stacks.getOrDefault(slot, ItemStack.EMPTY).copy());
        }

        if (target instanceof MobEntity mob) {
            for (EquipmentSlot slot : INHERITED_SLOTS) {
                mob.setEquipmentDropChance(slot, 0.0F);
            }
        }
    }

    private static double getMainHandAttackDamageBonus(LivingEntity target) {
        ItemStack stack = target.getEquippedStack(EquipmentSlot.MAINHAND);
        if (stack.isEmpty()) {
            return 0.0D;
        }

        Multimap<EntityAttribute, EntityAttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        return modifiers.get(EntityAttributes.GENERIC_ATTACK_DAMAGE).stream()
                .mapToDouble(EntityAttributeModifier::getValue)
                .sum();
    }

    private static final class EquipmentLoadout {
        private final EnumMap<EquipmentSlot, ItemStack> stacks;

        private EquipmentLoadout(EnumMap<EquipmentSlot, ItemStack> stacks) {
            this.stacks = stacks;
        }

        private static EquipmentLoadout fromPlayer(ServerPlayerEntity player) {
            EnumMap<EquipmentSlot, ItemStack> stacks = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : INHERITED_SLOTS) {
                stacks.put(slot, player.getEquippedStack(slot).copy());
            }
            return new EquipmentLoadout(stacks);
        }

        private void apply(LivingEntity target) {
            inheritPlayerEquipment(this, target);
        }
    }

    private static final class EquipmentSnapshot {
        private final EnumMap<EquipmentSlot, ItemStack> stacks;
        private final EnumMap<EquipmentSlot, Float> dropChances;

        private EquipmentSnapshot(EnumMap<EquipmentSlot, ItemStack> stacks, EnumMap<EquipmentSlot, Float> dropChances) {
            this.stacks = stacks;
            this.dropChances = dropChances;
        }

        private static EquipmentSnapshot capture(LivingEntity target) {
            EnumMap<EquipmentSlot, ItemStack> stacks = new EnumMap<>(EquipmentSlot.class);
            EnumMap<EquipmentSlot, Float> dropChances = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : INHERITED_SLOTS) {
                stacks.put(slot, target.getEquippedStack(slot).copy());
                dropChances.put(slot, target instanceof MobEntity mob
                        ? readDropChance(mob, slot)
                        : 0.0F);
            }
            return new EquipmentSnapshot(stacks, dropChances);
        }

        private void restore(LivingEntity target) {
            for (EquipmentSlot slot : INHERITED_SLOTS) {
                target.equipStack(slot, stacks.getOrDefault(slot, ItemStack.EMPTY).copy());
            }

            if (target instanceof MobEntity mob) {
                for (EquipmentSlot slot : INHERITED_SLOTS) {
                    mob.setEquipmentDropChance(slot, dropChances.getOrDefault(slot, 0.0F));
                }
            }
        }

        private static float readDropChance(MobEntity mob, EquipmentSlot slot) {
            MobEntityDropChancesAccessor accessor = (MobEntityDropChancesAccessor) mob;
            return switch (slot.getType()) {
                case HAND -> accessor.possession$getHandDropChances()[slot.getEntitySlotId()];
                case ARMOR -> accessor.possession$getArmorDropChances()[slot.getEntitySlotId()];
            };
        }
    }
}
