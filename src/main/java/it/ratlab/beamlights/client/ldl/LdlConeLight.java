package it.ratlab.beamlights.client.ldl;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import it.ratlab.beamlights.core.ConeLight;
import net.minecraft.core.BlockPos;

/**
 * Cone light along a central beam (config ldlConeLight): falloff and tight bounding box from ConeLight.Prepared.
 * Snapshots are immutable and published through a volatile field (read by LDL's meshing threads); lightAtPos
 * allocates nothing and rejects blocks outside the box or the cone's reach before any square root.
 */
final class LdlConeLight implements DynamicLightBehavior {
    private final DynamicLightBehaviorManager manager;
    private volatile ConeLight.Prepared prepared;
    private volatile BoundingBox box;
    private ConeLight.Prepared lastSeen;
    private volatile boolean removed;

    LdlConeLight(DynamicLightBehaviorManager manager, ConeLight.Shape shape, ConeLight.Look look) {
        this.manager = manager;
        update(shape, look);
        manager.add(this);
    }

    ConeLight.Shape shape() {
        return prepared.shape();
    }

    ConeLight.Look look() {
        return prepared.look();
    }

    void update(ConeLight.Shape s, ConeLight.Look look) {
        ConeLight.Prepared p = new ConeLight.Prepared(s, look, LdlBackend.FALLOFF);
        ConeLight.Box b = p.box();
        box = new BoundingBox(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
        prepared = p;
    }

    void remove() {
        removed = true;
        manager.remove(this);
    }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        return prepared.lightAtBlock(pos.getX(), pos.getY(), pos.getZ(), falloffRatio);
    }

    @Override
    public BoundingBox getBoundingBox() {
        return box;
    }

    @Override
    public boolean hasChanged() {
        ConeLight.Prepared p = prepared;
        if (p == lastSeen) return false;
        lastSeen = p;
        return true;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }
}
