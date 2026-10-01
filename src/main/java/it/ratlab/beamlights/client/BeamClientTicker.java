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
import it.ratlab.beamlights.core.LightSmoother;
import it.ratlab.beamlights.core.RayLayout;
import it.ratlab.beamlights.core.RaySpec;
import it.ratlab.beamlights.core.math.V3;
import it.ratlab.beamlights.data.BeamDefinitions;
import it.ratlab.beamlights.world.LevelOcclusion;
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

/** Per tick: collect beams near the player, trace, plan light points, smooth them, feed the backend. */
@EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
public final class BeamClientTicker {
    public static final BeamStats STATS = new BeamStats();
    private static final double HIT_BACKOFF = 0.5;
    private static final int RAYS_PER_BEAM = 32;
    private static final int MAX_BEAMS_PER_ENTITY = 256 / RAYS_PER_BEAM;

    private static LayoutKey layoutKey;
    private static List<RaySpec> cachedLayout = List.of();

    private static final LightSmoother SMOOTHER = new LightSmoother();

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
            SMOOTHER.clear();
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
        BeamDefinitions.CLIENT.clear();
    }

    private static void safeClear() {
        SMOOTHER.clear();
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

    /** Config values the ray layout depends on; the layout is rebuilt only when these change. */
    private record LayoutKey(RayLayout.Pattern pattern, int sideRays, double spread, double rollOffset, int innerRays,
                             double innerSpread, List<String> custom, int lumOffset, boolean midpoints,
                             double rangeFactor) {
    }

    private static List<RaySpec> layout() {
        LayoutKey k = new LayoutKey(BeamClientConfig.RAY_PATTERN.get(), BeamClientConfig.SIDE_RAYS.get(),
                BeamClientConfig.CONE_SPREAD.get(), BeamClientConfig.RAY_ROLL_OFFSET.get(),
                BeamClientConfig.INNER_RAYS.get(), BeamClientConfig.INNER_SPREAD.get(),
                List.copyOf(BeamClientConfig.CUSTOM_RAYS.get()), BeamClientConfig.SIDE_LUMINANCE_OFFSET.get(),
                BeamClientConfig.SIDE_MIDPOINTS.get(), BeamClientConfig.SIDE_RANGE_FACTOR.get());
        if (!k.equals(layoutKey)) {
            layoutKey = k;
            cachedLayout = RayLayout.build(k.pattern(), k.sideRays(), k.spread(), k.rollOffset(), k.innerRays(),
                    k.innerSpread(), k.custom(), k.lumOffset(), k.midpoints(), k.rangeFactor());
            if (k.pattern() == RayLayout.Pattern.CUSTOM) {
                int bad = RayLayout.invalidCount(k.custom());
                if (bad > 0) BeamLights.LOG.warn("Beam Lights: {} invalid customRays entries skipped", bad);
            }
            STATS.layout = k.pattern() + ", " + cachedLayout.size() + " side rays";
        }
        return cachedLayout;
    }

    /** Short layout description for the overlay and /beamlights status. */
    public static String layoutLine() {
        layout();
        return STATS.layout;
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
        LightPointPlanner.Settings sideMid = new LightPointPlanner.Settings(
                true, settings.midSpacing(), settings.midLuminanceOffset(),
                settings.maxPerBeam(), settings.mergeDistance(), HIT_BACKOFF);
        LightPointPlanner.Settings sideNoMid = new LightPointPlanner.Settings(
                false, settings.midSpacing(), settings.midLuminanceOffset(),
                settings.maxPerBeam(), settings.mergeDistance(), HIT_BACKOFF);
        List<RaySpec> layout = layout();
        int perEntity = BeamClientConfig.MAX_SOURCES_PER_ENTITY.get();
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

        List<LightSmoother.Target> targets = new ArrayList<>();
        List<Beam> beams = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        List<V3> shared = new ArrayList<>();
        for (Entity e : emitters) {
            beams.clear();
            sources.clear();
            shared.clear();
            BeamRegistry.INSTANCE.collectNamed(e, 1.0f, (src, beam) -> {
                sources.add(src);
                beams.add(beam);
            });
            // Keys ray index = beam * 32 + sub (sub 0 = central), 8 bits: at most 8 beams per entity.
            int beamCount = Math.min(beams.size(), MAX_BEAMS_PER_ENTITY);
            int entityUsed = 0;
            // Pass 0: central rays of every beam, so they win the per-entity budget; pass 1: side rays.
            for (int pass = 0; pass < 2; pass++) {
                for (int bi = 0; bi < beamCount; bi++) {
                    Beam beam = beams.get(bi);
                    List<V3> dirs = pass == 0 ? List.of(beam.dir().normalize())
                            : ConeRays.directions(beam.dir(), beam.coneDeg(), layout);
                    for (int i = 0; i < dirs.size(); i++) {
                        boolean side = pass == 1;
                        RaySpec spec = side ? layout.get(i) : null;
                        int lum = side ? Math.max(0, Math.min(15, beam.luminance() + spec.luminanceOffset()))
                                : beam.luminance();
                        if (lum <= 0) continue;
                        int ray = bi * RAYS_PER_BEAM + (side ? i + 1 : 0);
                        Beam rayBeam = side
                                ? new Beam(beam.origin(), dirs.get(i), (float) (beam.range() * spec.rangeFactor()),
                                beam.coneDeg(), lum, beam.rgb())
                                : beam;
                        BeamTracer.Result trace = BeamTracer.trace(rayBeam.origin(), rayBeam.dir(), rayBeam.range(),
                                occlusion);
                        LightPointPlanner.Settings s = !side ? settings : spec.midpoints() ? sideMid : sideNoMid;
                        LightPointPlanner.Plan plan = LightPointPlanner.plan(rayBeam.origin(), rayBeam.dir(), trace,
                                lum, s, occlusion, shared, side);
                        STATS.addPlan(plan);
                        for (PlannedPoint p : plan.points()) {
                            if (p.status() != Status.ACCEPTED) continue;
                            if (entityUsed >= perEntity) {
                                STATS.capped++;
                                continue;
                            }
                            targets.add(new LightSmoother.Target(Keys.of(e.getId(), ray, p.slot()), p.pos(),
                                    p.luminance()));
                            entityUsed++;
                        }
                        if (frame != null) {
                            frame.add(new TracedBeam(e.getName().getString() + "#" + e.getId(), sources.get(bi), e.getId(), ray, side,
                                    rayBeam, trace, plan));
                        }
                    }
                }
            }
        }

        // Global cap on the smoothed output; ghosts come last, so they are the first to go.
        List<LightSmoother.Output> out = SMOOTHER.update(targets, new LightSmoother.Settings(
                BeamClientConfig.SMOOTHING.get(), BeamClientConfig.SMOOTH_FACTOR.get(),
                BeamClientConfig.JUMP_DISTANCE.get(), BeamClientConfig.FADE_TICKS.get()));
        b.begin();
        int used = 0;
        for (LightSmoother.Output o : out) {
            if (used >= maxSources) {
                if (!o.ghost()) STATS.globalCapped++;
                continue;
            }
            b.put(o.key(), o.pos(), o.luminance());
            used++;
            if (o.ghost()) STATS.ghosts++;
        }
        b.end();

        STATS.endTick(System.nanoTime() - t0, b.movesThisTick());
        DebugState.setLastFrame(frame != null ? frame : List.of());
        if (DebugState.consumeDump()) DebugDump.write(frame, b, player);
    }
}
