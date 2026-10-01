package it.ratlab.beamlights.client.ldl;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import it.ratlab.beamlights.api.math.V3;
import it.ratlab.beamlights.client.PooledLight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A free-floating LambDynamicLights light: a sphere with LDL's own falloff (level - falloffRatio * distance; light 15
 * reaches 7.75 blocks, like SDL). Moved on the client thread, read by LDL's chunk meshing threads, so the state is
 * one immutable snapshot behind a volatile field. LDL rebuilds the sections of the old and new bounding box itself
 * whenever hasChanged() is true.
 */
final class LdlPointLight implements DynamicLightBehavior, PooledLight {
    private record State(double x, double y, double z, int luminance) {
    }

    private static final State OFF = new State(0, 0, 0, 0);

    private final DynamicLightBehaviorManager manager;
    private final Level level;
    private volatile State state = OFF;
    private State lastSeen = OFF;
    private boolean registered;
    private volatile boolean removed;

    LdlPointLight(DynamicLightBehaviorManager manager, Level level) {
        this.manager = manager;
        this.level = level;
    }

    @Override public Level level() { return level; }
    @Override public boolean hasPosition() { return registered; }
    @Override public int luminance() { return state.luminance(); }

    @Override
    public double distSq(V3 p) {
        State s = state;
        double dx = s.x() - p.x(), dy = s.y() - p.y(), dz = s.z() - p.z();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public void set(V3 pos, int lum) {
        state = new State(pos.x(), pos.y(), pos.z(), Math.max(0, Math.min(15, lum)));
        if (!registered && !removed) {
            registered = true;
            manager.add(this);
        }
    }

    @Override
    public void remove() {
        removed = true;
        if (registered) manager.remove(this);
    }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        State s = state;
        double dx = pos.getX() + 0.5 - s.x(), dy = pos.getY() + 0.5 - s.y(), dz = pos.getZ() + 0.5 - s.z();
        return s.luminance() - falloffRatio * Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public BoundingBox getBoundingBox() {
        State s = state;
        double r = s.luminance() / LdlBackend.FALLOFF;
        return new BoundingBox((int) Math.floor(s.x() - r), (int) Math.floor(s.y() - r), (int) Math.floor(s.z() - r),
                (int) Math.floor(s.x() + r), (int) Math.floor(s.y() + r), (int) Math.floor(s.z() + r));
    }

    @Override
    public boolean hasChanged() {
        State s = state;
        if (s == lastSeen) return false;
        lastSeen = s;
        return true;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }
}
