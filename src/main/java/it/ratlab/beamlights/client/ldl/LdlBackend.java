package it.ratlab.beamlights.client.ldl;

import dev.lambdaurora.lambdynlights.LambDynLights;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import it.ratlab.beamlights.client.GatedLightBackend;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.ConeLight;
import it.ratlab.beamlights.core.SourceMotion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.Level;

/**
 * LambDynamicLights backend. Every light point is a custom DynamicLightBehavior (LDL API, added through
 * DynamicLightBehaviorManager); LDL computes the light per block and rebuilds the affected sections itself. Optional
 * cone lights along the central beams (config ldlConeLight). Gating and move budget are shared with the SDL backend
 * (GatedLightBackend); cone changes use the same gate and budget. While LDL's mode is OFF every light is removed.
 */
public final class LdlBackend extends GatedLightBackend<LdlPointLight> {
    /** LDL passes this falloff ratio to lightAtPos (15 / 7.75); used for bounding boxes. */
    static final double FALLOFF = ConeLight.LDL_FALLOFF;

    private final LambDynLights ldl;
    private final DynamicLightBehaviorManager manager;
    private final Long2ObjectOpenHashMap<LdlConeLight> cones = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet seenCones = new LongOpenHashSet();
    private final LongArrayList unseen = new LongArrayList();
    private int coneMoves;

    private LdlBackend(LambDynLights ldl, DynamicLightBehaviorManager manager) {
        this.ldl = ldl;
        this.manager = manager;
    }

    public static LdlBackend create() {
        LambDynLights ldl = LambDynLights.get();
        if (ldl == null) throw new IllegalStateException("LambDynLights.get() returned null");
        DynamicLightBehaviorManager manager = ldl.dynamicLightBehaviorManager();
        if (manager == null) throw new IllegalStateException("LambDynamicLights behavior manager not initialized");
        return new LdlBackend(ldl, manager);
    }

    @Override
    public String name() {
        return "lambdynamiclights";
    }

    @Override
    protected boolean active() {
        return ldl.config.getDynamicLightsMode().isEnabled();
    }

    @Override
    protected LdlPointLight newSource(Level level) {
        return new LdlPointLight(manager, level);
    }

    @Override
    public void begin(it.ratlab.beamlights.api.math.V3 viewer, int moveBudget) {
        super.begin(viewer, moveBudget);
        seenCones.clear();
        coneMoves = 0;
        if (!active() && !cones.isEmpty()) clearCones();
    }

    @Override
    public boolean wantsBeams() {
        return BeamClientConfig.LDL_CONE_LIGHT.get() && active();
    }

    @Override
    public void putBeam(long key, ConeLight.Shape cone, boolean priority, boolean exact) {
        if (!wantsBeams()) return;
        seenCones.add(key);
        LdlConeLight c = cones.get(key);
        ConeLight.Look look = BeamClientConfig.coneLook();
        double displacement;
        if (c != null) {
            ConeLight.Shape old = c.shape();
            double d = ConeLight.displacementSq(old, cone);
            if (!SourceMotion.shouldChange(d, old.luminance(), cone.luminance(), true, gate(exact))
                    && Math.abs(old.halfAngleDeg() - cone.halfAngleDeg()) < 1.0 && look.equals(c.look())) return;
            int dl = cone.luminance() - old.luminance();
            displacement = d + dl * dl;
        } else {
            displacement = APPEAR_DISPLACEMENT + cone.luminance();
        }
        offerExtra(key, priority, viewer().distSq(cone.origin()), displacement, () -> applyCone(key, cone, look));
    }

    private void applyCone(long key, ConeLight.Shape cone, ConeLight.Look look) {
        LdlConeLight c = cones.get(key);
        if (c == null) {
            cones.put(key, new LdlConeLight(manager, cone, look));
        } else {
            c.update(cone, look);
        }
        coneMoves++;
    }

    @Override
    protected void beforeSchedule() {
        unseen.clear();
        for (Long2ObjectMap.Entry<LdlConeLight> e : cones.long2ObjectEntrySet()) {
            if (!seenCones.contains(e.getLongKey())) unseen.add(e.getLongKey());
        }
        for (int i = 0; i < unseen.size(); i++) {
            long key = unseen.getLong(i);
            offerExtra(key, false, viewer().distSq(cones.get(key).shape().origin()), APPEAR_DISPLACEMENT, () -> {
                LdlConeLight c = cones.remove(key);
                if (c != null) c.remove();
                coneMoves++;
            });
        }
    }

    private void clearCones() {
        for (LdlConeLight c : cones.values()) c.remove();
        cones.clear();
    }

    @Override
    public void clear() {
        super.clear();
        clearCones();
        seenCones.clear();
        coneMoves = 0;
    }

    @Override public int ownCount() { return super.ownCount() + cones.size(); }
    @Override public int totalCount() { return ldl.getLightSourcesCount(); }
    @Override public int movesThisTick() { return super.movesThisTick() + coneMoves; }

    @Override
    public String statusLine() {
        return "LDL mode " + ldl.config.getDynamicLightsMode() + (cones.isEmpty() ? "" : ", " + cones.size() + " cones");
    }
}
