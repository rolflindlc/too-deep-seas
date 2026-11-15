package com.rolflin.toodeepseas.data;

import com.rolflin.toodeepseas.core.AccessoryDef;
import com.rolflin.toodeepseas.core.Loadout;
import com.rolflin.toodeepseas.core.Modifier;
import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Equipo guardado en la caña: {@code RodData{Slots:[{id,lvl,uses}], Bait:{id,count}}}.
 * Vive en el NBT del objeto, así que se conserva al mejorar la caña.
 */
public final class RodData {
    public static final String ROOT = "RodData";

    public record Slot(String id, int level, int uses) {
    }

    private RodData() {
    }

    public static List<Slot> slots(ItemStack rod) {
        List<Slot> out = new ArrayList<>();
        CompoundTag root = rod.getTagElement(ROOT);
        if (root == null) return out;
        ListTag list = root.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            out.add(new Slot(t.getString("id"), t.getInt("lvl"), t.getInt("uses")));
        }
        return out;
    }

    public static void setSlots(ItemStack rod, List<Slot> slots) {
        ListTag list = new ListTag();
        for (Slot s : slots) {
            CompoundTag t = new CompoundTag();
            t.putString("id", s.id());
            t.putInt("lvl", s.level());
            t.putInt("uses", s.uses());
            list.add(t);
        }
        rod.getOrCreateTagElement(ROOT).put("Slots", list);
    }

    /** Efectos de los accesorios equipados. Los gastados (0 usos) no aportan nada. */
    public static List<Modifier> modifiers(ItemStack rod) {
        List<Modifier> out = new ArrayList<>();
        for (Slot s : slots(rod)) {
            AccessoryDef def = Defs.ACCESSORIES.get(s.id());
            if (def != null && s.uses() > 0) out.addAll(def.modifiers(s.level()));
        }
        return out;
    }

    /** @return null si se ha equipado; si no, el motivo ("cana", "desconocido", "nivel", "ranuras", "categoria", "repetido"). */
    public static String tryAdd(ItemStack rod, String id, int level) {
        RodTier tier = ModItems.tierOf(rod);
        if (tier == null) return "cana";
        AccessoryDef def = Defs.ACCESSORIES.get(id);
        if (def == null) return "desconocido";
        if (level < 1 || level > def.maxLevel()) return "nivel";
        List<Slot> slots = slots(rod);
        List<Loadout.Equipped> all = new ArrayList<>();
        for (Slot s : slots) {
            AccessoryDef d = Defs.ACCESSORIES.get(s.id());
            if (d != null) all.add(new Loadout.Equipped(s.id(), d.category(), s.level()));
        }
        all.add(new Loadout.Equipped(id, def.category(), level));
        String error = Loadout.validate(tier, all);
        if (error != null) return error;
        slots.add(new Slot(id, level, def.usesAt(level)));
        setSlots(rod, slots);
        return null;
    }

    public static String baitId(ItemStack rod) {
        CompoundTag root = rod.getTagElement(ROOT);
        return root == null ? "" : root.getCompound("Bait").getString("id");
    }

    public static int baitCount(ItemStack rod) {
        CompoundTag root = rod.getTagElement(ROOT);
        return root == null ? 0 : root.getCompound("Bait").getInt("count");
    }

    public static void setBait(ItemStack rod, String id, int count) {
        CompoundTag root = rod.getOrCreateTagElement(ROOT);
        if (count <= 0 || id.isEmpty()) {
            root.remove("Bait");
            return;
        }
        CompoundTag bait = new CompoundTag();
        bait.putString("id", id);
        bait.putInt("count", Math.min(64, count));
        root.put("Bait", bait);
    }
}
