package com.rolflin.toodeepseas.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefsTest {
    private static final Path DATA = Path.of("src/main/resources/data/toodeepseas");

    private static List<Path> jsons(String folder) throws IOException {
        try (Stream<Path> s = Files.list(DATA.resolve(folder))) {
            return s.filter(p -> p.toString().endsWith(".json")).toList();
        }
    }

    private static JsonObject read(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    @Test
    void todosLosAccesoriosDelDatapackSonValidos() throws IOException {
        List<Path> files = jsons("accessories");
        assertEquals(20, files.size());
        for (Path p : files) {
            AccessoryDef d = AccessoryDef.parse(p.getFileName().toString().replace(".json", ""), read(p));
            assertTrue(d.maxLevel() >= 1 && d.usesAt(1) > 0, d.id());
        }
    }

    @Test
    void sedalReforzado() throws IOException {
        AccessoryDef d = AccessoryDef.parse("sedal_reforzado", read(DATA.resolve("accessories/sedal_reforzado.json")));
        assertEquals(Loadout.Category.SEDAL, d.category());
        assertEquals(3, d.maxLevel());
        assertEquals(90, d.usesAt(3));
        assertEquals(List.of(new Modifier(Stat.DEPTH, 20)), d.modifiers(2));
        RodStats s = RodStats.compute(new RodStats.Input(RodTier.COBRE, d.modifiers(1), 0, 0, 0, FishingMedium.WATER, 200));
        assertEquals(55, s.maxDepth);
    }

    @Test
    void cebos() throws IOException {
        assertEquals(10, jsons("baits").size());
        for (Path p : jsons("baits")) BaitDef.parse(p.getFileName().toString(), read(p));
        BaitDef carne = BaitDef.parse("carne_podrida_aderezada", read(DATA.resolve("baits/carne_podrida_aderezada.json")));
        assertTrue(carne.conditionMet("rio", 60, "despejado"));
        assertFalse(carne.conditionMet("rio", 30, "despejado"));
        assertEquals(2.0, carne.weight("carnivoro", true));
        assertEquals(1.0, carne.weight("comun", true));
        assertEquals(BaitDef.FALLBACK, carne.weight("carnivoro", false));
        assertEquals(3.0, BaitDef.combine(2.0, 2.0));
        BaitDef cobre = BaitDef.parse("cebo_cobre_cargado", read(DATA.resolve("baits/cebo_cobre_cargado.json")));
        assertFalse(cobre.conditionMet("oceano", 60, "lluvia"));
        assertTrue(cobre.conditionMet("oceano", 60, "tormenta"));
    }
}
