package it.ratlab.beamlights.client;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.client.debug.BeamStats;
import it.ratlab.beamlights.client.debug.DebugDump;
import it.ratlab.beamlights.client.debug.DebugState;
import it.ratlab.beamlights.client.debug.TracedBeam;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.BeamRegistry;
import it.ratlab.beamlights.core.BeamTracer;
import it.ratlab.beamlights.core.ConeRays;
import it.ratlab.beamlights.core.Keys;
import it.ratlab.beamlights.core.LightPointPlanner;
import it.ratlab.beamlights.core.LightPointPlanner.PlannedPoint;
import it.ratlab.beamlights.core.LightPointPlanner.Status;
import it.ratlab.beamlights.core.math.V3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Per tick: collect beams near the player, trace, plan light points, feed the backend. */
@EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
public final class BeamClientTicker {
    public static final BeamStats STATS = new BeamStats();
    private static final double HIT_BACKOFF = 0.5;

    private static LightBackend backend;
    private static ClientLevel lastLevel;
    private static long lastErrorLogMs;

    private BeamClientTicker() {
    }

    public static LightBackend backend() {
        if (backend == null) backend = BackendFactory.create();
        return backend;
    }

    /** Clears every light and re-creates the backend (used by /beamlights reload). */
    public static void resetBackend() {
        safeClear();
        backend = BackendFactory.create();
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || !BeamClientConfig.ENABLED.get()) {
            if (lastLevel != null || !BeamClientConfig.ENABLED.get()) safeClear();
            lastLevel = null;
            DebugState.setLastFrame(List.of());
            return;
        }
        if (level != lastLevel) {
            safeClear();
            lastLevel = level;
        }
        try {
            tick(level, player, backend());
        } catch (Throwable t) {
            LightBackend old = backend();
            try {
                old.clear();
            } catch (Throwable ignored) {
            }
            if (permanent(t)) {
                BeamLights.LOG.error("Beam Lights: backend '{}' failed, dynamic beam light disabled", old.name(), t);
                backend = new NoopBackend("failed: " + t);
            } else {
                logRateLimited(old.name(), t);
            }
            DebugState.setLastFrame(List.of());
        }
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        safeClear();
        lastLevel = null;
    }

    private static void safeClear() {
        if (backend == null) return;
        try {
            backend.clear();
        } catch (Throwable t) {
            if (permanent(t)) {
                BeamLights.LOG.warn("Beam Lights: backend clear failed", t);
                backend = new NoopBackend("failed on clear: " + t);
            } else {
                logRateLimited(backend.name(), t);
            }
        }
    }

    /** API mismatch: retrying every tick is pointless. */
    private static boolean permanent(Throwable t) {
        return t instanceof LinkageError || t instanceof ClassCastException;
    }

    private static void logRateLimited(String backendName, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogMs < 10_000L) return;
        lastErrorLogMs = now;
        BeamLights.LOG.error("Beam Lights: backend '{}' tick failed, will retry", backendName, t);
    }

    private static void tick(ClientLevel level, LocalPlayer player, LightBackend b) {
        long t0 = System.nanoTime();
        STATS.beginTick();
        boolean capture = DebugState.overlay() || DebugState.render() || DebugState.dumpRequested();
        List<TracedBeam> frame = capture ? new ArrayList<>() : null;

        double range = BeamClientConfig.OTHER_PLAYERS_RANGE.get();
        double rangeSq = range * range;
        boolean others = BeamClientConfig.OTHER_PLAYERS.get();
        int maxSources = BeamClientConfig.MAX_SOURCES.get();
        LightPointPlanner.Settings settings = new LightPointPlanner.Settings(
                BeamClientConfig.MIDPOINTS.get(), BeamClientConfig.MID_SPACING.get(),
                BeamClientConfig.MID_LUMINANCE_OFFSET.get(), BeamClientConfig.MAX_SOURCES_PER_BEAM.get(),
                BeamClientConfig.MERGE_DISTANCE.get(), HIT_BACKOFF);
        LightPointPlanner.Settings sideSettings = new LightPointPlanner.Settings(
                BeamClientConfig.SIDE_MIDPOINTS.get(), settings.midSpacing(), settings.midLuminanceOffset(),
                settings.maxPerBeam(), settings.mergeDistance(), HIT_BACKOFF);
        int rays = BeamClientConfig.RAYS.get();
        double coneSpread = BeamClientConfig.CONE_SPREAD.get();
        int sideOffset = BeamClientConfig.SIDE_LUMINANCE_OFFSET.get();
        LevelOcclusion occlusion = new LevelOcclusion(level);

        List<Entity> emitters = new ArrayList<>();
        for (Entity e : level.entitiesForRendering()) {
            if (!BeamRegistry.INSTANCE.mayEmit(e)) continue;
            if (e != player) {
                if (!others && e instanceof Player) continue;
                if (e.distanceToSqr(player) > rangeSq) continue;
            }
            emitters.add(e);
        }
        emitters.sort(Comparator.comparingDouble(e -> e == player ? -1.0 : e.distanceToSqr(player)));

        b.begin();
        int used = 0;
        List<Beam> beams = new ArrayList<>();
        List<V3> shared = new ArrayList<>();
        for (Entity e : emitters) {
            beams.clear();
            shared.clear();
            BeamRegistry.INSTANCE.collect(e, 1.0f, beams::add);
            for (int bi = 0; bi < beams.size(); bi++) {
                Beam beam = beams.get(bi);
                List<V3> dirs = ConeRays.directions(beam.dir(), beam.coneDeg(), coneSpread, rays);
                for (int sub = 0; sub < dirs.size(); sub++) {
                    boolean side = sub > 0;
                    int lum = side ? Math.max(0, Math.min(15, beam.luminance() + sideOffset)) : beam.luminance();
                    if (lum <= 0) continue;
                    // Ray index for Keys: 4 sub-rays per beam, fits 8 bits for up to 64 beams per entity.
                    int ray = bi * 4 + sub;
                    Beam rayBeam = side
                            ? new Beam(beam.origin(), dirs.get(sub), beam.range(), beam.coneDeg(), lum, beam.rgb())
                            : beam;
                    BeamTracer.Result trace = BeamTracer.trace(rayBeam.origin(), rayBeam.dir(), rayBeam.range(), occlusion);
                    LightPointPlanner.Plan plan = LightPointPlanner.plan(rayBeam.origin(), rayBeam.dir(), trace, lum,
                            side ? sideSettings : settings, occlusion, shared, side);
                    STATS.addPlan(plan);
                    for (PlannedPoint p : plan.points()) {
                        if (p.status() != Status.ACCEPTED) continue;
                        if (used >= maxSources) {
                            STATS.globalCapped++;
                            continue;
                        }
                        b.put(Keys.of(e.getId(), ray, p.slot()), p.pos(), p.luminance());
                        used++;
                    }
                    if (frame != null) {
                        frame.add(new TracedBeam(e.getName().getString() + "#" + e.getId(), e.getId(), ray, side,
                                rayBeam, trace, plan));
                    }
                }
            }
        }
        b.end();

        STATS.endTick(System.nanoTime() - t0, b.movesThisTick());
        DebugState.setLastFrame(frame != null ? frame : List.of());
        if (DebugState.consumeDump()) DebugDump.write(frame, b, player);
    }
}
