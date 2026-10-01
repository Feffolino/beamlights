package it.ratlab.beamlights.client.mask;

import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.api.math.V3;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.MaskPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Gamma mask state shared by the ticker (publishes the local beam each tick) and the renderer (draws it each frame).
 * Client thread only.
 */
public final class GammaMask {
    /** Local player's first beam of the last tick; eyeOffset = beam origin - eye, view = player look at that tick. */
    record LocalBeam(V3 eyeOffset, V3 dir, V3 view, float range, float coneDeg, int luminance, int rgb,
                     int blockLight, int skyLight) {
    }

    private static LocalBeam beam;
    private static boolean failed;
    private static String failure = "";
    private static MaskPolicy.Reason reason = MaskPolicy.Reason.DISABLED;

    private GammaMask() {
    }

    /**
     * True when the mask would draw a local beam this tick (every check but the beam itself): the ticker then keeps
     * only the central ray of the local player as physical light.
     */
    public static boolean wanted() {
        return decide(true) == MaskPolicy.Reason.ACTIVE;
    }

    private static MaskPolicy.Reason decide(boolean hasBeam) {
        Minecraft mc = Minecraft.getInstance();
        return MaskPolicy.decide(BeamClientConfig.GAMMA_MASK.get(), failed, IrisCompat.shaderPackInUse(),
                mc.options.getCameraType().isFirstPerson(), hasBeam);
    }

    /** Called once per tick by the ticker; null = no local beam (or mask not wanted). */
    public static void publish(Level level, Beam b, V3 lightPos, V3 eye, V3 view) {
        if (b == null || b.luminance() <= 0 || level == null) {
            beam = null;
        } else {
            V3 off = b.origin().sub(eye);
            if (off.lengthSq() > 1) off = off.normalize();
            // Engine light only (dynamic lights are not in the light engine): the room's own light at the lit point.
            BlockPos pos = BlockPos.containing(lightPos != null ? lightPos.x() : eye.x(),
                    lightPos != null ? lightPos.y() : eye.y(), lightPos != null ? lightPos.z() : eye.z());
            beam = new LocalBeam(off, b.dir().normalize(), view.normalize(), b.range(), b.coneDeg(), b.luminance(),
                    b.rgb(), level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
        }
        reason = decide(beam != null);
    }

    static LocalBeam beam() {
        return reason == MaskPolicy.Reason.ACTIVE ? beam : null;
    }

    static void fail(String why) {
        failed = true;
        failure = why;
        beam = null;
        reason = MaskPolicy.Reason.FAILED;
    }

    /** Clears a failure (/beamlights reload) so the pass is tried again. */
    public static void resetFailure() {
        failed = false;
        failure = "";
    }

    public static String statusLine() {
        String s = "mask: " + reason.text;
        if (reason == MaskPolicy.Reason.FAILED && !failure.isEmpty()) s += " (" + failure + ")";
        if (reason == MaskPolicy.Reason.ACTIVE) s += String.format(java.util.Locale.ROOT, ", strength %.2f",
                GammaMaskRenderer.currentStrength());
        return s;
    }
}
