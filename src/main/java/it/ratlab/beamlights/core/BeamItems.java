package it.ratlab.beamlights.core;

import com.mojang.serialization.DataResult;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.api.BeamItemData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Reads and writes the beam of a stack in the vanilla {@code minecraft:custom_data} component under the key
 * {@code beamlights:beam}. No registry entry, so the mod stays optional on both sides. Decoded values are cached by
 * CustomData instance (immutable), so a held item is not decoded every tick.
 */
public final class BeamItems {
    public static final String KEY = "beamlights:beam";
    private static final int CACHE_SIZE = 64;
    private static final int MAX_WARNED = 64;

    private static final CustomData[] KEYS = new CustomData[CACHE_SIZE];
    @SuppressWarnings("unchecked")
    private static final Optional<BeamItemData>[] VALUES = new Optional[CACHE_SIZE];
    private static int next;
    private static final Set<String> WARNED = new HashSet<>();

    private BeamItems() {
    }

    /** The beam stored in the stack, empty when absent or invalid (invalid data is logged once per message). */
    public static Optional<BeamItemData> get(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(KEY)) return Optional.empty();
        synchronized (KEYS) {
            for (int i = 0; i < CACHE_SIZE; i++) {
                if (KEYS[i] == data) return VALUES[i];
            }
        }
        Optional<BeamItemData> decoded = decode(data.copyTag().get(KEY));
        synchronized (KEYS) {
            KEYS[next] = data;
            VALUES[next] = decoded;
            next = (next + 1) % CACHE_SIZE;
        }
        return decoded;
    }

    /** True when the stack carries valid beam data. */
    public static boolean has(ItemStack stack) {
        return get(stack).isPresent();
    }

    public static void set(ItemStack stack, BeamItemData beam) {
        Tag encoded = BeamItemData.CODEC.encodeStart(NbtOps.INSTANCE, beam).getOrThrow(IllegalArgumentException::new);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(KEY, encoded));
    }

    public static void clear(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(KEY)) return;
        CompoundTag tag = data.copyTag();
        tag.remove(KEY);
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static Optional<BeamItemData> decode(Tag tag) {
        DataResult<BeamItemData> r = BeamItemData.CODEC.parse(NbtOps.INSTANCE, tag);
        r.error().ifPresent(e -> warnOnce(e.message()));
        return r.result();
    }

    private static void warnOnce(String message) {
        boolean first;
        synchronized (WARNED) {
            first = WARNED.size() < MAX_WARNED && WARNED.add(message);
        }
        if (first) BeamLights.LOG.warn("Beam Lights: invalid {} item data ignored: {}", KEY, message);
    }
}
