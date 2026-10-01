package it.ratlab.beamlights.client.sdl;

import it.ratlab.beamlights.client.LightBackend;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.Keys;
import it.ratlab.beamlights.core.MoveScheduler;
import it.ratlab.beamlights.core.SourceMotion;
import it.ratlab.beamlights.api.math.V3;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import toni.sodiumdynamiclights.SodiumDynamicLights;

import java.util.ArrayList;
import java.util.List;

/**
 * One pooled BeamLightSource per stable key. Changes (move past the threshold and hysteresis, luminance change
 * past the tolerance, new source, removal; see SourceMotion.shouldChange) are collected during the tick and applied in end() within the move budget (MoveScheduler); the rest keeps
 * its old state and is offered again next tick.
 */
public final class SdlBackend implements LightBackend {
    // Ranking weight of new and removed sources: before plain moves at the same distance.
    private static final double APPEAR_DISPLACEMENT = 1e6;

    private static final class Change {
        long key;
        V3 pos;
        int lum;
        BeamLightSource src;
        boolean removal;
    }

    private final SodiumDynamicLights sdl;
    private final Long2ObjectOpenHashMap<BeamLightSource> sources = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet seen = new LongOpenHashSet();
    private final LongOpenHashSet moved = new LongOpenHashSet();
    private final MoveScheduler<Change> scheduler = new MoveScheduler<>();
    private final List<Change> changePool = new ArrayList<>();
    private int changeCount;
    private V3 viewer = V3.ZERO;
    private int budget;
    private SourceMotion.Gate gate = SourceMotion.Gate.legacy(0.25);
    private SourceMotion.Gate exactGate = gate;
    private int deferred;

    private SdlBackend(SodiumDynamicLights sdl) {
        this.sdl = sdl;
    }

    public static SdlBackend create() {
        SodiumDynamicLights sdl = SodiumDynamicLights.get();
        if (sdl == null) throw new IllegalStateException("SodiumDynamicLights.get() returned null");
        return new SdlBackend(sdl);
    }

    @Override
    public String name() {
        return "sodiumdynamiclights";
    }

    @Override
    public void begin(V3 viewer, int moveBudget) {
        seen.clear();
        moved.clear();
        scheduler.begin();
        changeCount = 0;
        this.viewer = viewer;
        this.budget = moveBudget;
        this.gate = new SourceMotion.Gate(BeamClientConfig.MOVE_THRESHOLD.get(),
                BeamClientConfig.MOVE_HYSTERESIS.get(), BeamClientConfig.CENTRAL_HYSTERESIS.get(),
                BeamClientConfig.LUMINANCE_HYSTERESIS.get());
        this.exactGate = gate.exact();
    }

    @Override
    public void put(long key, V3 pos, int luminance, boolean priority) {
        put(key, pos, luminance, priority, false);
    }

    @Override
    public void put(long key, V3 pos, int luminance, boolean priority, boolean exact) {
        seen.add(key);
        Level level = Minecraft.getInstance().level;
        BeamLightSource src = sources.get(key);
        if (src != null && src.level() != level) {
            src.remove();
            sources.remove(key);
            src = null;
        }
        double displacement;
        if (src != null && src.hasPosition()) {
            double d = src.distSq(pos);
            if (!SourceMotion.shouldChange(d, src.luminance(), luminance, Keys.isCentralHit(key),
                    exact ? exactGate : gate)) {
                SodiumDynamicLights.updateTracking(src);
                return;
            }
            int dl = luminance - src.luminance();
            displacement = d + dl * dl;
            SodiumDynamicLights.updateTracking(src);
        } else {
            displacement = APPEAR_DISPLACEMENT + luminance;
        }
        Change c = change(key, pos, luminance, src, false);
        scheduler.offer(key, priority, viewer.distSq(pos), displacement, c);
    }

    @Override
    public void end() {
        ObjectIterator<Long2ObjectMap.Entry<BeamLightSource>> it = sources.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            Long2ObjectMap.Entry<BeamLightSource> e = it.next();
            if (seen.contains(e.getLongKey())) continue;
            BeamLightSource src = e.getValue();
            double dist = src.hasPosition() ? src.distSq(viewer) : 0;
            scheduler.offer(e.getLongKey(), false, dist, APPEAR_DISPLACEMENT,
                    change(e.getLongKey(), null, 0, src, true));
        }
        deferred = scheduler.schedule(budget);
        Level level = Minecraft.getInstance().level;
        for (int i = 0; i < scheduler.size(); i++) {
            if (!scheduler.allowed(i)) continue;
            Change c = scheduler.payload(i);
            if (c.removal) {
                c.src.remove();
                sources.remove(c.key);
            } else {
                BeamLightSource src = c.src;
                if (src == null) {
                    src = new BeamLightSource(level);
                    sources.put(c.key, src);
                }
                src.set(c.pos, c.lum);
                SodiumDynamicLights.updateTracking(src);
            }
            moved.add(c.key);
        }
        for (int i = 0; i < changeCount; i++) changePool.get(i).src = null;
    }

    private Change change(long key, V3 pos, int lum, BeamLightSource src, boolean removal) {
        Change c;
        if (changeCount < changePool.size()) {
            c = changePool.get(changeCount);
        } else {
            c = new Change();
            changePool.add(c);
        }
        changeCount++;
        c.key = key;
        c.pos = pos;
        c.lum = lum;
        c.src = src;
        c.removal = removal;
        return c;
    }

    @Override
    public void clear() {
        for (BeamLightSource s : sources.values()) s.remove();
        sources.clear();
        seen.clear();
        moved.clear();
        scheduler.clear();
        changeCount = 0;
        deferred = 0;
    }

    @Override public int ownCount() { return sources.size(); }
    @Override public int totalCount() { return sdl.getLightSourcesCount(); }
    @Override public int movesThisTick() { return moved.size(); }
    @Override public int deferredThisTick() { return deferred; }
    @Override public boolean movedThisTick(long key) { return moved.contains(key); }

    @Override
    public String statusLine() {
        return "SDL mode " + sdl.config.getDynamicLightsMode();
    }
}
