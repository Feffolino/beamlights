package it.ratlab.beamlights.api;

import it.ratlab.beamlights.core.math.V3;

/**
 * One light beam for one tick.
 *
 * @param origin    world position the beam starts from
 * @param dir       direction (any length, normalized by consumers)
 * @param range     max length in blocks
 * @param coneDeg   outer cone angle in degrees (used by multi-ray in phase 2 and spawn blocking in phase 3)
 * @param luminance light level at the hit point, 0..15 (0 = no light)
 * @param rgb       beam colour 0xRRGGBB (ignored by backends without colour)
 */
public record Beam(V3 origin, V3 dir, float range, float coneDeg, int luminance, int rgb) {
}
