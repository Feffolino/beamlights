package it.ratlab.beamlights.client;

import it.ratlab.beamlights.api.math.V3;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.Keys;
import it.ratlab.beamlights.core.MoveScheduler;
import it.ratlab.beamlights.core.SourceMotion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared change gating of the light backends: one pooled light per stable key. Changes (move past the threshold and
 * hysteresis, luminance change past the tolerance, new light, removal; see SourceMotion.shouldChange) are collected
 * during the tick and applied in end() within the move budget (MoveScheduler); the rest keeps its old state and is
 * offered again next tick. Subclasses may offer extra changes (offerExtra) that share the same budget.
 */
public abstract class GatedLightBackend<S extends PooledLight> implements LightBackend {
    /** Ranking weight of new and removed lights: before plain moves at the same distance. */
    protected static final double APPEAR_DISPLACEMENT = 1e6;

    private static final class Change<S> {
        long key;
        V3 pos;
        int lum;
        S src;
        boolean removal;
        Runnable extra;
    }

    private final Long2ObjectOpenHashMap<S> sources = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet seen = new LongOpenHashSet();
    private final LongOpenHashSet moved = new LongOpenHashSet();
    private final MoveScheduler<Change<S>> scheduler = new MoveScheduler<>();
    private final List<Change<S>> changePool = new ArrayList<>();
    private int changeCount;
    private V3 viewer = V3.ZERO;
    private int budget;
    private SourceMotion.Gate gate = SourceMotion.Gate.legacy(0.25);
    private SourceMotion.Gate exactGate = gate;
    private int deferred;

    /** A new, not yet registered light in level. */
    protected abstract S newSource(Level level);

    /** False while the light engine is switched off: every light is removed and puts are ignored. */
    protected boolean active() {
        return true;
    }

    protected final V3 viewer() {
        return viewer;
    }

    /** Gate of this tick (exact = the emitter just settled). */
    protected final SourceMotion.Gate gate(boolean exact) {
        return exact ? exactGate : gate;
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
        if (!active() && !sources.isEmpty()) clearSources();
    }

    @Override
    public void put(long key, V3 pos, int luminance, boolean priority) {
        put(key, pos, luminance, priority, false);
    }

    @Override
    public void put(long key, V3 pos, int luminance, boolean priority, boolean exact) {
        if (!active()) return;
        seen.add(key);
        Level level = Minecraft.getInstance().level;
        S src = sources.get(key);
        if (src != null && src.level() != level) {
            src.remove();
            sources.remove(key);
            src = null;
        }
        double displacement;
        if (src != null && src.hasPosition()) {
            double d = src.distSq(pos);
            src.touch();
            if (!SourceMotion.shouldChange(d, src.luminance(), luminance, Keys.isCentralHit(key), gate(exact))) return;
            int dl = luminance - src.luminance();
            displacement = d + dl * dl;
        } else {
            displacement = APPEAR_DISPLACEMENT + luminance;
        }
        scheduler.offer(key, priority, viewer.distSq(pos), displacement, change(key, pos, luminance, src, false, null));
    }

    /** Offers a subclass change (e.g. an LDL cone) to the shared budget; apply runs in end() when allowed. */
    protected final void offerExtra(long key, boolean priority, double distSq, double displacementSq, Runnable apply) {
        scheduler.offer(key, priority, distSq, displacementSq, change(key, null, 0, null, false, apply));
    }

    /** Called in end() before scheduling: subclasses offer removals of their own unseen lights here. */
    protected void beforeSchedule() {
    }

    @Override
    public void end() {
        if (active()) {
            ObjectIterator<Long2ObjectMap.Entry<S>> it = sources.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                Long2ObjectMap.Entry<S> e = it.next();
                if (seen.contains(e.getLongKey())) continue;
                S src = e.getValue();
                double dist = src.hasPosition() ? src.distSq(viewer) : 0;
                scheduler.offer(e.getLongKey(), false, dist, APPEAR_DISPLACEMENT,
                        change(e.getLongKey(), null, 0, src, true, null));
            }
        }
        beforeSchedule();
        deferred = scheduler.schedule(budget);
        Level level = Minecraft.getInstance().level;
        for (int i = 0; i < scheduler.size(); i++) {
            if (!scheduler.allowed(i)) continue;
            Change<S> c = scheduler.payload(i);
            if (c.extra != null) {
                c.extra.run();
                continue;
            }
            if (c.removal) {
                c.src.remove();
                sources.remove(c.key);
            } else {
                S src = c.src;
                if (src == null) {
                    src = newSource(level);
                    sources.put(c.key, src);
                }
                src.set(c.pos, c.lum);
                src.touch();
            }
            moved.add(c.key);
        }
        releaseChanges();
    }

    private Change<S> change(long key, V3 pos, int lum, S src, boolean removal, Runnable extra) {
        Change<S> c;
        if (changeCount < changePool.size()) {
            c = changePool.get(changeCount);
        } else {
            c = new Change<>();
            changePool.add(c);
        }
        changeCount++;
        c.key = key;
        c.pos = pos;
        c.lum = lum;
        c.src = src;
        c.removal = removal;
        c.extra = extra;
        return c;
    }

    private void releaseChanges() {
        for (int i = 0; i < changeCount; i++) {
            Change<S> c = changePool.get(i);
            c.src = null;
            c.extra = null;
        }
    }

    private void clearSources() {
        for (S s : sources.values()) s.remove();
        sources.clear();
    }

    @Override
    public void clear() {
        clearSources();
        seen.clear();
        moved.clear();
        scheduler.clear();
        releaseChanges();
        changeCount = 0;
        deferred = 0;
    }

    @Override public int ownCount() { return sources.size(); }
    @Override public int movesThisTick() { return moved.size(); }
    @Override public int deferredThisTick() { return deferred; }
    @Override public boolean movedThisTick(long key) { return moved.contains(key); }
}
