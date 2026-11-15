package com.rolflin.pesca.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reglas de las ranuras: genéricas, con 1 accesorio por categoría y 2 amuletos distintos. */
public final class Loadout {
    public enum Category { SEDAL, ANZUELO, PESO, CARRETE, AMULETO }

    public record Equipped(String id, Category category, int level) {
    }

    private Loadout() {
    }

    /** @return null si el conjunto es válido para esa caña; si no, el motivo. */
    public static String validate(RodTier tier, List<Equipped> items) {
        if (items.size() > tier.accessorySlots) return "ranuras";
        Set<String> ids = new HashSet<>();
        int[] perCategory = new int[Category.values().length];
        for (Equipped e : items) {
            if (e.level() < 1 || e.level() > tier.maxAccessoryLevel()) return "nivel";
            if (!ids.add(e.id())) return "repetido";
            int n = ++perCategory[e.category().ordinal()];
            if (n > (e.category() == Category.AMULETO ? 2 : 1)) return "categoria";
        }
        return null;
    }

    /** ¿Cabe este accesorio en la caña con lo que ya lleva? */
    public static boolean canAdd(RodTier tier, List<Equipped> current, Equipped extra) {
        List<Equipped> all = new java.util.ArrayList<>(current);
        all.add(extra);
        return validate(tier, all) == null;
    }
}
