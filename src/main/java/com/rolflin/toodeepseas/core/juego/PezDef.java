package com.rolflin.toodeepseas.core.juego;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Pez del minijuego, definido por datapack ({@code data/<ns>/fish/<id>.json}). Los 100 del mod salen del prototipo
 * ({@code tools/paridad/gen_peces.js}); otros mods o datapacks pueden añadir los suyos.
 *
 * @param order    posición en la lista del prototipo (el orden decide qué pez sale con una semilla dada)
 * @param rarity   0 común … 4 legendario
 * @param minDepth profundidad mínima (m)
 * @param biomes   biomas de pesca donde aparece
 * @param weather  climas en los que aparece (vacío = cualquiera)
 * @param subBiome sub-bioma del Nether que exige, o {@code null}
 * @param bait     cebo que exige (exclusivo de cebo), o {@code null}
 * @param shape    silueta especial (raya, anguila…), o {@code null}
 * @param ghost    semitransparente; se ve desde más lejos
 * @param color    color RGB del pez
 */
public record PezDef(String id, String name, int order, int rarity, int minDepth, List<Bioma> biomes, Mov movement, String family,
                     List<Clima> weather, Perfil.SubBioma subBiome, String bait, String shape, boolean ghost, int color) {

    /** Las 10 formas de moverse. */
    public enum Mov {
        SWIM, VERTICAL, SINE, DART, CIRCLE, ZIGZAG, JET, FLEE, SCHOOL, AMBUSH;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Mov byId(String id) {
            for (Mov m : values()) if (m.id().equals(id)) return m;
            throw new IllegalArgumentException("movimiento desconocido: " + id);
        }
    }

    /** ¿Puede salir este pez en una partida con ese bioma, clima, sub-bioma y cebos? (No mira la profundidad.) */
    public boolean aparece(Bioma bioma, Clima clima, Perfil.SubBioma sub, Collection<String> cebos) {
        return biomes.contains(bioma)
                && (weather.isEmpty() || weather.contains(clima))
                && (subBiome == null || (bioma == Bioma.LAVA && subBiome == sub))
                && (bait == null || cebos.contains(bait));
    }

    /** Igual, pero además tiene que estar a su profundidad. */
    public boolean aparece(Bioma bioma, Clima clima, Perfil.SubBioma sub, Collection<String> cebos, double profundidad) {
        return minDepth <= profundidad && aparece(bioma, clima, sub, cebos);
    }

    public static PezDef parse(String id, JsonObject o) {
        List<Bioma> biomes = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("biomes")) {
            Bioma b = Bioma.byId(e.getAsString());
            if (b == null) throw new IllegalArgumentException("bioma desconocido: " + e.getAsString());
            biomes.add(b);
        }
        if (biomes.isEmpty()) throw new IllegalArgumentException("sin biomas");
        List<Clima> weather = new ArrayList<>();
        if (o.has("weather")) for (JsonElement e : o.getAsJsonArray("weather")) {
            Clima c = Clima.byId(e.getAsString());
            if (c == null) throw new IllegalArgumentException("clima desconocido: " + e.getAsString());
            weather.add(c);
        }
        Perfil.SubBioma sub = null;
        if (o.has("sub_biome")) {
            sub = Perfil.SubBioma.byId(o.get("sub_biome").getAsString());
            if (sub == null) throw new IllegalArgumentException("sub-bioma desconocido: " + o.get("sub_biome").getAsString());
        }
        int rarity = o.get("rarity").getAsInt();
        if (rarity < 0 || rarity > 4) throw new IllegalArgumentException("rareza fuera de 0–4: " + rarity);
        String color = o.has("color") ? o.get("color").getAsString() : "#ffffff";
        return new PezDef(id, o.has("name") ? o.get("name").getAsString() : id, o.has("order") ? o.get("order").getAsInt() : Integer.MAX_VALUE,
                rarity, o.has("min_depth") ? o.get("min_depth").getAsInt() : 0, List.copyOf(biomes), Mov.byId(o.get("movement").getAsString()),
                o.has("family") ? o.get("family").getAsString() : "comun", List.copyOf(weather), sub,
                o.has("bait") ? o.get("bait").getAsString() : null, o.has("shape") ? o.get("shape").getAsString() : null,
                o.has("ghost") && o.get("ghost").getAsBoolean(), Integer.parseInt(color.substring(1), 16));
    }
}
