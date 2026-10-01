package it.ratlab.beamlights.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.function.Predicate;

/** Curios lookup; only class-loaded after ModList.isLoaded("curios"). */
public final class CuriosSlots {
    private CuriosSlots() {
    }

    /** Stacks in any Curios slot that pass the filter (empty list without a Curios inventory). */
    public static List<ItemStack> find(LivingEntity entity, Predicate<ItemStack> filter) {
        return CuriosApi.getCuriosInventory(entity)
                .map(h -> h.findCurios(filter).stream().map(SlotResult::stack).toList())
                .orElse(List.of());
    }
}
