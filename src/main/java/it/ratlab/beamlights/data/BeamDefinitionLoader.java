package it.ratlab.beamlights.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Server datapack reload of data/<ns>/beamlights/beams/*.json into BeamDefinitions.SERVER. */
public final class BeamDefinitionLoader extends SimpleJsonResourceReloadListener {
    public static final String FOLDER = "beamlights/beams";

    public BeamDefinitionLoader() {
        super(new Gson(), FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        BeamDefinitions.SERVER.loadJson(files);
    }
}
