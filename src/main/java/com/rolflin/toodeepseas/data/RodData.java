package com.rolflin.toodeepseas.data;

import com.rolflin.toodeepseas.core.AccessoryDef;
import com.rolflin.toodeepseas.core.FishingMedium;
import com.rolflin.toodeepseas.core.Loadout;
import com.rolflin.toodeepseas.core.Modifier;
import com.rolflin.toodeepseas.core.RodStats;
import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Equipo guardado en la caña: {@code RodData{Slots:[{id,lvl,uses}], Baits:[{id,count}]}}.
 * Vive en el NBT del objeto, así que se conserva al mejorar la caña.
 * (Las cañas de la fase 3 guardaban un solo cebo en {@code Bait:{id,count}}; se sigue leyendo.)
 */
public final class RodData {
    public static final String ROOT = "RodData";
    public static final int MAX_BAIT = 64;

    public record Slot(String id, int level, int uses) {
    }

    public record Bait(String id, int count) {
    }

    private RodData() {
    }

    // ---------- Accesorios ----------

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
        CompoundTag root = rod.getOrCreateTagElement(ROOT);
        if (slots.isEmpty()) {
            root.remove("Slots");
        } else {
            ListTag list = new ListTag();
            for (Slot s : slots) {
                CompoundTag t = new CompoundTag();
                t.putString("id", s.id());
                t.putInt("lvl", s.level());
                t.putInt("uses", s.uses());
                list.add(t);
            }
            root.put("Slots", list);
        }
        cleanup(rod);
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

    /**
     * ¿Cabe este accesorio junto a los que ya hay?
     *
     * @return null si cabe; si no, el motivo ("desconocido", "nivel", "tier", "ranuras", "categoria", "repetido").
     */
    @Nullable
    public static String validateAdd(RodTier tier, List<Slot> current, String id, int level) {
        AccessoryDef def = Defs.ACCESSORIES.get(id);
        if (def == null) return "desconocido";
        if (level < 1 || level > def.maxLevel()) return "nivel";
        if (tier.tier < def.minTier()) return "tier";
        List<Loadout.Equipped> all = new ArrayList<>();
        for (Slot s : current) {
            AccessoryDef d = Defs.ACCESSORIES.get(s.id());
            // Un accesorio cuya definición ya no existe sigue ocupando su ranura
            all.add(new Loadout.Equipped(s.id(), d != null ? d.category() : Loadout.Category.AMULETO, Math.max(1, Math.min(s.level(), tier.maxAccessoryLevel()))));
        }
        all.add(new Loadout.Equipped(id, def.category(), level));
        return Loadout.validate(tier, all);
    }

    /** Equipa un accesorio nuevo, con todos sus usos. */
    @Nullable
    public static String tryAdd(ItemStack rod, String id, int level) {
        return tryAdd(rod, id, level, -1);
    }

    /**
     * @param uses usos que le quedan al accesorio; negativo = todos
     * @return null si se ha equipado; si no, el motivo ("cana" o los de {@link #validateAdd}).
     */
    @Nullable
    public static String tryAdd(ItemStack rod, String id, int level, int uses) {
        RodTier tier = ModItems.tierOf(rod);
        if (tier == null) return "cana";
        List<Slot> slots = slots(rod);
        String error = validateAdd(tier, slots, id, level);
        if (error != null) return error;
        int max = Defs.ACCESSORIES.get(id).usesAt(level);
        slots.add(new Slot(id, level, uses < 0 ? max : Math.min(uses, max)));
        setSlots(rod, slots);
        return null;
    }

    // ---------- Cebos ----------

    public static List<Bait> baits(ItemStack rod) {
        List<Bait> out = new ArrayList<>();
        CompoundTag root = rod.getTagElement(ROOT);
        if (root == null) return out;
        if (root.contains("Baits", Tag.TAG_LIST)) {
            ListTag list = root.getList("Baits", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) addBait(out, list.getCompound(i));
        } else if (root.contains("Bait", Tag.TAG_COMPOUND)) {
            addBait(out, root.getCompound("Bait"));
        }
        return out;
    }

    private static void addBait(List<Bait> out, CompoundTag t) {
        String id = t.getString("id");
        int count = t.getInt("count");
        if (!id.isEmpty() && count > 0) out.add(new Bait(id, Math.min(MAX_BAIT, count)));
    }

    public static void setBaits(ItemStack rod, List<Bait> baits) {
        CompoundTag root = rod.getOrCreateTagElement(ROOT);
        root.remove("Bait");
        ListTag list = new ListTag();
        for (Bait b : baits) {
            if (b.id().isEmpty() || b.count() <= 0) continue;
            CompoundTag t = new CompoundTag();
            t.putString("id", b.id());
            t.putInt("count", Math.min(MAX_BAIT, b.count()));
            list.add(t);
        }
        if (list.isEmpty()) root.remove("Baits");
        else root.put("Baits", list);
        cleanup(rod);
    }

    /**
     * Mete cebo en la caña: primero en la ranura que ya tenga ese cebo y, si no, en una libre.
     *
     * @return cuántas unidades han cabido
     */
    public static int addBait(ItemStack rod, String id, int count) {
        RodTier tier = ModItems.tierOf(rod);
        if (tier == null || count <= 0 || Defs.BAITS.get(id) == null) return 0;
        List<Bait> baits = baits(rod);
        for (int i = 0; i < baits.size(); i++) {
            Bait b = baits.get(i);
            if (b.id().equals(id)) {
                int add = Math.min(count, MAX_BAIT - b.count());
                if (add <= 0) return 0;
                baits.set(i, new Bait(id, b.count() + add));
                setBaits(rod, baits);
                return add;
            }
        }
        if (baits.size() >= tier.baitSlots) return 0;
        int add = Math.min(count, MAX_BAIT);
        baits.add(new Bait(id, add));
        setBaits(rod, baits);
        return add;
    }

    /** Sustituye todo el cebo por uno solo (comando de pruebas). */
    public static void setBait(ItemStack rod, String id, int count) {
        setBaits(rod, count <= 0 || id.isEmpty() ? List.of() : List.of(new Bait(id, count)));
    }

    // ---------- Estadísticas ----------

    /** Parámetros del minijuego para esta caña tal como está equipada, o null si no es una caña. */
    @Nullable
    public static RodStats stats(ItemStack rod, FishingMedium medium, double realDepth) {
        RodTier tier = ModItems.tierOf(rod);
        if (tier == null) return null;
        return RodStats.compute(new RodStats.Input(tier, modifiers(rod),
                EnchantmentHelper.getFishingLuckBonus(rod), EnchantmentHelper.getFishingSpeedBonus(rod), 0, medium, realDepth));
    }

    /** Una caña vacía no guarda la etiqueta, para que sea idéntica a una recién fabricada. */
    private static void cleanup(ItemStack rod) {
        CompoundTag root = rod.getTagElement(ROOT);
        if (root != null && root.isEmpty()) rod.removeTagKey(ROOT);
    }
}
