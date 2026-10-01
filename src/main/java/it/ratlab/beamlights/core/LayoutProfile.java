package it.ratlab.beamlights.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ray layout per beam provider, from client resources assets/&lt;ns&gt;/beamlights/layouts/*.json:
 * <pre>{ "providers": ["omegaflashlight"], "indoor": "CENTER_ONLY", "outdoor": "TRIANGLE", "cone": true,
 *   "priority": 0 }</pre>
 * indoor = side rays while the beam has a cone (or the cone light is off), outdoor = open area (no cone);
 * cone = false turns the cone light off for these beams. Missing fields fall back to the client config
 * (rayPattern, openAreaPattern, ldlConeLight). Pure (Gson only).
 */
public record LayoutProfile(List<String> providers, RayLayout.Pattern indoor, RayLayout.Pattern outdoor,
                            Boolean cone, int priority) {

    public static LayoutProfile parse(JsonElement json) {
        if (json == null || !json.isJsonObject()) throw new IllegalArgumentException("expected a JSON object");
        JsonObject o = json.getAsJsonObject();
        List<String> providers = new ArrayList<>();
        JsonElement pe = o.get("providers");
        if (pe == null || !pe.isJsonArray()) throw new IllegalArgumentException("'providers' must be an array");
        for (JsonElement e : (JsonArray) pe) {
            if (!e.isJsonPrimitive()) throw new IllegalArgumentException("'providers' entries must be strings");
            providers.add(e.getAsString().trim());
        }
        if (providers.isEmpty()) throw new IllegalArgumentException("'providers' is empty");
        RayLayout.Pattern indoor = pattern(o, "indoor");
        RayLayout.Pattern outdoor = pattern(o, "outdoor");
        Boolean cone = o.has("cone") ? o.get("cone").getAsBoolean() : null;
        int priority = o.has("priority") ? o.get("priority").getAsInt() : 0;
        if (indoor == null && outdoor == null && cone == null) {
            throw new IllegalArgumentException("set at least one of 'indoor', 'outdoor', 'cone'");
        }
        return new LayoutProfile(List.copyOf(providers), indoor, outdoor, cone, priority);
    }

    private static RayLayout.Pattern pattern(JsonObject o, String key) {
        if (!o.has(key)) return null;
        String s = o.get(key).getAsString().trim().toUpperCase(Locale.ROOT);
        try {
            return RayLayout.Pattern.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown '" + key + "' pattern " + s);
        }
    }

    public RayLayout.Pattern indoorOr(RayLayout.Pattern fallback) {
        return indoor != null ? indoor : fallback;
    }

    public RayLayout.Pattern outdoorOr(RayLayout.Pattern fallback) {
        return outdoor != null ? outdoor : fallback;
    }

    public boolean coneOr(boolean fallback) {
        return cone != null ? cone : fallback;
    }
}
