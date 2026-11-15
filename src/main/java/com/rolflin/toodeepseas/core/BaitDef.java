package com.rolflin.toodeepseas.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cebo definido por datapack ({@code data/<ns>/baits/<id>.json}). */
public record BaitDef(String id, Map<String, Double> families, List<String> exclusives, List<String> biomes, int minDepth, String weather) {
    /** Peso general cuando la condición del cebo no se cumple. */
    public static final double FALLBACK = 1.2;
    /** Tope por familia al combinar dos cebos (caña de prismarina). */
    public static final double CAP = 3.0;

    public boolean conditionMet(String biome, double maxDepth, String weatherNow) {
        return (biomes.isEmpty() || biomes.contains(biome)) && maxDepth >= minDepth && (weather == null || weather.equals(weatherNow));
    }

    /** Multiplicador de aparición de una familia con este cebo. */
    public double weight(String family, boolean conditionMet) {
        if (!conditionMet) return FALLBACK;
        Double w = families.get(family);
        if (w == null) w = families.get("todas");
        return w == null ? 1.0 : w;
    }

    /** Dos cebos a la vez: los pesos se multiplican, con tope de ×3. */
    public static double combine(double a, double b) {
        return Math.min(CAP, a * b);
    }

    public static BaitDef parse(String id, JsonObject o) {
        Map<String, Double> fam = new LinkedHashMap<>();
        if (o.has("families")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("families").entrySet()) fam.put(e.getKey(), e.getValue().getAsDouble());
        List<String> ex = new ArrayList<>(), biomes = new ArrayList<>();
        if (o.has("exclusives")) for (JsonElement e : o.getAsJsonArray("exclusives")) ex.add(e.getAsString());
        JsonObject c = o.has("condition") ? o.getAsJsonObject("condition") : new JsonObject();
        if (c.has("biomes")) for (JsonElement e : c.getAsJsonArray("biomes")) biomes.add(e.getAsString());
        return new BaitDef(id, Map.copyOf(fam), List.copyOf(ex), List.copyOf(biomes),
                c.has("min_depth") ? c.get("min_depth").getAsInt() : 0, c.has("weather") ? c.get("weather").getAsString() : null);
    }
}
