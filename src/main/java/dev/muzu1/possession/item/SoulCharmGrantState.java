package dev.muzu1.possession.item;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SoulCharmGrantState extends PersistentState {
    private static final String GRANTED_KEY = "Granted";

    private final Set<UUID> grantedPlayers = new HashSet<>();

    public static SoulCharmGrantState fromNbt(NbtCompound nbt) {
        SoulCharmGrantState state = new SoulCharmGrantState();
        NbtCompound granted = nbt.getCompound(GRANTED_KEY);
        for (String key : granted.getKeys()) {
            if (granted.getBoolean(key)) {
                try {
                    state.grantedPlayers.add(UUID.fromString(key));
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed legacy entries instead of preventing the world from loading.
                }
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound granted = new NbtCompound();
        for (UUID uuid : grantedPlayers) {
            granted.putBoolean(uuid.toString(), true);
        }
        nbt.put(GRANTED_KEY, granted);
        return nbt;
    }

    public boolean markGranted(UUID uuid) {
        boolean added = grantedPlayers.add(uuid);
        if (added) {
            markDirty();
        }
        return added;
    }
}
