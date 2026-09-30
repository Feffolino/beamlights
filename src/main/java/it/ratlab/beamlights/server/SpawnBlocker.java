package it.ratlab.beamlights.server;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.config.BeamServerConfig;
import it.ratlab.beamlights.core.BeamCone;
import it.ratlab.beamlights.core.BeamRegistry;
import it.ratlab.beamlights.core.math.V3;
import it.ratlab.beamlights.world.LevelOcclusion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Server side: cancels natural monster spawns inside the cone of a lit beam. */
@EventBusSubscriber(modid = BeamLights.MOD_ID)
public final class SpawnBlocker {
    private static final AtomicLong BLOCKED = new AtomicLong();

    // Lit beams per emitter entity id, valid for one game tick of one level.
    private static final Map<Integer, List<Beam>> CACHE = new HashMap<>();
    private static ServerLevel cacheLevel;
    private static long cacheTick = Long.MIN_VALUE;
    private static long lastLogMs;

    private SpawnBlocker() {
    }

    public static long blockedCount() {
        return BLOCKED.get();
    }

    @SubscribeEvent
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        Mob mob = event.getEntity();
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        ServerLevel level = event.getLevel().getLevel();
        if (level.isClientSide() || !BeamGameRules.blockSpawns(level)) return;

        V3 point = new V3(event.getX(), event.getY() + mob.getBbHeight() * 0.5, event.getZ());
        Entity emitter = findLitEmitter(level, point);
        if (emitter == null) return;

        event.setSpawnCancelled(true);
        long total = BLOCKED.incrementAndGet();
        if (BeamServerConfig.DEBUG_LOG.get()) {
            long now = System.currentTimeMillis();
            if (now - lastLogMs >= 1000L) {
                lastLogMs = now;
                BeamLights.LOG.info("Beam Lights: blocked {} at {} {} {} (beam of {}), {} spawns blocked since server start",
                        mob.getType().getDescriptionId(), (int) Math.floor(event.getX()), (int) Math.floor(event.getY()),
                        (int) Math.floor(event.getZ()), emitter.getName().getString(), total);
            }
        }
    }

    private static Entity findLitEmitter(ServerLevel level, V3 point) {
        double r = BeamServerConfig.SPAWN_CHECK_RANGE.get();
        AABB box = new AABB(point.x(), point.y(), point.z(), point.x(), point.y(), point.z()).inflate(r);
        List<Entity> emitters = level.getEntities((Entity) null, box, BeamRegistry.INSTANCE::mayEmit);
        if (emitters.isEmpty()) return null;

        LevelOcclusion occlusion = null;
        for (Entity e : emitters) {
            for (Beam beam : beams(level, e)) {
                // coneDeg is the half-angle, as in ConeRays.
                if (!BeamCone.inCone(beam.origin(), beam.dir(), beam.coneDeg(), beam.range(), point)) continue;
                if (occlusion == null) occlusion = new LevelOcclusion(level);
                if (BeamCone.visible(beam.origin(), point, occlusion)) return e;
            }
        }
        return null;
    }

    private static List<Beam> beams(ServerLevel level, Entity e) {
        long tick = level.getGameTime();
        if (level != cacheLevel || tick != cacheTick) {
            CACHE.clear();
            cacheLevel = level;
            cacheTick = tick;
        }
        return CACHE.computeIfAbsent(e.getId(), id -> {
            List<Beam> lit = new ArrayList<>();
            BeamRegistry.INSTANCE.collect(e, 1.0f, b -> {
                if (b.luminance() > 0) lit.add(b);
            });
            return lit;
        });
    }
}
