package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.core.BeamTracer;
import it.ratlab.beamlights.core.LightPointPlanner;

/**
 * One ray of the last tick, kept for overlay, render and dump; beam holds the ray direction and luminance, source the
 * name of the provider that emitted it.
 */
public record TracedBeam(String owner, String source, int entityId, int ray, boolean side, Beam beam, BeamTracer.Result trace,
                         LightPointPlanner.Plan plan) {
}
