package it.ratlab.beamlights.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/** Listed in kubejs.plugins.txt; KubeJS only reads that file when KubeJS itself is installed. */
public class BeamLightsKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("BeamLights", new BeamLightsBindingJS());
    }
}
