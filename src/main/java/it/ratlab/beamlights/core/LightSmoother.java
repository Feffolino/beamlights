package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sits between the ticker and the backend and softens light movement: small moves glide (lerp), jumps crossfade (a
 * ghost fades out at the old spot while the light ramps up at the new one), new lights ramp up and vanished lights
 * fade out. Ghosts use Keys.ghost(key), at most one per key. Client thread only.
 */
public final class LightSmoother {
    public record Target(long key, V3 pos, int luminance) {
    }

    public record Output(long key, V3 pos, int luminance, boolean ghost) {
    }

    public record Settings(boolean enabled, double smoothFactor, double jumpDistance, int fadeTicks) {
    }

    static final double SNAP_DISTANCE = 0.05;

    private static final class Live {
        V3 pos;
        int lum;
        /** Ramp-up tick (1..fadeTicks), 0 when not ramping. */
        int ramp;
    }

    private static final class Ghost {
        final V3 pos;
        final int startLum;
        int tick;
        int lum;

        Ghost(V3 pos, int startLum) {
            this.pos = pos;
            this.startLum = startLum;
        }
    }

    private final Map<Long, Live> live = new LinkedHashMap<>();
    private final Map<Long, Ghost> ghosts = new LinkedHashMap<>();

    public List<Output> update(List<Target> targets, Settings s) {
        List<Output> out = new ArrayList<>(targets.size() + ghosts.size());
        if (!s.enabled()) {
            clear();
            for (Target t : targets) out.add(new Output(t.key(), t.pos(), t.luminance(), false));
            return out;
        }
        int fade = Math.max(1, s.fadeTicks());
        int lumStep = (15 + fade - 1) / fade;
        double jumpSq = s.jumpDistance() * s.jumpDistance();
        Set<Long> targeted = new HashSet<>();
        // Ghosts spawned this tick already show their first fade step.
        Set<Long> fresh = new HashSet<>();

        for (Target t : targets) {
            if (t.luminance() <= 0 || !targeted.add(t.key())) continue;
            Live l = live.get(t.key());
            if (l == null) {
                l = new Live();
                live.put(t.key(), l);
                startRamp(l, t, fade);
            } else if (l.pos.distSq(t.pos()) >= jumpSq) {
                spawnGhost(t.key(), l, fade, fresh);
                startRamp(l, t, fade);
            } else {
                V3 p = l.pos.add(t.pos().sub(l.pos).scale(s.smoothFactor()));
                l.pos = p.distSq(t.pos()) < SNAP_DISTANCE * SNAP_DISTANCE ? t.pos() : p;
                if (l.ramp > 0 && l.ramp < fade) {
                    l.ramp++;
                    l.lum = rampLum(t.luminance(), l.ramp, fade);
                } else {
                    l.ramp = 0;
                    l.lum += Math.max(-lumStep, Math.min(lumStep, t.luminance() - l.lum));
                }
            }
            if (l.lum > 0) out.add(new Output(t.key(), l.pos, l.lum, false));
        }

        Iterator<Map.Entry<Long, Live>> it = live.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Live> e = it.next();
            if (targeted.contains(e.getKey())) continue;
            spawnGhost(e.getKey(), e.getValue(), fade, fresh);
            it.remove();
        }

        Iterator<Map.Entry<Long, Ghost>> git = ghosts.entrySet().iterator();
        while (git.hasNext()) {
            Map.Entry<Long, Ghost> e = git.next();
            Ghost g = e.getValue();
            if (!fresh.contains(e.getKey())) {
                g.tick++;
                g.lum = fadeLum(g.startLum, g.tick, fade);
            }
            if (g.lum <= 0) {
                git.remove();
                continue;
            }
            out.add(new Output(e.getKey(), g.pos, g.lum, true));
        }
        return out;
    }

    public void clear() {
        live.clear();
        ghosts.clear();
    }

    public int ghostCount() {
        return ghosts.size();
    }

    private void spawnGhost(long key, Live l, int fade, Set<Long> fresh) {
        long gk = Keys.ghost(key);
        Ghost g = new Ghost(l.pos, l.lum);
        g.tick = 1;
        g.lum = fadeLum(l.lum, 1, fade);
        // Replaces an older ghost of the same key at once.
        ghosts.put(gk, g);
        fresh.add(gk);
    }

    private static void startRamp(Live l, Target t, int fade) {
        l.pos = t.pos();
        l.ramp = fade > 1 ? 1 : 0;
        l.lum = rampLum(t.luminance(), 1, fade);
    }

    /** Linear ramp from 0 to target over fade ticks, at least 1 while the target is lit. */
    static int rampLum(int target, int tick, int fade) {
        if (target <= 0) return 0;
        return Math.max(1, (int) Math.round(target * (double) Math.min(tick, fade) / fade));
    }

    /** Linear fade from start to 0 over fade ticks. */
    static int fadeLum(int start, int tick, int fade) {
        if (tick >= fade) return 0;
        return (int) Math.round(start * (double) (fade - tick) / fade);
    }
}
