package it.ratlab.beamlights.client.sdl;

import it.ratlab.beamlights.client.LightBackend;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.SourceMotion;
import it.ratlab.beamlights.api.math.V3;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import toni.sodiumdynamiclights.SodiumDynamicLights;

/** One pooled BeamLightSource per stable key; sources move only past the move threshold. */
public final class SdlBackend implements LightBackend {
    private final SodiumDynamicLights sdl;
    private final Long2ObjectOpenHashMap<BeamLightSource> sources = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet seen = new LongOpenHashSet();
    private final LongOpenHashSet moved = new LongOpenHashSet();

    private SdlBackend(SodiumDynamicLights sdl) {
        this.sdl = sdl;
    }

    public static SdlBackend create() {
        SodiumDynamicLights sdl = SodiumDynamicLights.get();
        if (sdl == null) throw new IllegalStateException("SodiumDynamicLights.get() returned null");
        return new SdlBackend(sdl);
    }

    @Override
    public String name() {
        return "sodiumdynamiclights";
    }

    @Override
    public void begin() {
        seen.clear();
        moved.clear();
    }

    @Override
    public void put(long key, V3 pos, int luminance) {
        seen.add(key);
        Level level = Minecraft.getInstance().level;
        BeamLightSource src = sources.get(key);
        if (src == null || src.level() != level) {
            if (src != null) src.remove();
            src = new BeamLightSource(level);
            sources.put(key, src);
        }
        V3 current = src.hasPosition() ? src.position() : null;
        if (SourceMotion.shouldMove(current, pos, src.luminance(), luminance, BeamClientConfig.MOVE_THRESHOLD.get())) {
            src.set(pos, luminance);
            moved.add(key);
        }
        SodiumDynamicLights.updateTracking(src);
    }

    @Override
    public void end() {
        ObjectIterator<Long2ObjectMap.Entry<BeamLightSource>> it = sources.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            Long2ObjectMap.Entry<BeamLightSource> e = it.next();
            if (!seen.contains(e.getLongKey())) {
                e.getValue().remove();
                it.remove();
            }
        }
    }

    @Override
    public void clear() {
        for (BeamLightSource s : sources.values()) s.remove();
        sources.clear();
        seen.clear();
        moved.clear();
    }

    @Override public int ownCount() { return sources.size(); }
    @Override public int totalCount() { return sdl.getLightSourcesCount(); }
    @Override public int movesThisTick() { return moved.size(); }
    @Override public boolean movedThisTick(long key) { return moved.contains(key); }

    @Override
    public String statusLine() {
        return "SDL mode " + sdl.config.getDynamicLightsMode();
    }
}
