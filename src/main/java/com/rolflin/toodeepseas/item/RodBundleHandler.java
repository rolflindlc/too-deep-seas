package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.data.RodData;
import com.rolflin.toodeepseas.menu.MesaPescadorMenu;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Atajo de inventario, como el saco: clic derecho con un accesorio o un cebo sobre la caña para meterlo;
 * clic derecho con la caña en el cursor sobre una casilla vacía para sacar lo último que se metió.
 * Va por evento y no en la clase de la caña para que también funcione con la caña vanilla (tier 0).
 */
@Mod.EventBusSubscriber(modid = TooDeepSeas.MODID)
public final class RodBundleHandler {
    private RodBundleHandler() {
    }

    @SubscribeEvent
    public static void onStacked(ItemStackedOnOtherEvent event) {
        if (event.getClickAction() != ClickAction.SECONDARY) return;
        Player player = event.getPlayer();
        Slot slot = event.getSlot();
        AbstractContainerMenu menu = player.containerMenu;
        // En la mesa del pescador, las ranuras de equipo ya hacen este trabajo
        if (menu instanceof MesaPescadorMenu && slot.index < MesaPescadorMenu.GEAR_END) return;
        if (!slot.allowModification(player)) return;

        // Se leen del menú y de la casilla, no del evento, para no depender de cuál llama «llevado» a cuál
        ItemStack carried = menu.getCarried();
        ItemStack inSlot = slot.getItem();

        if (!carried.isEmpty() && inSlot.getCount() == 1 && ModItems.tierOf(inSlot) != null) {
            if (carried.getItem() instanceof AccessoryItem acc) {
                if (RodData.tryAdd(inSlot, acc.id(), AccessoryItem.level(carried), acc.uses(carried)) == null) {
                    carried.shrink(1);
                    sound(player, SoundEvents.BUNDLE_INSERT);
                }
                event.setCanceled(true);
            } else if (carried.getItem() instanceof BaitItem bait) {
                int added = RodData.addBait(inSlot, bait.id(), carried.getCount());
                if (added > 0) {
                    carried.shrink(added);
                    sound(player, SoundEvents.BUNDLE_INSERT);
                }
                event.setCanceled(true);
            }
        } else if (inSlot.isEmpty() && carried.getCount() == 1 && ModItems.tierOf(carried) != null) {
            if (takeOutLast(carried, slot)) {
                sound(player, SoundEvents.BUNDLE_REMOVE_ONE);
                event.setCanceled(true);
            }
        }
    }

    /** Saca a la casilla el último accesorio de la caña o, si no lleva ninguno, su último cebo. */
    private static boolean takeOutLast(ItemStack rod, Slot slot) {
        List<RodData.Slot> accessories = RodData.slots(rod);
        for (int i = accessories.size() - 1; i >= 0; i--) {
            RodData.Slot s = accessories.get(i);
            ItemStack out = ModItems.accessoryStack(s.id(), s.level(), s.uses());
            if (out.isEmpty()) continue; // accesorio sin objeto: se queda en la caña
            if (!slot.safeInsert(out).isEmpty()) return false;
            accessories.remove(i);
            RodData.setSlots(rod, accessories);
            return true;
        }
        List<RodData.Bait> baits = RodData.baits(rod);
        for (int i = baits.size() - 1; i >= 0; i--) {
            RodData.Bait b = baits.get(i);
            ItemStack out = ModItems.baitStack(b.id(), b.count());
            if (out.isEmpty()) continue;
            int left = slot.safeInsert(out).getCount();
            if (left >= b.count()) return false;
            if (left > 0) baits.set(i, new RodData.Bait(b.id(), left));
            else baits.remove(i);
            RodData.setBaits(rod, baits);
            return true;
        }
        return false;
    }

    private static void sound(Player player, SoundEvent sound) {
        player.playSound(sound, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }
}
