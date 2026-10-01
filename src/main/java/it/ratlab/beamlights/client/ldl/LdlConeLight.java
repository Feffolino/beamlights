package it.ratlab.beamlights.client.ldl;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import it.ratlab.beamlights.core.ConeLight;
import net.minecraft.core.BlockPos;

/**
 * Cone light along a central beam (config ldlConeLight): falloff from ConeLight, bounding box = cone plus reach.
 * Shape snapshots are immutable and published through a volatile field (read by LDL's meshing threads).
 */
final class LdlConeLight implements DynamicLightBehavior {
    private final DynamicLightBehaviorManager manager;
    private volatile ConeLight.Shape shape;
    private volatile ConeLight.Box box;
    private ConeLight.Shape lastSeen;
    private volatile boolean removed;

    LdlConeLight(DynamicLightBehaviorManager manager, ConeLight.Shape shape) {
        this.manager = manager;
        update(shape);
        manager.add(this);
    }

    ConeLight.Shape shape() {
        return shape;
    }

    void update(ConeLight.Shape s) {
        box = ConeLight.bounds(s, LdlBackend.FALLOFF);
        shape = s;
    }

    void remove() {
        removed = true;
        manager.remove(this);
    }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        return ConeLight.lightAtBlock(shape, pos.getX(), pos.getY(), pos.getZ(), falloffRatio);
    }

    @Override
    public BoundingBox getBoundingBox() {
        ConeLight.Box b = box;
        return new BoundingBox(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
    }

    @Override
    public boolean hasChanged() {
        ConeLight.Shape s = shape;
        if (s == lastSeen) return false;
        lastSeen = s;
        return true;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }
}
