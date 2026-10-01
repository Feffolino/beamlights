package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.api.BeamItemData;
import it.ratlab.beamlights.api.BeamProvider;
import it.ratlab.beamlights.api.math.V3;
import it.ratlab.beamlights.compat.curios.CuriosSlots;
import it.ratlab.beamlights.core.BeamDefinition.Slot;
import it.ratlab.beamlights.data.BeamComponents;
import it.ratlab.beamlights.data.BeamDefinitions;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.function.Consumer;

/**
 * Beams from the beamlights:beam item component (checked first) and from datapack definitions
 * (data/<ns>/beamlights/beams). Players: hands, head and Curios; other living entities (armor stands, mobs): hands and
 * head only. At most two beams per entity.
 */
public final class DataBeamProvider implements BeamProvider {
    private static final int MAX_BEAMS = 2;

    private final boolean curios = ModList.get().isLoaded("curios");
    // Omega items are handled natively by the Omega provider; skipping them avoids double beams.
    private final boolean skipOmega = ModList.get().isLoaded("omegaflashlight");

    @Override
    public String name() {
        return "data";
    }

    @Override
    public boolean mayEmit(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        BeamDefinitions defs = BeamDefinitions.of(entity.level());
        DataComponentType<BeamItemData> type = BeamComponents.BEAM.get();
        if (eligible(defs, type, living.getMainHandItem()) || eligible(defs, type, living.getOffhandItem())
                || eligible(defs, type, living.getItemBySlot(EquipmentSlot.HEAD))) return true;
        if (!curios || !(entity instanceof Player)) return false;
        // Datapack curios definitions: let collect scan; component-only: scan here (players only, few per level).
        return defs.usesCurios() || CuriosSlots.any(living, s -> s.has(type) && !omega(s));
    }

    @Override
    public void collect(Entity entity, float partialTick, Consumer<Beam> out) {
        if (!(entity instanceof LivingEntity living)) return;
        BeamDefinitions defs = BeamDefinitions.of(entity.level());
        DataComponentType<BeamItemData> type = BeamComponents.BEAM.get();
        Vec3 eye = living.getEyePosition(partialTick);
        Vec3 look = living.getViewVector(partialTick);
        V3 eyeV = new V3(eye.x, eye.y, eye.z);
        V3 lookV = new V3(look.x, look.y, look.z);

        int n = emit(defs, type, living, living.getMainHandItem(), Slot.MAINHAND, eyeV, lookV, out, 0);
        n = emit(defs, type, living, living.getOffhandItem(), Slot.OFFHAND, eyeV, lookV, out, n);
        n = emit(defs, type, living, living.getItemBySlot(EquipmentSlot.HEAD), Slot.HEAD, eyeV, lookV, out, n);
        if (n >= MAX_BEAMS || !curios || !(entity instanceof Player)) return;
        for (ItemStack stack : CuriosSlots.find(living, s -> eligible(defs, type, s))) {
            n = emit(defs, type, living, stack, Slot.CURIOS, eyeV, lookV, out, n);
            if (n >= MAX_BEAMS) return;
        }
    }

    private int emit(BeamDefinitions defs, DataComponentType<BeamItemData> type, LivingEntity living, ItemStack stack,
                     Slot slot, V3 eye, V3 look, Consumer<Beam> out, int count) {
        if (count >= MAX_BEAMS || !eligible(defs, type, stack)) return count;
        // The item component wins over datapack definitions in the slots it lists.
        BeamItemData c = stack.get(type);
        if (c != null && c.allows(slot.id())) {
            out.accept(new Beam(BeamDefinition.origin(eye, look, c.forward(), c.down()), look, c.range(), c.cone(),
                    c.luminance(), c.color()));
            return count + 1;
        }
        if (defs.isEmpty()) return count;
        BeamDefinitions.Entry e = defs.best(stack, slot, living.registryAccess());
        if (e == null) return count;
        BeamDefinition d = e.def();
        out.accept(new Beam(d.origin(eye, look), look, d.range(), d.cone(), d.luminance(), d.rgb()));
        return count + 1;
    }

    private boolean eligible(BeamDefinitions defs, DataComponentType<BeamItemData> type, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.has(type) && (defs.isEmpty() || !defs.hasCandidates(stack.getItem()))) return false;
        return !omega(stack);
    }

    private boolean omega(ItemStack stack) {
        return skipOmega && "omegaflashlight".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }
}
