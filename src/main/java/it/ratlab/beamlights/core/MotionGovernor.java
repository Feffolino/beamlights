package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motion-adaptive updates of one emitter. Every light change costs chunk rebuilds, and while the beam turns fast the
 * side rays and midpoints sweep across many blocks per second where nobody can follow them anyway. Per emitter:
 * <ul>
 * <li>speed: angular speed of the central beam direction (deg/s) and origin speed (blocks/s), EMA-smoothed;</li>
 * <li>state: STILL, MOVING, FAST with hysteresis (FAST is left only below 0.7 x the fast thresholds, STILL needs
 * settleTicks slow ticks in a row);</li>
 * <li>filter: the central hit points always pass; side ray and midpoint targets are held (FAST, FREEZE), dropped so
 * they fade out (FAST, FADE), refreshed every sideUpdateTicks (MOVING) or passed through (STILL, mode OFF).</li>
 * </ul>
 * Held targets go through the smoother like fresh ones, so a frozen light simply stays where it is. On reaching
 * STILL the emitter is in resync for settleTicks ticks: the backend then moves side lights to their exact target
 * (no hysteresis). Client thread only.
 */
public final class MotionGovernor {
    public enum State { STILL, MOVING, FAST }

    /** What side rays and midpoints do while FAST: keep their lights, fade them out, or no adaptation at all. */
    public enum SideMode { FREEZE, FADE, OFF }

    /** FAST is left only below this fraction of the fast thresholds. */
    public static final double FAST_EXIT = 0.7;
    /** Slow origin speed = fastMove / this. */
    public static final double SLOW_MOVE_DIVISOR = 4.0;
    public static final double EMA_ALPHA = 0.5;
    public static final double SECONDS_PER_TICK = 0.05;

    public record Settings(double fastTurnDegPerSec, double slowTurnDegPerSec, double fastMoveBlocksPerSec,
                           int sideUpdateTicks, int settleTicks, SideMode mode) {
        public static final Settings DEFAULT = new Settings(90, 20, 8, 3, 6, SideMode.FREEZE);

        double slowMove() {
            return fastMoveBlocksPerSec / SLOW_MOVE_DIVISOR;
        }
    }

    private State state = State.STILL;
    private V3 lastOrigin, lastDir;
    private double turn, move;
    private int slowTicks;
    private int sinceSide = Integer.MAX_VALUE / 2;
    private int resyncLeft;
    private boolean sideDue = true;
    private int suppressed;
    private final Map<Long, LightSmoother.Target> held = new LinkedHashMap<>();

    /** Angle in degrees between two directions (any length; 0 when one is zero). */
    public static double angleDeg(V3 a, V3 b) {
        double la = Math.sqrt(a.distSq(V3.ZERO)), lb = Math.sqrt(b.distSq(V3.ZERO));
        if (la < 1e-9 || lb < 1e-9) return 0;
        double cos = (a.x() * b.x() + a.y() * b.y() + a.z() * b.z()) / (la * lb);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos))));
    }

    public static double ema(double previous, double sample, double alpha) {
        return previous + (sample - previous) * alpha;
    }

    /**
     * One observation of the emitter: central direction and origin of its first beam, elapsedTicks since the last
     * observation (remote emitters are re-traced every N ticks). Updates speeds, state, the side refresh decision and
     * the resync window. Returns the new state.
     */
    public State observe(V3 origin, V3 dir, int elapsedTicks, Settings s) {
        int dt = Math.max(1, elapsedTicks);
        double seconds = dt * SECONDS_PER_TICK;
        double turnSample = lastDir == null ? 0 : angleDeg(lastDir, dir) / seconds;
        double moveSample = lastOrigin == null ? 0 : Math.sqrt(lastOrigin.distSq(origin)) / seconds;
        lastDir = dir;
        lastOrigin = origin;
        turn = ema(turn, turnSample, EMA_ALPHA);
        move = ema(move, moveSample, EMA_ALPHA);
        return advance(dt, s);
    }

    /** State step from the current speeds (package-private for tests: setSpeeds + advance). */
    State advance(int dt, Settings s) {
        State prev = state;
        boolean fastIn = turn >= s.fastTurnDegPerSec() || move >= s.fastMoveBlocksPerSec();
        boolean fastHold = turn >= s.fastTurnDegPerSec() * FAST_EXIT || move >= s.fastMoveBlocksPerSec() * FAST_EXIT;
        boolean slow = turn < s.slowTurnDegPerSec() && move < s.slowMove();
        slowTicks = slow ? slowTicks + dt : 0;
        if (fastIn || (prev == State.FAST && fastHold)) {
            state = State.FAST;
        } else if (slow && (prev == State.STILL || slowTicks >= Math.max(1, s.settleTicks()))) {
            state = State.STILL;
        } else {
            state = State.MOVING;
        }
        if (state == State.STILL && prev != State.STILL) resyncLeft = Math.max(1, s.settleTicks());
        else if (resyncLeft > 0) resyncLeft = Math.max(0, resyncLeft - dt);
        // Side refresh: always when STILL or without adaptation, every sideUpdateTicks while MOVING, never while FAST.
        if (sinceSide < Integer.MAX_VALUE / 2) sinceSide += dt;
        if (s.mode() == SideMode.OFF || state == State.STILL) {
            sideDue = true;
        } else if (state == State.MOVING) {
            sideDue = sinceSide >= Math.max(1, s.sideUpdateTicks());
        } else {
            sideDue = false;
        }
        if (sideDue) sinceSide = 0;
        return state;
    }

    /**
     * Appends the targets the smoother may see this tick to out: central hit points always; side and midpoint
     * targets fresh when a side refresh is due, otherwise the held ones (FREEZE / MOVING) or none (FAST with FADE).
     * {@link #suppressed()} counts the side/midpoint targets that differ (block or luminance) from what was passed.
     */
    public void filter(List<LightSmoother.Target> fresh, List<LightSmoother.Target> out, Settings s) {
        suppressed = 0;
        boolean fade = state == State.FAST && s.mode() == SideMode.FADE;
        if (sideDue) held.clear();
        int matched = 0;
        for (LightSmoother.Target t : fresh) {
            if (Keys.isCentralHit(t.key())) {
                out.add(t);
            } else if (sideDue) {
                held.put(t.key(), t);
                out.add(t);
            } else {
                LightSmoother.Target h = held.get(t.key());
                if (h == null) {
                    suppressed++;
                } else {
                    matched++;
                    if (h.luminance() != t.luminance()
                            || !SourceMotion.snapToBlock(h.pos()).equals(SourceMotion.snapToBlock(t.pos()))) {
                        suppressed++;
                    }
                }
            }
        }
        if (sideDue) return;
        // Held lights whose ray no longer produces them stay too (they would fade out otherwise).
        suppressed += held.size() - matched;
        if (!fade) out.addAll(held.values());
    }

    public State state() {
        return state;
    }

    /** Smoothed angular speed of the central direction, deg/s. */
    public double turnDegPerSec() {
        return turn;
    }

    /** Smoothed origin speed, blocks/s. */
    public double moveBlocksPerSec() {
        return move;
    }

    /** True for settleTicks ticks after reaching STILL: side lights go to their exact target. */
    public boolean resync() {
        return resyncLeft > 0;
    }

    public int suppressed() {
        return suppressed;
    }

    /** Test hook: sets the smoothed speeds directly. */
    void setSpeeds(double turnDegPerSec, double moveBlocksPerSec) {
        turn = turnDegPerSec;
        move = moveBlocksPerSec;
    }
}
