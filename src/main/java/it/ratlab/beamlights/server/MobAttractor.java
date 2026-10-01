package it.ratlab.beamlights.server;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.config.BeamServerConfig;
import it.ratlab.beamlights.core.AttractTarget;
import it.ratlab.beamlights.core.BeamRegistry;
import it.ratlab.beamlights.core.BeamTracer;
import it.ratlab.beamlights.api.math.V3;
import it.ratlab.beamlights.world.LevelOcclusion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server side: idle hostile mobs without a target drift toward the point a beam lights up, in short steps and only
 * by chance, so vanilla targeting and other mods' attractors (sounds, scents) always win. Central ray only.
 */
@EventBusSubscriber(modid = BeamLights.MOD_ID)
public final class MobAttractor {
    private static final double EMITTER_SEARCH = 64.0;
    private static final double ARRIVED_RADIUS = 1.5;
    private static final AtomicLong PATHS = new AtomicLong();

    private static long lastLogMs;

    // Last step target per mob; weak keys so removed mobs drop out on their own. Server thread only.
    private static final Map<Mob, V3> LAST_TARGET = new WeakHashMap<>();

    private MobAttractor() {
    }

    public static long pathsIssued() {
        return PATHS.get();
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!BeamGameRules.attractMobs(level)) return;
        if (level.getGameTime() % BeamServerConfig.ATTRACT_INTERVAL.get() != 0) return;
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        // Players plus any other emitter near a player, deduplicated by entity id.
        Map<Integer, Entity> emitters = new LinkedHashMap<>();
        for (ServerPlayer p : players) {
            if (!p.isSpectator()) emitters.putIfAbsent(p.getId(), p);
            for (Entity e : level.getEntities((Entity) null, p.getBoundingBox().inflate(EMITTER_SEARCH),
                    BeamRegistry.INSTANCE::mayEmit)) {
                emitters.putIfAbsent(e.getId(), e);
            }
        }

        List<Beam> beams = new ArrayList<>();
        List<Beam> collected = new ArrayList<>(2);
        for (Entity e : emitters.values()) {
            BeamRegistry.INSTANCE.collect(e, 1.0f, collected, null);
            for (Beam b : collected) {
                if (b.luminance() > 0) beams.add(b);
            }
        }
        if (beams.isEmpty()) return;

        LevelOcclusion occlusion = new LevelOcclusion(level);
        double radius = BeamServerConfig.ATTRACT_RADIUS.get();
        int maxMobs = BeamServerConfig.ATTRACT_MAX_MOBS.get();
        double speed = BeamServerConfig.ATTRACT_SPEED.get();
        double repath = BeamServerConfig.ATTRACT_REPATH_DISTANCE.get();
        double chance = BeamServerConfig.ATTRACT_CHANCE.get();
        double step = BeamServerConfig.ATTRACT_STEP_DISTANCE.get();
        for (Beam beam : beams) {
            BeamTracer.Result r = BeamTracer.trace(beam.origin(), beam.dir(), beam.range(), occlusion);
            if (!r.hit()) continue;
            attract(level, AttractTarget.litPoint(r.point(), beam.dir()), radius, maxMobs, speed, repath, chance, step);
        }
        if (BeamServerConfig.DEBUG_LOG.get()) {
            long now = System.currentTimeMillis();
            if (now - lastLogMs >= 1000L) {
                lastLogMs = now;
                BeamLights.LOG.info("Beam Lights: {} lit beams in {}, {} attraction steps issued since server start",
                        beams.size(), level.dimension().location(), PATHS.get());
            }
        }
    }

    private static void attract(ServerLevel level, V3 point, double radius, int maxMobs, double speed, double repath,
                                double chance, double step) {
        AABB box = new AABB(point.x(), point.y(), point.z(), point.x(), point.y(), point.z()).inflate(radius);
        double radiusSq = radius * radius;
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box,
                m -> m instanceof Enemy && m.isAlive() && m.getTarget() == null && !m.isNoAi()
                        && m.getNavigation().isDone() // never override an active path (vanilla or another mod)
                        && m.distanceToSqr(point.x(), point.y(), point.z()) <= radiusSq);
        if (mobs.isEmpty()) return;
        mobs.sort(Comparator.comparingDouble(m -> m.distanceToSqr(point.x(), point.y(), point.z())));

        int n = Math.min(maxMobs, mobs.size());
        for (int i = 0; i < n; i++) {
            Mob m = mobs.get(i);
            V3 pos = new V3(m.getX(), m.getY(), m.getZ());
            if (AttractTarget.arrived(pos, point, ARRIVED_RADIUS)) continue;
            if (!AttractTarget.roll(chance, m.getRandom().nextDouble())) continue;
            // Same step target as last time means the mob could not get there: leave it alone until the light moves.
            V3 target = AttractTarget.step(pos, point, step);
            if (!AttractTarget.shouldRepath(LAST_TARGET.get(m), target, repath)) continue;
            if (m.getNavigation().moveTo(target.x(), target.y(), target.z(), speed)) {
                LAST_TARGET.put(m, target);
                PATHS.incrementAndGet();
            }
        }
    }
}
