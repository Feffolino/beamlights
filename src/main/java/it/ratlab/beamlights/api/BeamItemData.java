package it.ratlab.beamlights.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.ratlab.beamlights.core.BeamDefinition;
import it.ratlab.beamlights.core.BeamValues;

import java.util.List;
import java.util.Locale;

/**
 * Beam data of one ItemStack, stored in the vanilla {@code minecraft:custom_data} component under the key
 * {@code beamlights:beam} ({@link BeamLightsApi#getBeam}, {@link BeamLightsApi#setBeam}): gives the stack a beam
 * without a datapack. Same fields as a datapack definition minus items, condition and priority. In the slots it
 * lists, it wins over datapack definitions (luminance 0 turns the beam off). Saved with the stack and synced like any
 * custom data; the mod is not needed on the other side. Immutable.
 *
 * <p>NBT / JSON form: {@code {luminance: 12, range: 20.0, cone: 25.0, color: "#FFE8B0",
 * origin: {forward: 0.3, down: 0.2}, slots: ["mainhand", "offhand"]}}; only luminance is required.
 *
 * @param luminance 0..15
 * @param range     1..128 blocks (default 16)
 * @param cone      cone half-angle 1..89 degrees (default 25)
 * @param color     0xRRGGBB (default white; "#RRGGBB" string or int in NBT/JSON)
 * @param forward   origin offset along the look direction, -4..4
 * @param down      origin offset world-down, -4..4
 * @param slots     lower case slot ids: mainhand, offhand, head, curios (default mainhand + offhand)
 */
public record BeamItemData(int luminance, float range, float cone, int color, double forward, double down,
                           List<String> slots) {
    public static final List<String> DEFAULT_SLOTS = List.of("mainhand", "offhand");

    public BeamItemData {
        slots = slots.stream().map(s -> s.toLowerCase(Locale.ROOT)).distinct().toList();
    }

    /** Beam with default range, cone, colour, origin and slots. */
    public static BeamItemData of(int luminance) {
        return new BeamItemData(luminance, BeamDefinition.DEFAULT_RANGE, BeamDefinition.DEFAULT_CONE,
                BeamDefinition.WHITE, 0, 0, DEFAULT_SLOTS);
    }

    /** True when the stack emits from this slot id (mainhand, offhand, head, curios). */
    public boolean allows(String slot) {
        return slots.contains(slot);
    }

    private static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(BeamDefinition.parseColor(s));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }, c -> String.format(Locale.ROOT, "#%06X", c & 0xFFFFFF));
    private static final Codec<Integer> COLOR = Codec.withAlternative(HEX_COLOR, Codec.INT);

    private record Origin(double forward, double down) {
        static final Origin NONE = new Origin(0, 0);
        static final Codec<Origin> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("forward", 0.0).forGetter(Origin::forward),
                Codec.DOUBLE.optionalFieldOf("down", 0.0).forGetter(Origin::down)
        ).apply(i, Origin::new));
    }

    /** Codec (NBT, JSON, KubeJS objects), validated. */
    public static final Codec<BeamItemData> CODEC = RecordCodecBuilder.<BeamItemData>create(i -> i.group(
            Codec.INT.fieldOf("luminance").forGetter(BeamItemData::luminance),
            Codec.FLOAT.optionalFieldOf("range", BeamDefinition.DEFAULT_RANGE).forGetter(BeamItemData::range),
            Codec.FLOAT.optionalFieldOf("cone", BeamDefinition.DEFAULT_CONE).forGetter(BeamItemData::cone),
            COLOR.optionalFieldOf("color", BeamDefinition.WHITE).forGetter(BeamItemData::color),
            Origin.CODEC.optionalFieldOf("origin", Origin.NONE).forGetter(d -> new Origin(d.forward(), d.down())),
            Codec.STRING.listOf().optionalFieldOf("slots", DEFAULT_SLOTS).forGetter(BeamItemData::slots)
    ).apply(i, (lum, range, cone, color, origin, slots) ->
            new BeamItemData(lum, range, cone, color, origin.forward(), origin.down(), slots)))
            .validate(BeamItemData::validate);

    private static DataResult<BeamItemData> validate(BeamItemData d) {
        String err = BeamValues.check(d.luminance, d.range, d.cone, d.forward, d.down, d.slots);
        return err == null ? DataResult.success(d) : DataResult.error(() -> "beamlights:beam " + err);
    }
}
