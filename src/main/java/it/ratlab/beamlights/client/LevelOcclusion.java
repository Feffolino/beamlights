package it.ratlab.beamlights.client;

import it.ratlab.beamlights.core.BeamTracer;
import it.ratlab.beamlights.core.LightPointPlanner;
import it.ratlab.beamlights.core.math.V3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Transparency rule of the spec: a block stops the beam when canOcclude(); everything else is passed through. */
public final class LevelOcclusion implements BeamTracer.BlockTest, LightPointPlanner.OccluderProbe {
    private static final int PROBE_RADIUS = 4;
    private static final int[][] AXES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private final Level level;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public LevelOcclusion(Level level) {
        this.level = level;
    }

    @Override
    public boolean stops(int x, int y, int z) {
        if (level.isOutsideBuildHeight(y)) return false;
        return level.getBlockState(cursor.set(x, y, z)).canOcclude();
    }

    @Override
    public boolean nearOccluder(V3 pos) {
        int bx = (int) Math.floor(pos.x()), by = (int) Math.floor(pos.y()), bz = (int) Math.floor(pos.z());
        for (int[] a : AXES) {
            for (int d = 1; d <= PROBE_RADIUS; d++) {
                if (stops(bx + a[0] * d, by + a[1] * d, bz + a[2] * d)) return true;
            }
        }
        return false;
    }
}
