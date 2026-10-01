package it.ratlab.beamlights.compat.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.api.BeamItemData;
import it.ratlab.beamlights.api.BeamLightsApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/** The BeamLights script binding; works on the side of the given entity (server scripts: server definitions). */
public final class BeamLightsBindingJS {
    /** True when any provider emits a lit beam (luminance > 0) for the entity right now. */
    public boolean isBeamActive(Entity entity) {
        return BeamLightsApi.isBeamActive(entity);
    }

    /** Number of lit beams the entity emits right now. */
    public int beamCount(Entity entity) {
        int n = 0;
        for (Beam b : BeamLightsApi.getBeams(entity)) {
            if (b.luminance() > 0) n++;
        }
        return n;
    }

    /** Lit and unlit beams the entity emits right now (read-only snapshot). */
    public List<Beam> beams(Entity entity) {
        return BeamLightsApi.getBeams(entity);
    }

    /** The beam data of the stack (custom_data "beamlights:beam") or null. */
    public BeamItemData getBeam(ItemStack stack) {
        return BeamLightsApi.getBeam(stack).orElse(null);
    }

    /**
     * Gives the stack a beam: {@code BeamLights.setBeam(item, {luminance: 12, range: 20, color: '#FFE8B0'})}. Accepts a
     * script object, a JSON string or a BeamItemData; invalid values throw with a readable reason.
     */
    public void setBeam(ItemStack stack, Object data) {
        BeamItemData beam;
        if (data instanceof BeamItemData d) {
            beam = d;
        } else {
            JsonElement json = data instanceof CharSequence s ? JsonParser.parseString(s.toString()) : toJson(data);
            beam = BeamItemData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalArgumentException::new);
        }
        BeamLightsApi.setBeam(stack, beam);
    }

    /** Removes the beam data from the stack. */
    public void clearBeam(ItemStack stack) {
        BeamLightsApi.clearBeam(stack);
    }

    // Own conversion: script objects hold Rhino wrappers (ConsString, NativeArray), which Gson reflection mishandles.
    private static JsonElement toJson(Object o) {
        if (o == null) return JsonNull.INSTANCE;
        if (o instanceof Boolean b) return new JsonPrimitive(b);
        if (o instanceof Number n) return new JsonPrimitive(n);
        if (o instanceof CharSequence s) return new JsonPrimitive(s.toString());
        if (o instanceof Map<?, ?> m) {
            JsonObject obj = new JsonObject();
            m.forEach((k, v) -> obj.add(String.valueOf(k), toJson(v)));
            return obj;
        }
        if (o instanceof Iterable<?> it) {
            JsonArray arr = new JsonArray();
            it.forEach(v -> arr.add(toJson(v)));
            return arr;
        }
        throw new IllegalArgumentException("Unsupported beam value: " + o.getClass().getSimpleName());
    }
}
