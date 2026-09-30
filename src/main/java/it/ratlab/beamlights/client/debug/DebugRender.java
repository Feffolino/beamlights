package it.ratlab.beamlights.client.debug;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.core.LightPointPlanner.PlannedPoint;
import it.ratlab.beamlights.core.LightPointPlanner.Status;
import it.ratlab.beamlights.core.math.V3;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/** World debug view of the last traced frame. */
@EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
public final class DebugRender {
    private static final double SDL_RADIUS = 7.75;
    private static final int CIRCLE_SEGMENTS = 32;
    // Private buffer so labels never flush the shared buffer source.
    private static final MultiBufferSource.BufferSource LABELS = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

    private DebugRender() {
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !DebugState.render()) return;
        List<TracedBeam> frame = DebugState.lastFrame();
        if (frame.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Vector3f lookF = camera.getLookVector();
        V3 look = new V3(lookF.x(), lookF.y(), lookF.z());
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        PlannedPoint selected = null;
        double bestDot = 0.9;
        for (TracedBeam tb : frame) {
            V3 origin = tb.beam().origin();
            V3 end = tb.trace().point();
            line(ps, lines, cam, origin, end, 1f, 1f, 1f, 1f);
            if (tb.trace().hit()) {
                V3 rangeEnd = origin.add(tb.beam().dir().normalize().scale(tb.beam().range()));
                line(ps, lines, cam, end, rangeEnd, 1f, 0.2f, 0.2f, 0.6f);
            }
            for (PlannedPoint p : tb.plan().points()) {
                switch (p.status()) {
                    case ACCEPTED -> {
                        float k = 0.35f + 0.65f * p.luminance() / 15f;
                        if (p.slot() == 0) box(ps, lines, cam, p.pos(), 0.2, k, k, 0f, 1f);
                        else box(ps, lines, cam, p.pos(), 0.15, 0f, k, k, 1f);
                        V3 toPoint = p.pos().sub(new V3(cam.x, cam.y, cam.z)).normalize();
                        double dot = toPoint.dot(look);
                        if (dot > bestDot) {
                            bestDot = dot;
                            selected = p;
                        }
                    }
                    case SKIPPED_AIR -> cross(ps, lines, cam, p.pos(), 0.15, 0.6f, 0.6f, 0.6f, 1f);
                    case MERGED -> box(ps, lines, cam, p.pos(), 0.12, 0.5f, 0.6f, 1f, 0.35f);
                    case CAPPED -> box(ps, lines, cam, p.pos(), 0.08, 1f, 0.2f, 0.2f, 1f);
                }
            }
        }
        if (selected != null) sphere(ps, lines, cam, selected.pos(), SDL_RADIUS, 1f, 0.9f, 0.3f, 0.5f);
        buffers.endBatch(RenderType.lines());

        Font font = mc.font;
        for (TracedBeam tb : frame) {
            for (PlannedPoint p : tb.plan().points()) {
                if (p.status() == Status.ACCEPTED) label(ps, LABELS, font, camera, cam, p.pos(), String.valueOf(p.luminance()));
            }
        }
        LABELS.endBatch();
    }

    private static void line(PoseStack ps, VertexConsumer vc, Vec3 cam, V3 a, V3 b, float r, float g, float bl, float al) {
        float x1 = (float) (a.x() - cam.x), y1 = (float) (a.y() - cam.y), z1 = (float) (a.z() - cam.z);
        float x2 = (float) (b.x() - cam.x), y2 = (float) (b.y() - cam.y), z2 = (float) (b.z() - cam.z);
        float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1;
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) return;
        nx /= len;
        ny /= len;
        nz /= len;
        PoseStack.Pose pose = ps.last();
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, bl, al).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, bl, al).setNormal(pose, nx, ny, nz);
    }

    private static void box(PoseStack ps, VertexConsumer vc, Vec3 cam, V3 c, double h, float r, float g, float b, float a) {
        double x = c.x() - cam.x, y = c.y() - cam.y, z = c.z() - cam.z;
        LevelRenderer.renderLineBox(ps, vc, x - h, y - h, z - h, x + h, y + h, z + h, r, g, b, a);
    }

    private static void cross(PoseStack ps, VertexConsumer vc, Vec3 cam, V3 c, double h, float r, float g, float b, float a) {
        line(ps, vc, cam, c.add(new V3(-h, -h, 0)), c.add(new V3(h, h, 0)), r, g, b, a);
        line(ps, vc, cam, c.add(new V3(-h, h, 0)), c.add(new V3(h, -h, 0)), r, g, b, a);
        line(ps, vc, cam, c.add(new V3(0, -h, -h)), c.add(new V3(0, h, h)), r, g, b, a);
        line(ps, vc, cam, c.add(new V3(0, h, -h)), c.add(new V3(0, -h, h)), r, g, b, a);
    }

    private static void sphere(PoseStack ps, VertexConsumer vc, Vec3 cam, V3 c, double radius,
                               float r, float g, float b, float a) {
        for (int plane = 0; plane < 3; plane++) {
            V3 prev = null;
            for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                double ang = Math.PI * 2 * i / CIRCLE_SEGMENTS;
                double u = Math.cos(ang) * radius, v = Math.sin(ang) * radius;
                V3 p = switch (plane) {
                    case 0 -> c.add(new V3(u, v, 0));
                    case 1 -> c.add(new V3(u, 0, v));
                    default -> c.add(new V3(0, u, v));
                };
                if (prev != null) line(ps, vc, cam, prev, p, r, g, b, a);
                prev = p;
            }
        }
    }

    private static void label(PoseStack ps, MultiBufferSource buffers, Font font, Camera camera, Vec3 cam, V3 pos,
                              String text) {
        ps.pushPose();
        ps.translate(pos.x() - cam.x, pos.y() - cam.y + 0.35, pos.z() - cam.z);
        ps.mulPose(camera.rotation());
        ps.scale(0.025f, -0.025f, 0.025f);
        Matrix4f matrix = ps.last().pose();
        float x = -font.width(text) / 2f;
        font.drawInBatch(text, x, 0, 0xFFFFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000,
                LightTexture.FULL_BRIGHT);
        ps.popPose();
    }
}
