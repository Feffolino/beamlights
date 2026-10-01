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
import it.ratlab.beamlights.core.ConeLight;
import it.ratlab.beamlights.core.ConePolicy;
import it.ratlab.beamlights.core.ConeRays;
import it.ratlab.beamlights.core.ConeSmoother;
import it.ratlab.beamlights.core.Keys;
import it.ratlab.beamlights.core.LightPointPlanner;
import it.ratlab.beamlights.core.LightPointPlanner.PlannedPoint;
import it.ratlab.beamlights.core.LightPointPlanner.Status;
import it.ratlab.beamlights.core.LightSmoother;
import it.ratlab.beamlights.core.MotionGovernor;
import it.ratlab.beamlights.core.RayLayout;
import it.ratlab.beamlights.core.RaySpec;
import it.ratlab.beamlights.core.SourceMotion;
import it.ratlab.beamlights.api.math.V3;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Per tick: collect beams near the player, trace, plan light points, smooth them, feed the backend. */
@EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
public final class BeamClientTicker {
    public static final BeamStats STATS = new BeamStats();
    private static final double HIT_BACKOFF = 0.5;
    private static final int RAYS_PER_BEAM = Keys.RAYS_PER_BEAM;
    private static final int MAX_BEAMS_PER_ENTITY = 256 / RAYS_PER_BEAM;

    private static LayoutKey layoutKey;
    private static List<RaySpec> cachedLayout = List.of();

    private static final LightSmoother SMOOTHER = new LightSmoother();
    private static final ConeSmoother CONE_SMOOTHER = new ConeSmoother();

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
            STATS.reset();
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
            CONE_SMOOTHER.clear();
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
        CONE_SMOOTHER.clear();
        EMITTER_CACHE.clear();
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

    /** Last traced light points of one emitter, reused between re-traces of remote emitters. */
    private static final class EmitterCache {
        final List<LightSmoother.Target> targets = new ArrayList<>();
        final List<TracedBeam> frame = new ArrayList<>();
        final List<ConeTarget> cones = new ArrayList<>();
        /** Cones shown per key (ConePolicy hysteresis and FAST freeze). */
        final Map<Long, ConeLight.Shape> heldCones = new HashMap<>();
        final MotionGovernor motion = new MotionGovernor();
        long lastSeen;
        long lastTraced;
        boolean traced;
        boolean lod;
    }

    /** Cone light of one central beam (LambDynamicLights ldlConeLight); shape null = no cone this trace. */
    private record ConeTarget(long key, ConeLight.Shape shape) {
    }

    // Reused every tick (client thread only).
    private static final List<ConeTarget> CONES = new ArrayList<>();
    private static final List<ConeSmoother.Target> CONE_IN = new ArrayList<>();
    private static final Set<Long> CONE_KEYS = new HashSet<>();
    private static final Map<Integer, EmitterCache> EMITTER_CACHE = new HashMap<>();
    private static final List<Entity> EMITTERS = new ArrayList<>();
    private static final List<LightSmoother.Target> TARGETS = new ArrayList<>();
    private static final List<Beam> BEAMS = new ArrayList<>();
    private static final List<String> SOURCES = new ArrayList<>();
    private static final List<V3> SHARED = new ArrayList<>();
    private static final List<LightSmoother.Target> FRESH = new ArrayList<>();
    /** Entity ids whose emitter just settled (MotionGovernor resync): their lights skip the hysteresis. */
    private static final Set<Integer> RESYNC = new HashSet<>();
    private static long tickCounter;

    /** Applies the cone hysteresis / FAST freeze to the emitter's fresh cones; drops cones without a shape. */
    private static void gateCones(EmitterCache cache, boolean fast, ConePolicy.Settings s) {
        List<ConeTarget> list = cache.cones;
        CONE_KEYS.clear();
        int w = 0;
        for (int i = 0; i < list.size(); i++) {
            ConeTarget c = list.get(i);
            ConeLight.Shape shown = ConePolicy.gate(cache.heldCones.get(c.key()), c.shape(), fast, s);
            if (shown == null) continue;
            CONE_KEYS.add(c.key());
            list.set(w++, shown == c.shape() ? c : new ConeTarget(c.key(), shown));
        }
        while (list.size() > w) list.remove(list.size() - 1);
        cache.heldCones.keySet().retainAll(CONE_KEYS);
        for (ConeTarget c : list) cache.heldCones.put(c.key(), c.shape());
    }

    private static void tick(ClientLevel level, LocalPlayer player, LightBackend b) {
        long t0 = System.nanoTime();
        long tick = ++tickCounter;
        STATS.beginTick();
        boolean capture = DebugState.overlay() || DebugState.render() || DebugState.dumpRequested();
        List<TracedBeam> frame = capture ? new ArrayList<>() : null;

        double range = BeamClientConfig.OTHER_PLAYERS_RANGE.get();
        double rangeSq = range * range;
        boolean others = BeamClientConfig.OTHER_PLAYERS.get();
        int maxSources = BeamClientConfig.MAX_SOURCES.get();
        boolean snap = BeamClientConfig.SNAP_TO_BLOCK.get();
        int interval = BeamClientConfig.REMOTE_UPDATE_INTERVAL.get();
        double lod = BeamClientConfig.LOD_DISTANCE.get();
        double lodSq = lod > 0 ? lod * lod : Double.POSITIVE_INFINITY;
        boolean sectionMerge = BeamClientConfig.MERGE_SAME_SECTION.get();
        LightPointPlanner.Settings settings = new LightPointPlanner.Settings(
                BeamClientConfig.MIDPOINTS.get(), BeamClientConfig.MID_SPACING.get(),
                BeamClientConfig.MID_LUMINANCE_OFFSET.get(), BeamClientConfig.MAX_SOURCES_PER_BEAM.get(),
                BeamClientConfig.MERGE_DISTANCE.get(), HIT_BACKOFF, sectionMerge);
        LightPointPlanner.Settings settingsNoMid = settings.withMidpoints(false);
        LightPointPlanner.Settings sideMid = settings.withMidpoints(true);
        List<RaySpec> layout = layout();
        int perEntity = BeamClientConfig.MAX_SOURCES_PER_ENTITY.get();
        LevelOcclusion occlusion = new LevelOcclusion(level);
        MotionGovernor.Settings motion = BeamClientConfig.motionSettings();
        RESYNC.clear();
        CONES.clear();
        boolean cones = b.wantsBeams();
        int coneOffset = BeamClientConfig.LDL_CONE_LUMINANCE_OFFSET.get();
        ConePolicy.Settings coneSettings = BeamClientConfig.coneSettings();

        List<Entity> emitters = EMITTERS;
        emitters.clear();
        for (Entity e : level.entitiesForRendering()) {
            if (!BeamRegistry.INSTANCE.mayEmit(e)) continue;
            if (e != player) {
                if (!others && e instanceof Player) continue;
                if (e.distanceToSqr(player) > rangeSq) continue;
            }
            emitters.add(e);
        }
        emitters.sort(Comparator.comparingDouble(e -> e == player ? -1.0 : e.distanceToSqr(player)));

        List<LightSmoother.Target> targets = TARGETS;
        targets.clear();
        List<Beam> beams = BEAMS;
        List<String> sources = capture ? SOURCES : null;
        List<V3> shared = SHARED;
        for (Entity e : emitters) {
            boolean local = e == player;
            boolean reduced = !local && e.distanceToSqr(player) > lodSq;
            EmitterCache cache = EMITTER_CACHE.computeIfAbsent(e.getId(), id -> new EmitterCache());
            cache.lastSeen = tick;
            if (reduced) STATS.lodEmitters++;
            if (cache.motion.resync()) RESYNC.add(e.getId());
            // Remote emitters: reuse the last points between re-traces (an LOD change forces a re-trace).
            if (!local && cache.traced && cache.lod == reduced && !SourceMotion.remoteDue(e.getId(), tick, interval)) {
                targets.addAll(cache.targets);
                CONES.addAll(cache.cones);
                if (frame != null) frame.addAll(cache.frame);
                STATS.reusedEmitters++;
                continue;
            }
            cache.traced = true;
            cache.lod = reduced;
            cache.targets.clear();
            cache.frame.clear();
            cache.cones.clear();
            shared.clear();
            List<LightSmoother.Target> fresh = FRESH;
            fresh.clear();
            BeamRegistry.INSTANCE.collect(e, 1.0f, beams, sources);
            // Keys ray index = beam * 32 + sub (sub 0 = central), 8 bits: at most 8 beams per entity.
            int beamCount = Math.min(beams.size(), MAX_BEAMS_PER_ENTITY);
            int entityUsed = 0;
            // Pass 0: central rays of every beam, so they win the per-entity budget; pass 1: side rays (not in LOD).
            int passes = reduced ? 1 : 2;
            for (int pass = 0; pass < passes; pass++) {
                boolean side = pass == 1;
                if (side && layout.isEmpty()) break;
                for (int bi = 0; bi < beamCount; bi++) {
                    Beam beam = beams.get(bi);
                    List<V3> dirs = side ? ConeRays.directions(beam.dir(), beam.coneDeg(), layout) : null;
                    int rays = side ? dirs.size() : 1;
                    for (int i = 0; i < rays; i++) {
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
                        if (cones && !side) {
                            int coneLum = Math.max(0, Math.min(15, lum + coneOffset));
                            cache.cones.add(new ConeTarget(Keys.of(e.getId(), ray, ConeLight.SLOT),
                                    ConePolicy.target(rayBeam.origin(), rayBeam.dir(), trace.hit(), trace.distance(),
                                            HIT_BACKOFF, beam.coneDeg(), coneLum, coneSettings)));
                        }
                        LightPointPlanner.Settings s = !side ? (reduced ? settingsNoMid : settings)
                                : spec.midpoints() ? sideMid : settingsNoMid;
                        LightPointPlanner.Plan plan = LightPointPlanner.plan(rayBeam.origin(), rayBeam.dir(), trace,
                                lum, s, occlusion, shared, side);
                        STATS.addPlan(plan);
                        for (PlannedPoint p : plan.points()) {
                            if (p.status() != Status.ACCEPTED) continue;
                            if (entityUsed >= perEntity) {
                                STATS.capped++;
                                continue;
                            }
                            fresh.add(new LightSmoother.Target(Keys.of(e.getId(), ray, p.slot()), p.pos(),
                                    p.luminance()));
                            entityUsed++;
                        }
                        if (frame != null) {
                            String src = bi < sources.size() ? sources.get(bi) : "?";
                            cache.frame.add(new TracedBeam(e.getName().getString() + "#" + e.getId(), src, e.getId(),
                                    ray, side, rayBeam, trace, plan));
                        }
                    }
                }
            }
            // Motion: measured on every trace; side rays and midpoints may be held while the beam turns fast.
            if (beamCount > 0) {
                int elapsed = cache.lastTraced == 0 ? 1 : (int) Math.min(100, tick - cache.lastTraced);
                cache.motion.observe(beams.get(0).origin(), beams.get(0).dir(), elapsed, motion);
            }
            cache.lastTraced = tick;
            cache.motion.filter(fresh, cache.targets, motion);
            gateCones(cache, cache.motion.state() == MotionGovernor.State.FAST, coneSettings);
            STATS.suppressed += cache.motion.suppressed();
            if (cache.motion.resync()) RESYNC.add(e.getId());
            if (local) STATS.setMotion(cache.motion.state(), cache.motion.turnDegPerSec(),
                    cache.motion.moveBlocksPerSec(), cache.motion.resync());
            targets.addAll(cache.targets);
            CONES.addAll(cache.cones);
            if (frame != null) frame.addAll(cache.frame);
        }
        EMITTER_CACHE.values().removeIf(c -> c.lastSeen != tick);

        // Global cap on the smoothed output; ghosts come last, so they are the first to go.
        List<LightSmoother.Output> out = SMOOTHER.update(targets, new LightSmoother.Settings(
                BeamClientConfig.SMOOTHING.get(), BeamClientConfig.SMOOTH_FACTOR.get(),
                BeamClientConfig.JUMP_DISTANCE.get(), BeamClientConfig.FADE_TICKS.get(),
                BeamClientConfig.FADE_STEPS.get(), BeamClientConfig.GLIDE_MIN_TICKS.get(), snap));
        V3 viewer = new V3(player.getX(), player.getEyeY(), player.getZ());
        b.begin(viewer, BeamClientConfig.MAX_MOVES_PER_TICK.get());
        int used = 0;
        int localId = player.getId();
        for (LightSmoother.Output o : out) {
            if (used >= maxSources) {
                if (!o.ghost()) STATS.globalCapped++;
                continue;
            }
            // Snapped lights only change when they enter another block; glide and fades still drive them.
            V3 pos = snap ? SourceMotion.snapToBlock(o.pos()) : o.pos();
            boolean priority = !o.ghost() && Keys.entityId(o.key()) == localId
                    && Keys.ray(o.key()) % RAYS_PER_BEAM == 0;
            b.put(o.key(), pos, o.luminance(), priority, !o.ghost() && RESYNC.contains(Keys.entityId(o.key())));
            used++;
            if (o.ghost()) STATS.ghosts++;
        }
        CONE_IN.clear();
        for (ConeTarget c : CONES) CONE_IN.add(new ConeSmoother.Target(c.key(), c.shape()));
        for (ConeSmoother.Target c : CONE_SMOOTHER.update(CONE_IN, new ConeSmoother.Settings(
                BeamClientConfig.SMOOTHING.get(), BeamClientConfig.SMOOTH_FACTOR.get(),
                BeamClientConfig.JUMP_DISTANCE.get(), BeamClientConfig.CONE_STEPS.get(),
                BeamClientConfig.GLIDE_MIN_TICKS.get(), BeamClientConfig.FADE_TICKS.get(),
                BeamClientConfig.FADE_STEPS.get()))) {
            int id = Keys.entityId(c.key());
            b.putBeam(c.key(), c.shape(), id == localId, RESYNC.contains(id));
        }
        b.end();

        STATS.snapped = snap;
        STATS.endTick(System.nanoTime() - t0, b.movesThisTick(), b.deferredThisTick());
        DebugState.setLastFrame(frame != null ? frame : List.of());
        if (DebugState.consumeDump()) DebugDump.write(frame, b, player);
    }
}
