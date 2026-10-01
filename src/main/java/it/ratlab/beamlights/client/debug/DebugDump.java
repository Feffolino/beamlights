package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.client.LightBackend;
import it.ratlab.beamlights.core.Keys;
import it.ratlab.beamlights.core.LightPointPlanner.PlannedPoint;
import it.ratlab.beamlights.core.LightPointPlanner.Status;
import it.ratlab.beamlights.core.math.V3;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** Writes one tick of beams and light points to latest.log, with a one-line chat summary. */
public final class DebugDump {
    private DebugDump() {
    }

    public static void write(List<TracedBeam> frame, LightBackend backend, LocalPlayer player) {
        List<TracedBeam> beams = frame == null ? List.of() : frame;
        BeamLights.LOG.info("[beamlights dump] backend={} ({}) own={} total={} beams={}", backend.name(),
                backend.statusLine(), backend.ownCount(), backend.totalCount(), beams.size());
        int accepted = 0;
        for (TracedBeam tb : beams) {
            BeamLights.LOG.info("[beamlights dump]  owner={} source={} ray={} side={} origin={} dir={} range={} cone={} lum={} hit={} dist={}",
                    tb.owner(), tb.source(), tb.ray(), tb.side(), fmt(tb.beam().origin()), fmt(tb.beam().dir().normalize()),
                    tb.beam().range(), tb.beam().coneDeg(), tb.beam().luminance(), tb.trace().hit(),
                    String.format(Locale.ROOT, "%.2f", tb.trace().distance()));
            for (PlannedPoint p : tb.plan().points()) {
                boolean ok = p.status() == Status.ACCEPTED;
                if (ok) accepted++;
                boolean moved = ok && backend.movedThisTick(Keys.of(tb.entityId(), tb.ray(), p.slot()));
                BeamLights.LOG.info("[beamlights dump]    slot={} pos={} lum={} status={} moved={}",
                        p.slot(), fmt(p.pos()), p.luminance(), p.status(), moved);
            }
        }
        player.displayClientMessage(Component.literal("[Beam Lights] dumped " + beams.size() + " beams, " + accepted
                + " light points to latest.log"), false);
    }

    private static String fmt(V3 v) {
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", v.x(), v.y(), v.z());
    }
}
