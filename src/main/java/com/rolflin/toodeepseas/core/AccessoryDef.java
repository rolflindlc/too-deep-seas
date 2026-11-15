package com.rolflin.toodeepseas.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Accesorio definido por datapack ({@code data/<ns>/accessories/<id>.json}).
 * Los niveles salen de la lista de usos: 3 valores = niveles I a III.
 * {@code repair} es el id del objeto con el que se repara en la mesa del pescador.
 */
public record AccessoryDef(String id, Loadout.Category category, int[] uses, String trigger, List<Effect> effects, String special, String repair) {
    /** Valor de una estadística en cada nivel. */
    public record Effect(Stat stat, double[] values) {
    }

    public int maxLevel() {
        return uses.length;
    }

    public int usesAt(int level) {
        return uses[Math.max(1, Math.min(level, uses.length)) - 1];
    }

    /** Tier mínimo de caña que lo admite ({@code min_tier:2.5} en {@code special}); 0 si no pide ninguno. */
    public double minTier() {
        if (special != null) for (String part : special.split(";")) {
            if (part.startsWith("min_tier:")) return Double.parseDouble(part.substring("min_tier:".length()));
        }
        return 0;
    }

    public List<Modifier> modifiers(int level) {
        List<Modifier> out = new ArrayList<>();
        for (Effect e : effects) out.add(new Modifier(e.stat(), e.values()[Math.max(1, Math.min(level, e.values().length)) - 1]));
        return out;
    }

    public static AccessoryDef parse(String id, JsonObject o) {
        Loadout.Category cat = Loadout.Category.valueOf(o.get("category").getAsString().toUpperCase(Locale.ROOT));
        int[] uses = o.getAsJsonArray("uses").asList().stream().mapToInt(JsonElement::getAsInt).toArray();
        if (uses.length < 1 || uses.length > 3) throw new IllegalArgumentException("uses debe tener de 1 a 3 valores");
        List<Effect> effects = new ArrayList<>();
        if (o.has("effects")) for (JsonElement el : o.getAsJsonArray("effects")) {
            JsonObject e = el.getAsJsonObject();
            double[] v = e.getAsJsonArray("values").asList().stream().mapToDouble(JsonElement::getAsDouble).toArray();
            if (v.length != uses.length) throw new IllegalArgumentException("values y uses deben tener la misma longitud");
            effects.add(new Effect(Stat.valueOf(e.get("stat").getAsString().toUpperCase(Locale.ROOT)), v));
        }
        return new AccessoryDef(id, cat, uses, o.has("trigger") ? o.get("trigger").getAsString() : "run_end", List.copyOf(effects),
                o.has("special") ? o.get("special").getAsString() : null,
                o.has("repair") ? o.get("repair").getAsString() : null);
    }
}
