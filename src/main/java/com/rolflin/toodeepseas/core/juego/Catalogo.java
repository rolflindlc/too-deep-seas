package com.rolflin.toodeepseas.core.juego;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las capturas como objetos: qué objeto es cada pez y cada tesoro, qué alimenta, qué efectos da y para qué sirve.
 * Se lee de {@code toodeepseas_capturas.json}, que genera {@code tools/gen_capturas.py} a partir de los peces del
 * datapack y de la lista de tesoros del prototipo. Va dentro del jar (no es datapack) porque los objetos se registran
 * antes de que carguen los datos. Java puro: solo depende de gson.
 */
public final class Catalogo {
    /** Efecto de poción: id, segundos (0 = mientras se lleve puesto), nivel − 1 y probabilidad. */
    public record Efecto(String id, int seconds, int amplifier, float chance) {
    }

    /**
     * @param item   id del objeto (los 4 peces vanilla son los de Minecraft)
     * @param own    el objeto es del mod
     * @param always se puede comer sin hambre
     * @param fuel   ticks que arde en un horno (0 = no arde)
     * @param lines  líneas de «para qué sirve» de su descripción
     */
    public record Pez(String id, String item, boolean own, int rarity, String family, int nutrition, float saturation,
                      List<Efecto> effects, boolean always, int fuel, int lines) {
    }

    /**
     * @param order    posición en la lista del prototipo
     * @param item     id del objeto, o {@code null} si es un cofre del tesoro del bioma
     * @param kind     material, abrir (botín al usarlo), caliz, cabeza (efectos al llevarlo) o mano (en la secundaria)
     * @param leftover objeto que queda al abrirlo, o {@code null}
     */
    public record Tesoro(String id, String name, int order, String item, boolean own, boolean chest, int rarity, int minDepth,
                         List<Bioma> biomes, List<Clima> weather, String kind, List<Efecto> effects, String leftover, int xp,
                         int stack, int lines) {
        public boolean aparece(Bioma bioma, Clima clima, double profundidad) {
            return minDepth <= profundidad && biomes.contains(bioma) && (weather.isEmpty() || weather.contains(clima));
        }
    }

    /** @param container objeto que queda al comerlo (cuenco, palo), o {@code null} */
    public record Comida(String id, int nutrition, float saturation, List<Efecto> effects, String container) {
    }

    /** Mezcla del soporte para pociones: poción rara + uno de los ingredientes = poción {@code to}. */
    public record Mezcla(String to, List<String> with) {
    }

    public static final List<Pez> PECES;
    public static final List<Tesoro> TESOROS;
    public static final List<Comida> COMIDAS;
    public static final List<Mezcla> MEZCLAS;
    private static final Map<String, Pez> PEZ_POR_ID = new LinkedHashMap<>();
    private static final Map<String, Tesoro> TESORO_POR_ID = new LinkedHashMap<>();

    static {
        JsonObject root;
        try (InputStream in = Catalogo.class.getResourceAsStream("/toodeepseas_capturas.json")) {
            if (in == null) throw new IllegalStateException("falta toodeepseas_capturas.json");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        List<Pez> peces = new ArrayList<>();
        for (JsonElement e : root.getAsJsonArray("peces")) {
            JsonObject o = e.getAsJsonObject();
            Pez p = new Pez(o.get("id").getAsString(), o.get("item").getAsString(), o.get("own").getAsBoolean(), o.get("rarity").getAsInt(),
                    o.get("family").getAsString(), o.get("nutrition").getAsInt(), o.get("saturation").getAsFloat(), efectos(o.getAsJsonArray("effects")),
                    o.get("always").getAsBoolean(), o.get("fuel").getAsInt(), o.get("lines").getAsInt());
            peces.add(p);
            PEZ_POR_ID.put(p.id(), p);
        }
        List<Tesoro> tesoros = new ArrayList<>();
        for (JsonElement e : root.getAsJsonArray("tesoros")) {
            JsonObject o = e.getAsJsonObject();
            List<Bioma> biomas = new ArrayList<>();
            for (JsonElement b : o.getAsJsonArray("biomes")) biomas.add(Bioma.byId(b.getAsString()));
            List<Clima> climas = new ArrayList<>();
            for (JsonElement c : o.getAsJsonArray("weather")) climas.add(Clima.byId(c.getAsString()));
            Tesoro t = new Tesoro(o.get("id").getAsString(), o.get("name").getAsString(), o.get("order").getAsInt(), texto(o, "item"),
                    o.get("own").getAsBoolean(), o.get("chest").getAsBoolean(), o.get("rarity").getAsInt(), o.get("min_depth").getAsInt(),
                    List.copyOf(biomas), List.copyOf(climas), o.get("kind").getAsString(), efectos(o.getAsJsonArray("effects")),
                    texto(o, "leftover"), o.get("xp").getAsInt(), o.get("stack").getAsInt(), o.get("lines").getAsInt());
            tesoros.add(t);
            TESORO_POR_ID.put(t.id(), t);
        }
        List<Comida> comidas = new ArrayList<>();
        for (JsonElement e : root.getAsJsonArray("comidas")) {
            JsonObject o = e.getAsJsonObject();
            comidas.add(new Comida(o.get("id").getAsString(), o.get("nutrition").getAsInt(), o.get("saturation").getAsFloat(),
                    efectos(o.getAsJsonArray("effects")), texto(o, "container")));
        }
        List<Mezcla> mezclas = new ArrayList<>();
        for (JsonElement e : root.getAsJsonArray("mezclas")) {
            JsonObject o = e.getAsJsonObject();
            List<String> with = new ArrayList<>();
            for (JsonElement w : o.getAsJsonArray("with")) with.add(w.getAsString());
            mezclas.add(new Mezcla(o.get("to").getAsString(), List.copyOf(with)));
        }
        PECES = List.copyOf(peces);
        TESOROS = List.copyOf(tesoros);
        COMIDAS = List.copyOf(comidas);
        MEZCLAS = List.copyOf(mezclas);
    }

    private Catalogo() {
    }

    public static Pez pez(String id) {
        return PEZ_POR_ID.get(id);
    }

    public static Tesoro tesoro(String id) {
        return TESORO_POR_ID.get(id);
    }

    private static String texto(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    /** [id, segundos, nivel, probabilidad] o, en lo que se lleva puesto, [id, nivel]. */
    private static List<Efecto> efectos(JsonArray array) {
        List<Efecto> out = new ArrayList<>();
        for (JsonElement e : array) {
            JsonArray a = e.getAsJsonArray();
            if (a.size() == 2) out.add(new Efecto(a.get(0).getAsString(), 0, a.get(1).getAsInt(), 1F));
            else out.add(new Efecto(a.get(0).getAsString(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsFloat()));
        }
        return List.copyOf(out);
    }
}
