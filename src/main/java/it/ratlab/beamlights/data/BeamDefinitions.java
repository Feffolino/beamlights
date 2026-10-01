package it.ratlab.beamlights.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.core.BeamDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loaded beam definitions of one side. The server copy comes from the datapack reload, the client copy from the sync
 * payload (raw JSON, parsed again here). Item to candidates is cached and dropped on reload and on tag updates.
 */
public final class BeamDefinitions {
    public static final BeamDefinitions SERVER = new BeamDefinitions("server");
    public static final BeamDefinitions CLIENT = new BeamDefinitions("client");

    /** A definition with its item ids and tags resolved; sorted by priority (high first), then file id. */
    public record Entry(ResourceLocation id, BeamDefinition def, List<Item> items, List<TagKey<Item>> tags,
                        DataComponentType<?> component) {
        boolean matches(Item item) {
            if (items.contains(item)) return true;
            for (TagKey<Item> t : tags) {
                if (item.builtInRegistryHolder().is(t)) return true;
            }
            return false;
        }
    }

    private final String side;
    private volatile List<Entry> entries = List.of();
    private volatile Map<ResourceLocation, String> raw = Map.of();
    private volatile boolean usesCurios;
    private final Map<Item, List<Entry>> byItem = new ConcurrentHashMap<>();

    private BeamDefinitions(String side) {
        this.side = side;
    }

    public static BeamDefinitions of(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean usesCurios() {
        return usesCurios;
    }

    /** Raw JSON per file id, as sent to clients. */
    public Map<ResourceLocation, String> raw() {
        return raw;
    }

    public void clear() {
        load(Map.of(), false);
    }

    /** Server reload: parsed JSON per file id. */
    public void loadJson(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, String> r = new TreeMap<>();
        files.forEach((id, json) -> r.put(id, json.toString()));
        load(r, true);
    }

    /** Client sync: raw JSON per file id; errors were already logged by the server, so only at debug level here. */
    public void loadRaw(Map<ResourceLocation, String> files) {
        load(new TreeMap<>(files), false);
    }

    private void load(Map<ResourceLocation, String> files, boolean warn) {
        List<Entry> list = new ArrayList<>();
        Map<ResourceLocation, String> kept = new LinkedHashMap<>();
        boolean curios = false;
        for (Map.Entry<ResourceLocation, String> f : files.entrySet()) {
            try {
                Entry e = resolve(f.getKey(), BeamDefinition.parse(JsonParser.parseString(f.getValue())), warn);
                list.add(e);
                kept.put(f.getKey(), f.getValue());
                curios |= e.def().slots().contains(BeamDefinition.Slot.CURIOS);
            } catch (RuntimeException ex) {
                if (warn) {
                    BeamLights.LOG.warn("Beam Lights: skipped beam definition {}: {}", f.getKey(), ex.getMessage());
                } else {
                    BeamLights.LOG.debug("Beam Lights: skipped beam definition {}: {}", f.getKey(), ex.getMessage());
                }
            }
        }
        list.sort(Comparator.comparingInt((Entry e) -> -e.def().priority()).thenComparing(Entry::id));
        entries = List.copyOf(list);
        raw = Map.copyOf(kept);
        usesCurios = curios;
        byItem.clear();
        if (warn || !list.isEmpty()) {
            BeamLights.LOG.info("Beam Lights: {} beam definitions loaded ({})", list.size(), side);
        }
    }

    private static Entry resolve(ResourceLocation id, BeamDefinition def, boolean warn) {
        List<Item> items = new ArrayList<>();
        List<TagKey<Item>> tags = new ArrayList<>();
        for (String s : def.items()) {
            if (BeamDefinition.isTag(s)) {
                tags.add(TagKey.create(Registries.ITEM, ResourceLocation.parse(s.substring(1))));
            } else {
                ResourceLocation rl = ResourceLocation.parse(s);
                if (BuiltInRegistries.ITEM.containsKey(rl)) {
                    items.add(BuiltInRegistries.ITEM.get(rl));
                } else if (warn) {
                    // Optional mod not installed: not an error, the entry is just ignored.
                    BeamLights.LOG.debug("Beam Lights: {}: unknown item {} ignored", id, s);
                }
            }
        }
        if (items.isEmpty() && tags.isEmpty()) throw new IllegalArgumentException("no known item in 'items'");
        DataComponentType<?> component = null;
        if (def.condition() != null) {
            ResourceLocation rl = ResourceLocation.parse(def.condition().component());
            component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(rl);
            if (component == null) throw new IllegalArgumentException("unknown data component " + rl);
            if (def.condition().expected() != null && component.codec() == null) {
                throw new IllegalArgumentException("component " + rl + " has no codec, use \"present\": true");
            }
        }
        return new Entry(id, def, List.copyOf(items), List.copyOf(tags), component);
    }

    /** Called when tags change: tag membership is resolved lazily and cached per item. */
    public void invalidateTags() {
        byItem.clear();
    }

    public boolean hasCandidates(Item item) {
        return !candidates(item).isEmpty();
    }

    public List<Entry> candidates(Item item) {
        List<Entry> all = entries;
        if (all.isEmpty()) return List.of();
        return byItem.computeIfAbsent(item, i -> {
            List<Entry> c = new ArrayList<>();
            for (Entry e : all) {
                if (e.matches(i)) c.add(e);
            }
            return c.isEmpty() ? List.of() : List.copyOf(c);
        });
    }

    /** Highest-priority definition allowed in this slot whose condition passes, or null. */
    public Entry best(ItemStack stack, BeamDefinition.Slot slot, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return null;
        for (Entry e : candidates(stack.getItem())) {
            if (e.def().slots().contains(slot) && conditionMet(e, stack, registries)) return e;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static boolean conditionMet(Entry e, ItemStack stack, HolderLookup.Provider registries) {
        BeamDefinition.Condition c = e.def().condition();
        if (c == null) return true;
        Object value = stack.get(e.component());
        if (value == null) return false;
        if (c.expected() == null) return true;
        Codec<Object> codec = (Codec<Object>) e.component().codec();
        if (codec == null) return false;
        JsonElement encoded = codec.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), value)
                .result().orElse(null);
        return c.expected().equals(encoded);
    }
}
