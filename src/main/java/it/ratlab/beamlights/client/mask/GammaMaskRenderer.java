package it.ratlab.beamlights.client.mask;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.api.math.V3;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.MaskMath;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;

/**
 * Draws the gamma mask after the level (before the hand): copies colour and depth of the main target, then a fullscreen
 * pass adds light inside the beam cone. Render thread only. Any failure disables the mask (GammaMask.fail) and the ticker
 * falls back to the normal physical lights.
 */
public final class GammaMaskRenderer {
    /** Within this angle of the player's view the beam follows the camera every frame (hand-held). */
    private static final double FOLLOW_VIEW_COS = Math.cos(Math.toRadians(12));

    private static ShaderInstance shader;
    private static TextureTarget copy;
    private static double strength;
    private static long lastNanos;
    /** Last drawn beam, kept while fading out. */
    private static GammaMask.LocalBeam fading;

    private GammaMaskRenderer() {
    }

    static double currentStrength() {
        return strength;
    }

    @EventBusSubscriber(modid = BeamLights.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        static void onRegisterShaders(RegisterShadersEvent event) {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BeamLights.MOD_ID, "gamma_mask"),
                        DefaultVertexFormat.POSITION), s -> shader = s);
            } catch (IOException e) {
                BeamLights.LOG.error("Beam Lights: gamma mask shader failed to load", e);
                GammaMask.fail("shader not loaded");
            }
        }
    }

    @EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
    static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        static void onStage(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
            try {
                render(event);
            } catch (Throwable t) {
                BeamLights.LOG.error("Beam Lights: gamma mask pass failed, mask disabled (/beamlights reload retries)",
                        t);
                GammaMask.fail(t.getClass().getSimpleName());
                strength = 0;
                fading = null;
            }
        }
    }

    private static void render(RenderLevelStageEvent event) {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.25, (now - lastNanos) / 1e9);
        lastNanos = now;

        GammaMask.LocalBeam beam = GammaMask.beam();
        double target = beam == null ? 0
                : BeamClientConfig.MASK_STRENGTH.get() * Math.min(15, beam.luminance()) / 15.0 * ambientFactor();
        strength = MaskMath.approach(strength, target, dt, BeamClientConfig.MASK_FADE_SECONDS.get());
        if (beam != null) fading = beam;
        if (strength <= 0.001 || fading == null) {
            if (strength <= 0.001) fading = null;
            return;
        }
        if (shader == null) {
            GammaMask.fail("shader not loaded");
            return;
        }
        draw(event, fading);
    }

    /** 1 in the dark, lower when the camera stands in block or sky light (light engine only, no dynamic lights). */
    private static double ambientFactor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 1;
        BlockPos pos = BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition());
        int block = mc.level.getBrightness(LightLayer.BLOCK, pos);
        int sky = mc.level.getBrightness(LightLayer.SKY, pos) - mc.level.getSkyDarken();
        double ambient = Math.max(block, Math.max(0, sky)) / 15.0;
        return 1 - BeamClientConfig.MASK_AMBIENT_FADE.get() * MaskMath.smoothstep(0.2, 1.0, ambient);
    }

    private static void draw(RenderLevelStageEvent event, GammaMask.LocalBeam b) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        int w = main.width, h = main.height;
        if (copy == null) {
            copy = new TextureTarget(w, h, true, Minecraft.ON_OSX);
        } else if (copy.width != w || copy.height != h) {
            copy.resize(w, h, Minecraft.ON_OSX);
        }
        if (main.isStencilEnabled() && !copy.isStencilEnabled()) copy.enableStencil();

        // Copy colour + depth (GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, GL_NEAREST).
        GlStateManager._glBindFramebuffer(0x8CA8, main.frameBufferId);
        GlStateManager._glBindFramebuffer(0x8CA9, copy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h, 0x4000 | 0x100, 0x2600);
        main.bindWrite(true);

        // Direction: follow the camera while the beam points where the player looks.
        V3 dir = b.dir();
        if (dir.dot(b.view()) >= FOLLOW_VIEW_COS) {
            Vector3f look = event.getCamera().getLookVector();
            dir = new V3(look.x(), look.y(), look.z()).normalize();
        }
        V3 origin = b.eyeOffset();

        Matrix4f invViewProj = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
        double coneDeg = b.coneDeg();
        double angleScale = BeamClientConfig.MASK_ANGLE_SCALE.get();
        int rgb = b.rgb();
        double tint = BeamClientConfig.MASK_TINT.get();
        float tr = (float) (1 - tint + tint * ((rgb >> 16) & 0xFF) / 255.0);
        float tg = (float) (1 - tint + tint * ((rgb >> 8) & 0xFF) / 255.0);
        float tb = (float) (1 - tint + tint * (rgb & 0xFF) / 255.0);

        ShaderInstance s = shader;
        s.setSampler("ColorSampler", copy.getColorTextureId());
        s.setSampler("DepthSampler", copy.getDepthTextureId());
        s.safeGetUniform("InvViewProj").set(invViewProj);
        s.safeGetUniform("BeamOrigin").set((float) origin.x(), (float) origin.y(), (float) origin.z());
        s.safeGetUniform("BeamDir").set((float) dir.x(), (float) dir.y(), (float) dir.z());
        s.safeGetUniform("CosOuter").set((float) MaskMath.cosOuter(coneDeg, angleScale));
        s.safeGetUniform("CosInner").set((float) MaskMath.cosInner(coneDeg, angleScale,
                BeamClientConfig.MASK_SOFTNESS.get()));
        s.safeGetUniform("Range").set((float) (b.range() * BeamClientConfig.MASK_RANGE_SCALE.get()));
        s.safeGetUniform("Falloff").set(BeamClientConfig.MASK_FALLOFF.get().floatValue());
        s.safeGetUniform("Strength").set((float) strength);
        s.safeGetUniform("Gain").set(BeamClientConfig.MASK_GAIN.get().floatValue());
        s.safeGetUniform("Shading").set(BeamClientConfig.MASK_SHADING.get().floatValue());
        s.safeGetUniform("Knee").set(BeamClientConfig.MASK_KNEE.get().floatValue());
        s.safeGetUniform("BrightCutoff").set(BeamClientConfig.MASK_BRIGHT_CUTOFF.get().floatValue());
        s.safeGetUniform("BlackLift").set(BeamClientConfig.MASK_BLACK_LIFT.get().floatValue());
        s.safeGetUniform("Tint").set(tr, tg, tb);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> s);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.addVertex(-1, -1, 0);
        bb.addVertex(1, -1, 0);
        bb.addVertex(1, 1, 0);
        bb.addVertex(-1, 1, 0);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
