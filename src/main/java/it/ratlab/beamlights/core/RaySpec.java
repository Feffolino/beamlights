package it.ratlab.beamlights.core;

/**
 * One side ray of a beam. spread = tilt as a fraction of the beam half-angle (0 = axis); rollDeg = angle around the
 * axis from "right", counter-clockwise (90 = up, 270 = down); rangeFactor = fraction of the beam range.
 */
public record RaySpec(double spread, double rollDeg, int luminanceOffset, boolean midpoints, double rangeFactor) {
}
