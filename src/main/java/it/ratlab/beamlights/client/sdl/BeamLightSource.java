package it.ratlab.beamlights.client.sdl;

import it.ratlab.beamlights.api.math.V3;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import toni.sodiumdynamiclights.DynamicLightSource;
import toni.sodiumdynamiclights.DynamicLightsMode;
import toni.sodiumdynamiclights.SodiumDynamicLights;

/** A free-floating SDL light source (no entity). Chunk tracking copied from SDL's entity implementation. */
public final class BeamLightSource implements DynamicLightSource {
    private final Level level;
    private double x, y, z;
    private boolean hasPosition;
    private int luminance;

    private double prevX, prevY, prevZ;
    private int lastLuminance;
    private long lastUpdate;
    private LongOpenHashSet trackedLitChunkPos = new LongOpenHashSet();

    BeamLightSource(Level level) {
        this.level = level;
    }

    Level level() {
        return level;
    }

    boolean hasPosition() {
        return hasPosition;
    }

    V3 position() {
        return new V3(x, y, z);
    }

    int luminance() {
        return luminance;
    }

    void set(V3 pos, int lum) {
        x = pos.x();
        y = pos.y();
        z = pos.z();
        hasPosition = true;
        luminance = lum;
    }

    /** Turns the light off, rebuilds the sections it lit and unregisters it from SDL. */
    void remove() {
        luminance = 0;
        LevelRenderer renderer = Minecraft.getInstance().levelRenderer;
        if (renderer != null) sodiumdynamiclights$scheduleTrackedChunksRebuild(renderer);
        trackedLitChunkPos.clear();
        SodiumDynamicLights.get().removeLightSource(this);
    }

    @Override public double sdl$getDynamicLightX() { return x; }
    @Override public double sdl$getDynamicLightY() { return y; }
    @Override public double sdl$getDynamicLightZ() { return z; }
    @Override public Level sdl$getDynamicLightLevel() { return level; }
    @Override public int sdl$getLuminance() { return luminance; }
    @Override public void sdl$resetDynamicLight() { lastLuminance = 0; }

    @Override
    public void sdl$dynamicLightTick() {
        // Luminance is pushed by SdlBackend.put, nothing to compute here.
    }

    @Override
    public boolean sdl$shouldUpdateDynamicLight() {
        DynamicLightsMode mode = SodiumDynamicLights.get().config.getDynamicLightsMode();
        if (!mode.isEnabled()) return false;
        if (mode.hasDelay()) {
            long now = System.currentTimeMillis();
            if (now < lastUpdate + mode.getDelay()) return false;
            lastUpdate = now;
        }
        return true;
    }

    @Override
    public boolean sodiumdynamiclights$updateDynamicLight(LevelRenderer renderer) {
        if (!sdl$shouldUpdateDynamicLight()) return false;
        boolean moved = Math.abs(x - prevX) > 0.1 || Math.abs(y - prevY) > 0.1 || Math.abs(z - prevZ) > 0.1;
        if (!moved && luminance == lastLuminance) return false;
        prevX = x;
        prevY = y;
        prevZ = z;
        lastLuminance = luminance;

        LongOpenHashSet newTracked = new LongOpenHashSet();
        if (luminance > 0) {
            BlockPos.MutableBlockPos section = new BlockPos.MutableBlockPos(
                    SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z));
            SodiumDynamicLights.scheduleChunkRebuild(renderer, section);
            SodiumDynamicLights.updateTrackedChunks(section, trackedLitChunkPos, newTracked);

            Direction dirX = (Mth.floor(x) & 15) >= 8 ? Direction.EAST : Direction.WEST;
            Direction dirY = (Mth.floor(y) & 15) >= 8 ? Direction.UP : Direction.DOWN;
            Direction dirZ = (Mth.floor(z) & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;
            for (int i = 0; i < 7; i++) {
                if (i % 4 == 0) {
                    section.move(dirX);
                } else if (i % 4 == 1) {
                    section.move(dirZ);
                } else if (i % 4 == 2) {
                    section.move(dirX.getOpposite());
                } else {
                    section.move(dirZ.getOpposite());
                    section.move(dirY);
                }
                SodiumDynamicLights.scheduleChunkRebuild(renderer, section);
                SodiumDynamicLights.updateTrackedChunks(section, trackedLitChunkPos, newTracked);
            }
        }
        // Sections lit before but not now: rebuild so the old light disappears.
        sodiumdynamiclights$scheduleTrackedChunksRebuild(renderer);
        trackedLitChunkPos = newTracked;
        return true;
    }

    @Override
    public void sodiumdynamiclights$scheduleTrackedChunksRebuild(LevelRenderer renderer) {
        if (Minecraft.getInstance().level != level) return;
        for (long pos : trackedLitChunkPos) {
            SodiumDynamicLights.scheduleChunkRebuild(renderer, pos);
        }
    }
}
