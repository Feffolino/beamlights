package it.ratlab.beamlights.compat.omega;

import com.omega.flashlight.Config;
import com.omega.flashlight.client.Flashlight;
import com.omega.flashlight.entity.PlacedFlashlightEntity;
import com.omega.flashlight.item.FlashlightContents;
import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.api.BeamProvider;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.Luminance;
import it.ratlab.beamlights.api.math.V3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Native Omega Flashlight support: same origin, direction, range and cone as the mod's shader beam.
 * Runs on both sides: on the server (spawn blocking) only geometry and "lit" matter, so the client config and the
 * client-only flicker are never touched there.
 */
final class OmegaProvider implements BeamProvider {
    /** Server placeholder: any lit beam counts, the exact level only matters for client light. */
    private static final int SERVER_LUMINANCE = 15;

    @Override
    public String name() {
        return "omegaflashlight";
    }

    @Override
    public boolean mayEmit(Entity entity) {
        return entity instanceof Player || entity instanceof PlacedFlashlightEntity;
    }

    @Override
    public void collect(Entity entity, float partialTick, Consumer<Beam> out) {
        if (entity instanceof Player player) {
            collectPlayer(player, partialTick, out);
        } else if (entity instanceof PlacedFlashlightEntity placed) {
            collectPlaced(placed, out);
        }
    }

    private static void collectPlayer(Player player, float partialTick, Consumer<Beam> out) {
        ItemStack stack = Flashlight.held(player);
        if (stack.isEmpty() || !Flashlight.isOn(player) || !FlashlightContents.hasWorkingSetup(stack)) return;
        int tier = FlashlightContents.lampTier(stack);
        if (tier <= 0) return;

        int mode = Flashlight.mode(player);
        int max = FlashlightContents.maxCharge(stack);
        double charge = max > 0 ? (double) FlashlightContents.totalCharge(stack) / max : Double.NaN;
        int luminance;
        if (player.level().isClientSide()) {
            float flicker = OmegaClientFlicker.localFlicker(player);
            luminance = Luminance.compute(BeamClientConfig.luminanceForTier(tier), BeamClientConfig.SCALE_WITH_BATTERY.get(),
                    charge, BeamClientConfig.BATTERY_MIN_FACTOR.get(), flicker);
        } else {
            luminance = SERVER_LUMINANCE;
        }

        out.accept(new Beam(v3(Flashlight.lightOrigin(player, partialTick)), v3(Flashlight.lightDir(player, partialTick)),
                Flashlight.range(mode, player), Flashlight.outerAngleDeg(mode), luminance, Config.lampTierColor(tier)));
    }

    private static void collectPlaced(PlacedFlashlightEntity placed, Consumer<Beam> out) {
        int mode = placed.getMode();
        float mul = placed.getLampMul();
        int tier = Luminance.tierFromMultiplier(mul, Config.bulbImprovedMultiplier, Config.bulbHighQualityMultiplier);
        int luminance = placed.level().isClientSide()
                ? Luminance.compute(BeamClientConfig.luminanceForTier(tier), false, 1.0, 0.0, 1.0f)
                : SERVER_LUMINANCE;
        out.accept(new Beam(v3(placed.lightOrigin()), v3(placed.lightDir()), Flashlight.range(mode) * mul,
                Flashlight.outerAngleDeg(mode), luminance, placed.getLightColor()));
    }

    private static V3 v3(Vec3 v) {
        return new V3(v.x, v.y, v.z);
    }
}
