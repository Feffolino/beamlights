package it.ratlab.beamlights.api;

import it.ratlab.beamlights.api.math.V3;
import net.minecraft.world.phys.Vec3;

/**
 * One light beam for one tick. Immutable, safe to share between threads; providers create a new one per tick (cheap).
 *
 * @param origin    world position the beam starts from (usually the eye, see {@code Entity#getEyePosition})
 * @param dir       direction (any non-zero length, normalized by consumers)
 * @param range     max length in blocks (1..128 is the useful range)
 * @param coneDeg   cone half-angle in degrees (side rays on the client, spawn blocking on the server)
 * @param luminance light level at the hit point, 0..15 (0 = beam off, ignored by the client and the server)
 * @param rgb       beam colour 0xRRGGBB (kept for colour-capable backends; the Sodium Dynamic Lights backend ignores it)
 */
public record Beam(V3 origin, V3 dir, float range, float coneDeg, int luminance, int rgb) {
    /** Same beam from Minecraft vectors. */
    public static Beam of(Vec3 origin, Vec3 dir, float range, float coneDeg, int luminance, int rgb) {
        return new Beam(new V3(origin.x, origin.y, origin.z), new V3(dir.x, dir.y, dir.z), range, coneDeg, luminance,
                rgb);
    }

    /** Copy with another light level (clamped to 0..15). */
    public Beam withLuminance(int lum) {
        return new Beam(origin, dir, range, coneDeg, Math.max(0, Math.min(15, lum)), rgb);
    }

    /** Copy with another range in blocks. */
    public Beam withRange(float newRange) {
        return new Beam(origin, dir, newRange, coneDeg, luminance, rgb);
    }

    /** Copy with another colour 0xRRGGBB. */
    public Beam withRgb(int newRgb) {
        return new Beam(origin, dir, range, coneDeg, luminance, newRgb);
    }
}
