package com.rolflin.toodeepseas.core.juego;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El catálogo de capturas y todo lo que genera tools/gen_capturas.py: que nada apunte a un objeto o etiqueta que no existe. */
class CatalogoTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final Path DATA = RES.resolve("data/toodeepseas");
    private static final Path ASSETS = RES.resolve("assets/toodeepseas");
    private static final Set<String> VANILLA = Set.of("minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish");
    private static final Set<String> POCIONES = Set.of("prisa", "gracia_delfin", "conducto", "resistencia", "absorcion", "brillo", "levitacion",
            "descomposicion", "aletas", "coraza");

    private static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    private static Set<String> propios() {
        Set<String> out = new HashSet<>();
        for (Catalogo.Pez p : Catalogo.PECES) if (p.own()) out.add(p.id());
        for (Catalogo.Tesoro t : Catalogo.TESOROS) if (t.own()) out.add(t.id());
        for (Catalogo.Comida c : Catalogo.COMIDAS) out.add(c.id());
        return out;
    }

    /** Todas las cadenas de un JSON, con la clave bajo la que están. */
    private static void cadenas(String key, JsonElement e, List<String[]> out) {
        if (e.isJsonObject()) for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) cadenas(en.getKey(), en.getValue(), out);
        else if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) cadenas(key, x, out);
        else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) out.add(new String[]{key, e.getAsString()});
    }

    private static List<Path> archivos(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(f -> f.toString().endsWith(".json")).sorted().toList();
        }
    }

    @Test
    void cienPecesYTreintaYTresTesoros() throws IOException {
        assertEquals(100, Catalogo.PECES.size());
        Set<String> enDatapack = new TreeSet<>();
        try (Stream<Path> s = Files.list(DATA.resolve("fish"))) {
            s.forEach(p -> enDatapack.add(p.getFileName().toString().replace(".json", "")));
        }
        Set<String> enCatalogo = new TreeSet<>();
        int propios = 0;
        for (Catalogo.Pez p : Catalogo.PECES) {
            enCatalogo.add(p.id());
            if (p.own()) {
                propios++;
                assertEquals("toodeepseas:" + p.id(), p.item());
            } else {
                assertTrue(VANILLA.contains(p.item()), p.item());
            }
            assertTrue(p.nutrition() > 0 && p.rarity() >= 0 && p.rarity() <= 4, p.id());
            assertEquals(p.family().equals("igneo"), p.fuel() > 0, "solo arden los peces de lava: " + p.id());
            // El catálogo y el datapack dicen lo mismo del pez
            PezDef def = PezDef.parse(p.id(), json(DATA.resolve("fish/" + p.id() + ".json")));
            assertEquals(def.rarity(), p.rarity(), p.id());
            assertEquals(def.family(), p.family(), p.id());
        }
        assertEquals(enDatapack, enCatalogo);
        assertEquals(96, propios);

        assertEquals(33, Catalogo.TESOROS.size());
        int tesorosPropios = 0, cofres = 0;
        for (int i = 0; i < Catalogo.TESOROS.size(); i++) {
            Catalogo.Tesoro t = Catalogo.TESOROS.get(i);
            assertEquals(i, t.order(), "los tesoros van en el orden del prototipo");
            assertFalse(t.biomes().isEmpty(), t.id());
            if (t.own()) tesorosPropios++;
            if (t.chest()) {
                cofres++;
                assertEquals(null, t.item());
            } else {
                assertNotNull(t.item(), t.id());
            }
            assertTrue(Set.of("material", "abrir", "caliz", "cabeza", "mano").contains(t.kind()), t.kind());
            if (t.kind().equals("abrir")) assertTrue(Files.exists(DATA.resolve("loot_tables/tesoros/" + t.id() + ".json")), t.id());
            if (t.kind().equals("cabeza") || t.kind().equals("mano")) assertFalse(t.effects().isEmpty(), t.id());
        }
        assertEquals(20, tesorosPropios);
        assertEquals(2, cofres);
        assertEquals(11, Catalogo.COMIDAS.size());
        assertNotNull(Catalogo.pez("pez_espada"));
        assertNotNull(Catalogo.tesoro("geoda"));
    }

    @Test
    void cadaObjetoTieneModeloTexturaYTextos() throws IOException {
        for (String lang : new String[]{"es_es", "en_us"}) {
            JsonObject l = json(ASSETS.resolve("lang/" + lang + ".json"));
            for (String id : propios()) {
                assertTrue(l.has("captura.toodeepseas." + id), lang + ": sin nombre " + id);
                assertTrue(Files.exists(ASSETS.resolve("models/item/" + id + ".json")), "sin modelo " + id);
                assertTrue(Files.exists(ASSETS.resolve("textures/item/capturas/" + id + ".png")), "sin textura " + id);
            }
            for (Catalogo.Pez p : Catalogo.PECES) {
                for (int i = 1; i <= p.lines(); i++) assertTrue(l.has("captura.toodeepseas." + p.id() + ".uso." + i), lang + ": " + p.id() + " uso " + i);
                assertTrue(l.has("tooltip.toodeepseas.captura.familia." + p.family()), p.family());
            }
            for (Catalogo.Tesoro t : Catalogo.TESOROS) {
                for (int i = 1; i <= t.lines(); i++) assertTrue(l.has("captura.toodeepseas." + t.id() + ".uso." + i), lang + ": " + t.id() + " uso " + i);
                if (t.own()) assertTrue(t.lines() > 0, "tesoro sin ningún uso: " + t.id());
            }
            for (String p : POCIONES) {
                for (String forma : new String[]{"potion", "splash_potion", "lingering_potion", "tipped_arrow"}) {
                    assertTrue(l.has("item.minecraft." + forma + ".effect." + p), lang + ": " + forma + " " + p);
                }
            }
            assertTrue(l.has("effect.toodeepseas.aletas") && l.has("effect.toodeepseas.coraza"));
        }
        assertTrue(Files.exists(ASSETS.resolve("textures/mob_effect/aletas.png")) && Files.exists(ASSETS.resolve("textures/mob_effect/coraza.png")));
    }

    @Test
    void todosLosPecesSirvenParaAlgo() {
        // Además de comerse y venderse: cocina, plato, poción, material o combustible
        for (Catalogo.Pez p : Catalogo.PECES) {
            if (p.own()) assertTrue(p.lines() > 0 || !p.effects().isEmpty(), "pez sin ningún uso propio: " + p.id());
        }
    }

    @Test
    void lasMezclasUsanObjetosYPocionesQueExisten() {
        Set<String> propios = propios();
        Set<String> destinos = new HashSet<>();
        Set<String> ingredientes = new HashSet<>();
        for (Catalogo.Mezcla m : Catalogo.MEZCLAS) {
            assertTrue(destinos.add(m.to()), "poción repetida: " + m.to());
            if (m.to().startsWith("toodeepseas:")) assertTrue(POCIONES.contains(m.to().substring(12)), m.to());
            assertFalse(m.with().isEmpty());
            for (String id : m.with()) {
                assertTrue(ingredientes.add(id), "un ingrediente no puede dar dos pociones: " + id);
                assertTrue(id.startsWith("toodeepseas:") && propios.contains(id.substring(12)), id);
            }
        }
        for (String p : POCIONES) assertTrue(destinos.contains("toodeepseas:" + p), "poción sin receta: " + p);
    }

    @Test
    void recetasYEtiquetasNoApuntanANadaQueNoExista() throws IOException {
        Set<String> propios = propios();
        int olla = 0, corte = 0, normales = 0;
        for (Path p : archivos(DATA.resolve("recipes/capturas"))) {
            JsonObject o = json(p);
            String type = o.get("type").getAsString();
            boolean fd = type.startsWith("farmersdelight:");
            String cond = o.has("conditions") ? o.get("conditions").toString() : "";
            // Lo de Farmer's Delight solo carga con él; su alternativa de mesa de trabajo, solo sin él
            if (fd) assertTrue(cond.contains("forge:mod_loaded") && !cond.contains("forge:not"), p.toString());
            if (type.equals("farmersdelight:cooking")) olla++;
            else if (type.equals("farmersdelight:cutting")) corte++;
            else normales++;
            List<String[]> strings = new ArrayList<>();
            cadenas("", o, strings);
            for (String[] s : strings) {
                String key = s[0], value = s[1];
                if (key.equals("tag") && value.startsWith("toodeepseas:")) {
                    assertTrue(Files.exists(DATA.resolve("tags/items/" + value.substring(12) + ".json")), p + ": etiqueta " + value);
                } else if ((key.equals("item") || key.equals("result")) && value.startsWith("toodeepseas:")) {
                    assertTrue(propios.contains(value.substring(12)), p + ": objeto " + value);
                } else if (value.startsWith("farmersdelight:")) {
                    assertTrue(fd, p + " usa Farmer's Delight sin condición: " + value);
                }
            }
        }
        assertEquals(8, olla);
        assertEquals(6, corte);
        assertTrue(normales > 40, "recetas: " + normales);

        for (Path p : archivos(DATA.resolve("tags/items"))) {
            for (JsonElement e : json(p).getAsJsonArray("values")) {
                String v = e.getAsString();
                assertTrue(VANILLA.contains(v) || (v.startsWith("toodeepseas:") && propios.contains(v.substring(12))), p + ": " + v);
            }
        }
        // Las etiquetas comunes (forge, minecraft) apuntan a etiquetas del mod que existen
        for (String tag : new String[]{"data/minecraft/tags/items/fishes.json", "data/forge/tags/items/raw_fishes.json",
                "data/forge/tags/items/raw_fishes/cod.json", "data/forge/tags/items/raw_fishes/salmon.json", "data/forge/tags/items/cooked_fishes.json"}) {
            for (JsonElement e : json(RES.resolve(tag)).getAsJsonArray("values")) {
                String v = e.getAsString();
                if (v.startsWith("#toodeepseas:")) assertTrue(Files.exists(DATA.resolve("tags/items/" + v.substring(13) + ".json")), tag + ": " + v);
                else assertTrue(propios.contains(v.substring(12)), tag + ": " + v);
            }
        }
    }
}
