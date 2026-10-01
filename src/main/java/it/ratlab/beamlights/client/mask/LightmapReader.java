package it.ratlab.beamlights.client.mask;

import com.mojang.blaze3d.platform.NativeImage;
import it.ratlab.beamlights.BeamLights;
import net.minecraft.client.renderer.LightTexture;

import java.lang.reflect.Field;

/**
 * Reads the CPU copy of the vanilla lightmap (16x16, x = block light, y = sky light, already including brightness
 * setting, dimension and time of day). Reflection on the private field: NeoForge runs with Mojang names. Render thread.
 */
final class LightmapReader {
    private static boolean resolved;
    private static Field pixels;

    private LightmapReader() {
    }

    /** Largest RGB channel 0..1 of the lightmap, bilinear at fractional light levels; 1 when unreadable. */
    static double maxChannel(LightTexture lt, double block, double sky) {
        NativeImage img = image(lt);
        if (img == null) return 1;
        double bx = clamp(block), sy = clamp(sky);
        int x0 = (int) Math.floor(bx), y0 = (int) Math.floor(sy);
        int x1 = Math.min(15, x0 + 1), y1 = Math.min(15, y0 + 1);
        double fx = bx - x0, fy = sy - y0;
        double top = lerp(max(img, x0, y0), max(img, x1, y0), fx);
        double bottom = lerp(max(img, x0, y1), max(img, x1, y1), fx);
        return lerp(top, bottom, fy);
    }

    private static double max(NativeImage img, int x, int y) {
        int abgr = img.getPixelRGBA(x, y);
        int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
        return Math.max(r, Math.max(g, b)) / 255.0;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(15, v));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static NativeImage image(LightTexture lt) {
        if (!resolved) {
            resolved = true;
            try {
                pixels = LightTexture.class.getDeclaredField("lightPixels");
                pixels.setAccessible(true);
            } catch (Throwable t) {
                BeamLights.LOG.warn("Beam Lights: lightmap not readable, gamma mask uses lightmap 1 (no boost)", t);
            }
        }
        if (pixels == null) return null;
        try {
            return (NativeImage) pixels.get(lt);
        } catch (Throwable t) {
            pixels = null;
            return null;
        }
    }
}
