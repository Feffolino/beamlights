package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.core.BeamTracer;
import it.ratlab.beamlights.core.LightPointPlanner;

/** One beam of the last tick, kept for overlay, render and dump. */
public record TracedBeam(String owner, int entityId, int ray, Beam beam, BeamTracer.Result trace,
                         LightPointPlanner.Plan plan) {
}
