package it.ratlab.beamlights.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the side rays of a beam from a pattern. The central ray is never included (the ticker adds it). */
public final class RayLayout {
    public static final int MAX_SIDE_RAYS = 24;

    public enum Pattern { CENTER_ONLY, TRIANGLE, CROSS, RING, DOUBLE_RING, FAN_HORIZONTAL, FAN_VERTICAL, CUSTOM }

    private RayLayout() {
    }

    public static List<RaySpec> build(Pattern pattern, int sideRays, double spread, double rollOffsetDeg,
                                      int innerRays, double innerSpread, List<String> custom, int lumOffset,
                                      boolean midpoints, double rangeFactor) {
        List<RaySpec> out = new ArrayList<>();
        int n = Math.max(1, sideRays);
        switch (pattern) {
            case CENTER_ONLY -> {
            }
            case TRIANGLE -> ring(out, 3, spread, rollOffsetDeg, lumOffset, midpoints, rangeFactor);
            case CROSS -> ring(out, 4, spread, rollOffsetDeg, lumOffset, midpoints, rangeFactor);
            case RING -> ring(out, n, spread, rollOffsetDeg, lumOffset, midpoints, rangeFactor);
            case DOUBLE_RING -> {
                int inner = Math.max(1, innerRays);
                ring(out, inner, innerSpread, rollOffsetDeg + 180.0 / inner, lumOffset, midpoints, rangeFactor);
                ring(out, n, spread, rollOffsetDeg, lumOffset, midpoints, rangeFactor);
            }
            case FAN_HORIZONTAL -> fan(out, n, spread, 0, lumOffset, midpoints, rangeFactor);
            case FAN_VERTICAL -> fan(out, n, spread, 90, lumOffset, midpoints, rangeFactor);
            case CUSTOM -> {
                if (custom != null) {
                    for (String entry : custom) {
                        RaySpec s = parse(entry, lumOffset, midpoints, rangeFactor);
                        if (s != null) out.add(s);
                    }
                }
            }
        }
        return out.size() > MAX_SIDE_RAYS ? List.copyOf(out.subList(0, MAX_SIDE_RAYS)) : List.copyOf(out);
    }

    /** Number of custom entries that {@link #parse} rejects. */
    public static int invalidCount(List<String> custom) {
        if (custom == null) return 0;
        int bad = 0;
        for (String entry : custom) if (parse(entry, 0, false, 1.0) == null) bad++;
        return bad;
    }

    /** "spread,roll[,lumOffset[,midpoints[,rangeFactor]]]"; missing fields take the defaults; null when invalid. */
    public static RaySpec parse(String entry, int lumOffset, boolean midpoints, double rangeFactor) {
        if (entry == null) return null;
        String[] f = entry.split(",", -1);
        if (f.length < 2 || f.length > 5) return null;
        try {
            double spread = Double.parseDouble(f[0].trim());
            double roll = Double.parseDouble(f[1].trim());
            int lum = f.length > 2 ? Integer.parseInt(f[2].trim()) : lumOffset;
            boolean mid = midpoints;
            if (f.length > 3) {
                String m = f[3].trim().toLowerCase(Locale.ROOT);
                if (m.equals("true")) mid = true;
                else if (m.equals("false")) mid = false;
                else return null;
            }
            double range = f.length > 4 ? Double.parseDouble(f[4].trim()) : rangeFactor;
            if (!(spread >= 0 && spread <= 1) || !Double.isFinite(roll)) return null;
            if (lum < -15 || lum > 15) return null;
            if (!(range >= 0.1 && range <= 1.0)) return null;
            return new RaySpec(spread, norm(roll), lum, mid, range);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void ring(List<RaySpec> out, int count, double spread, double start, int lum, boolean mid,
                             double range) {
        for (int i = 0; i < count; i++) out.add(new RaySpec(spread, norm(start + i * 360.0 / count), lum, mid, range));
    }

    /** Alternates both sides of a line (roll base / base+180), tilt growing from spread/n to spread. */
    private static void fan(List<RaySpec> out, int count, double spread, double base, int lum, boolean mid,
                            double range) {
        for (int i = 0; i < count; i++) {
            double roll = i % 2 == 0 ? base : base + 180;
            out.add(new RaySpec(spread * (i + 1) / count, norm(roll), lum, mid, range));
        }
    }

    static double norm(double deg) {
        double r = deg % 360.0;
        return r < 0 ? r + 360.0 : r;
    }
}
