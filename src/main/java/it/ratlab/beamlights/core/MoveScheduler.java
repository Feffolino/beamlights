package it.ratlab.beamlights.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global per-tick budget of light source changes (move, luminance change, creation, removal). Each change costs
 * chunk-section rebuilds, so past the budget the rest waits for the next tick. Order: priority (local player's
 * central ray), then keys deferred {@link #MAX_AGE} ticks in a row (no starvation), then nearest to the viewer, then
 * largest displacement. Candidate objects are pooled; client thread only.
 *
 * @param <T> backend payload carried with each candidate
 */
public final class MoveScheduler<T> {
    /** A key deferred this many ticks in a row is ranked before distance. */
    public static final int MAX_AGE = 4;

    private static final class Candidate<T> {
        long key;
        boolean priority;
        double distSq;
        double displacementSq;
        int age;
        boolean allowed;
        T payload;

        boolean aged() {
            return age >= MAX_AGE;
        }
    }

    private static final Comparator<Candidate<?>> ORDER = (a, b) -> {
        if (a.priority != b.priority) return a.priority ? -1 : 1;
        if (a.aged() != b.aged()) return a.aged() ? -1 : 1;
        int c = Double.compare(a.distSq, b.distSq);
        if (c != 0) return c;
        c = Double.compare(b.displacementSq, a.displacementSq);
        return c != 0 ? c : Long.compare(a.key, b.key);
    };

    private final List<Candidate<T>> pool = new ArrayList<>();
    private final List<Candidate<T>> active = new ArrayList<>();
    private final Map<Long, Integer> ages = new HashMap<>();
    private int deferred;

    public void begin() {
        active.clear();
        deferred = 0;
    }

    public void offer(long key, boolean priority, double distSq, double displacementSq, T payload) {
        Candidate<T> c;
        if (active.size() < pool.size()) {
            c = pool.get(active.size());
        } else {
            c = new Candidate<>();
            pool.add(c);
        }
        c.key = key;
        c.priority = priority;
        c.distSq = distSq;
        c.displacementSq = displacementSq;
        Integer age = ages.isEmpty() ? null : ages.get(key);
        c.age = age == null ? 0 : age;
        c.allowed = false;
        c.payload = payload;
        active.add(c);
    }

    /**
     * Ranks the offered candidates and allows the first {@code budget} (0 or less = unlimited). Returns the number
     * deferred. Keys not offered this tick lose their deferral age.
     */
    public int schedule(int budget) {
        int n = active.size();
        int allow = budget <= 0 ? n : Math.min(budget, n);
        if (allow < n) active.sort(ORDER);
        ages.clear();
        for (int i = 0; i < n; i++) {
            Candidate<T> c = active.get(i);
            c.allowed = i < allow;
            if (!c.allowed) ages.put(c.key, c.age + 1);
        }
        deferred = n - allow;
        return deferred;
    }

    public int size() {
        return active.size();
    }

    public long key(int i) {
        return active.get(i).key;
    }

    public boolean allowed(int i) {
        return active.get(i).allowed;
    }

    public T payload(int i) {
        return active.get(i).payload;
    }

    public int deferred() {
        return deferred;
    }

    /** Drops payload references and ages (backend clear). */
    public void clear() {
        for (Candidate<T> c : pool) c.payload = null;
        active.clear();
        ages.clear();
        deferred = 0;
    }
}
