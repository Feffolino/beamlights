package it.ratlab.beamlights.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Smooths the LENGTH of cone lights (apex, direction and luminance follow at once; direction changes are already gated
 * by motion). A length jump (>= jumpDistance, e.g. beam moving from a near to a far face) is walked in coneSteps equal
 * steps, one every glideMinTicks ticks; smaller differences lerp by smoothFactor and snap when within 0.25. A cone whose
 * target vanished fades its luminance to 0 in fadeSteps (same quantization as LightSmoother) and is then dropped.
 * Client thread only.
 */
public final class ConeSmoother {
    public record Target(long key, ConeLight.Shape shape) {
    }

    public record Settings(boolean enabled, double smoothFactor, double jumpDistance, int coneSteps, int glideMinTicks,
                           int fadeTicks, int fadeSteps) {
    }

    static final double SNAP_LENGTH = 0.25;
    private static final int FAR = Integer.MAX_VALUE / 2;

    private static final class Live {
        double shown;
        double goal;
        double delta;
        int remaining;
        int sinceStep = FAR;
        ConeLight.Shape last;
    }

    private static final class Ghost {
        final ConeLight.Shape shape;
        int tick;

        Ghost(ConeLight.Shape shape) {
            this.shape = shape;
        }
    }

    private final Map<Long, Live> live = new LinkedHashMap<>();
    private final Map<Long, Ghost> ghosts = new LinkedHashMap<>();
    private final Set<Long> targeted = new HashSet<>();

    public List<Target> update(List<Target> targets, Settings s) {
        List<Target> out = new ArrayList<>(targets.size() + ghosts.size());
        if (!s.enabled()) {
            clear();
            out.addAll(targets);
            return out;
        }
        int fade = Math.max(1, s.fadeTicks());
        int glideMin = Math.max(1, s.glideMinTicks());
        int steps = Math.max(1, s.coneSteps());
        targeted.clear();
        for (Target t : targets) {
            if (!targeted.add(t.key())) continue;
            ghosts.remove(t.key());
            ConeLight.Shape sh = t.shape();
            double len = sh.length();
            Live l = live.get(t.key());
            if (l == null) {
                l = new Live();
                l.shown = len;
                l.goal = len;
                live.put(t.key(), l);
            } else {
                if (l.sinceStep < FAR) l.sinceStep++;
                if (l.remaining > 0 && Math.abs(len - l.goal) >= s.jumpDistance()) l.remaining = 0;
                if (l.remaining == 0 && Math.abs(len - l.shown) >= s.jumpDistance()) {
                    l.goal = len;
                    l.remaining = steps;
                    l.delta = (len - l.shown) / steps;
                    l.sinceStep = FAR;
                }
                if (l.remaining > 0) {
                    if (l.sinceStep >= glideMin) {
                        l.remaining--;
                        l.shown = l.remaining == 0 ? l.goal : l.shown + l.delta;
                        l.sinceStep = 0;
                    }
                } else if (Math.abs(len - l.shown) < SNAP_LENGTH) {
                    l.shown = len;
                } else {
                    l.shown += (len - l.shown) * s.smoothFactor();
                }
            }
            ConeLight.Shape shown = withLength(sh, l.shown);
            l.last = shown;
            out.add(new Target(t.key(), shown));
        }

        Iterator<Map.Entry<Long, Live>> it = live.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Live> e = it.next();
            if (targeted.contains(e.getKey())) continue;
            ghosts.put(e.getKey(), new Ghost(e.getValue().last));
            it.remove();
        }
        Iterator<Map.Entry<Long, Ghost>> git = ghosts.entrySet().iterator();
        while (git.hasNext()) {
            Map.Entry<Long, Ghost> e = git.next();
            Ghost g = e.getValue();
            g.tick++;
            int lum = LightSmoother.fadeLum(g.shape.luminance(), g.tick, fade, s.fadeSteps());
            if (lum <= 0) {
                git.remove();
                continue;
            }
            ConeLight.Shape o = g.shape;
            out.add(new Target(e.getKey(), new ConeLight.Shape(o.origin(), o.axis(), o.length(), o.halfAngleDeg(), lum)));
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

    private static ConeLight.Shape withLength(ConeLight.Shape o, double length) {
        return length == o.length() ? o
                : new ConeLight.Shape(o.origin(), o.axis(), length, o.halfAngleDeg(), o.luminance());
    }
}
