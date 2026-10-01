package it.ratlab.beamlights.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.ratlab.beamlights.api.math.V3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One data-driven beam (data/&lt;ns&gt;/beamlights/beams/*.json), validated, no Minecraft classes. Item entries are
 * "ns:path" ids or "#ns:path" tags (no namespace = minecraft).
 *
 * @param condition null = always on
 */
public record BeamDefinition(List<String> items, List<Slot> slots, int luminance, float range, float cone, int rgb,
                             Condition condition, double forward, double down, int priority) {
    public static final float DEFAULT_RANGE = 16f;
    public static final float DEFAULT_CONE = 25f;
    public static final int WHITE = 0xFFFFFF;

    private static final Pattern ID = Pattern.compile("([a-z0-9_.-]+:)?[a-z0-9_./-]+");

    public enum Slot {
        MAINHAND, OFFHAND, HEAD, CURIOS;

        private final String id = name().toLowerCase(Locale.ROOT);

        /** Lower case id as used in JSON and the item component. */
        public String id() {
            return id;
        }

        public static Slot parse(String s) {
            for (Slot v : values()) {
                if (v.id.equals(s)) return v;
            }
            throw new IllegalArgumentException("unknown slot '" + s + "' (mainhand, offhand, head, curios)");
        }
    }

    /**
     * Data component check on the stack.
     *
     * @param expected "equals" value as JSON (compared with the component encoded by its codec); null = presence only
     */
    public record Condition(String component, JsonElement expected) {
    }

    /** Parses and validates one file; throws IllegalArgumentException with a readable reason. */
    public static BeamDefinition parse(JsonElement json) {
        if (json == null || !json.isJsonObject()) throw new IllegalArgumentException("expected a JSON object");
        JsonObject o = json.getAsJsonObject();

        List<String> items = new ArrayList<>();
        JsonArray arr = array(o, "items", true);
        for (JsonElement e : arr) items.add(itemEntry(string(e, "items entry")));
        if (items.isEmpty()) throw new IllegalArgumentException("'items' is empty");

        List<Slot> slots = new ArrayList<>();
        JsonArray sa = array(o, "slots", false);
        if (sa == null) {
            slots.add(Slot.MAINHAND);
            slots.add(Slot.OFFHAND);
        } else {
            for (JsonElement e : sa) {
                Slot s = Slot.parse(string(e, "slots entry").toLowerCase(Locale.ROOT));
                if (!slots.contains(s)) slots.add(s);
            }
            if (slots.isEmpty()) throw new IllegalArgumentException("'slots' is empty");
        }

        if (!o.has("luminance")) throw new IllegalArgumentException("missing 'luminance'");
        int luminance = (int) range(number(o, "luminance", 0), 0, 15, "luminance");
        if (number(o, "luminance", 0) != luminance) throw new IllegalArgumentException("'luminance' must be an integer");
        float range = (float) range(number(o, "range", DEFAULT_RANGE), 1, 128, "range");
        float cone = (float) range(number(o, "cone", DEFAULT_CONE), 1, 89, "cone");
        int rgb = o.has("color") ? parseColor(string(o.get("color"), "color")) : WHITE;
        int priority = (int) number(o, "priority", 0);

        double forward = 0, down = 0;
        if (o.has("origin")) {
            JsonElement oe = o.get("origin");
            if (!oe.isJsonObject()) throw new IllegalArgumentException("'origin' must be an object");
            JsonObject oo = oe.getAsJsonObject();
            forward = range(number(oo, "forward", 0), -4, 4, "origin.forward");
            down = range(number(oo, "down", 0), -4, 4, "origin.down");
        }

        Condition condition = null;
        if (o.has("condition")) condition = condition(o.get("condition"));

        return new BeamDefinition(List.copyOf(items), List.copyOf(slots), luminance, range, cone, rgb, condition,
                forward, down, priority);
    }

    /** Beam start: eye + look * forward, moved down by 'down' blocks (world up axis). */
    public V3 origin(V3 eye, V3 look) {
        return origin(eye, look, forward, down);
    }

    /** Beam start for explicit offsets (item component). */
    public static V3 origin(V3 eye, V3 look, double forward, double down) {
        if (forward == 0 && down == 0) return eye;
        return eye.add(look.normalize().scale(forward)).sub(new V3(0, down, 0));
    }

    /** "#RRGGBB", "RRGGBB" or "0xRRGGBB" to 0xRRGGBB. */
    public static int parseColor(String s) {
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        else if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
        if (t.length() != 6) throw new IllegalArgumentException("bad color '" + s + "' (expected #RRGGBB)");
        try {
            return Integer.parseInt(t, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad color '" + s + "' (expected #RRGGBB)");
        }
    }

    /** Normalized item entry: lower case, namespace added; tags keep the leading '#'. */
    public static String itemEntry(String s) {
        String t = s.trim();
        boolean tag = t.startsWith("#");
        if (tag) t = t.substring(1);
        if (!ID.matcher(t).matches()) throw new IllegalArgumentException("bad item id '" + s + "'");
        if (!t.contains(":")) t = "minecraft:" + t;
        return tag ? "#" + t : t;
    }

    public static boolean isTag(String entry) {
        return entry.startsWith("#");
    }

    private static Condition condition(JsonElement e) {
        if (!e.isJsonObject()) throw new IllegalArgumentException("'condition' must be an object");
        JsonObject c = e.getAsJsonObject();
        if (!c.has("component")) throw new IllegalArgumentException("'condition' needs 'component'");
        String component = itemEntry(string(c.get("component"), "condition.component"));
        if (isTag(component)) throw new IllegalArgumentException("'condition.component' cannot be a tag");
        boolean hasEquals = c.has("equals");
        boolean hasPresent = c.has("present");
        if (hasEquals == hasPresent) {
            throw new IllegalArgumentException("'condition' needs exactly one of 'equals' or 'present'");
        }
        if (hasPresent) {
            JsonElement p = c.get("present");
            if (!p.isJsonPrimitive() || !p.getAsJsonPrimitive().isBoolean() || !p.getAsBoolean()) {
                throw new IllegalArgumentException("'condition.present' must be true");
            }
            return new Condition(component, null);
        }
        return new Condition(component, c.get("equals").deepCopy());
    }

    private static JsonArray array(JsonObject o, String key, boolean required) {
        if (!o.has(key)) {
            if (required) throw new IllegalArgumentException("missing '" + key + "'");
            return null;
        }
        JsonElement e = o.get(key);
        if (e.isJsonArray()) return e.getAsJsonArray();
        // A single string is accepted as a one-element list.
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            JsonArray a = new JsonArray();
            a.add(e);
            return a;
        }
        throw new IllegalArgumentException("'" + key + "' must be a list");
    }

    private static String string(JsonElement e, String what) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(what + " must be a string");
        }
        return e.getAsString();
    }

    private static double number(JsonObject o, String key, double def) {
        if (!o.has(key)) return def;
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive() || !((JsonPrimitive) e).isNumber()) {
            throw new IllegalArgumentException("'" + key + "' must be a number");
        }
        return e.getAsDouble();
    }

    private static double range(double v, double lo, double hi, String key) {
        if (Double.isNaN(v) || v < lo || v > hi) {
            throw new IllegalArgumentException("'" + key + "' = " + v + " out of range " + lo + ".." + hi);
        }
        return v;
    }
}
