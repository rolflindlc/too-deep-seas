package com.rolflin.toodeepseas.core.juego;

import com.google.gson.JsonParser;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PezDefTest {
    private static final Path DATA = Path.of("src/main/resources/data/toodeepseas");
    /** Cebos del Nether que llegarán con la fase de la lava. */
    private static final Set<String> CEBOS_FUTUROS = Set.of("hongo_carmesi_cebado", "hongo_distorsionado_cebado", "verruga_cebada");

    private static List<PezDef> peces() throws IOException {
        List<PezDef> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DATA.resolve("fish"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                out.add(PezDef.parse(p.getFileName().toString().replace(".json", ""), JsonParser.parseString(Files.readString(p)).getAsJsonObject()));
            }
        }
        return out;
    }

    @Test
    void sonCienPecesConDiezMovimientos() throws IOException {
        List<PezDef> peces = peces();
        assertEquals(100, peces.size());
        Map<PezDef.Mov, Integer> n = new EnumMap<>(PezDef.Mov.class);
        Set<Integer> orden = new HashSet<>();
        for (PezDef p : peces) {
            n.merge(p.movement(), 1, Integer::sum);
            assertTrue(orden.add(p.order()), "orden repetido: " + p.id());
            assertFalse(p.name().isBlank(), p.id());
        }
        // El reparto del documento de diseño
        assertEquals(Map.of(PezDef.Mov.SWIM, 12, PezDef.Mov.VERTICAL, 9, PezDef.Mov.SINE, 11, PezDef.Mov.DART, 10, PezDef.Mov.CIRCLE, 9,
                PezDef.Mov.ZIGZAG, 10, PezDef.Mov.JET, 9, PezDef.Mov.FLEE, 9, PezDef.Mov.SCHOOL, 11, PezDef.Mov.AMBUSH, 10), n);
    }

    @Test
    void cadaBiomaTieneTodasLasRarezas() throws IOException {
        List<PezDef> peces = peces();
        for (Bioma b : Bioma.values()) {
            Set<Integer> rarezas = new HashSet<>();
            for (PezDef p : peces) if (p.biomes().contains(b)) rarezas.add(p.rarity());
            for (int r = b.hasWeather ? 0 : 1; r <= 4; r++) assertTrue(rarezas.contains(r), b + " sin peces de rareza " + r);
        }
        // En el Vacío no hay paredes: nada de emboscadas
        for (PezDef p : peces) if (p.biomes().contains(Bioma.VACIO)) assertTrue(p.movement() != PezDef.Mov.AMBUSH, p.id());
    }

    @Test
    void losCebosExisten() throws IOException {
        for (PezDef p : peces()) {
            if (p.bait() == null) continue;
            assertTrue(CEBOS_FUTUROS.contains(p.bait()) || Files.exists(DATA.resolve("baits/" + p.bait() + ".json")), p.id() + " pide el cebo " + p.bait());
        }
    }

    @Test
    void unPezSoloSaleDondeYCuandoLeToca() throws IOException {
        PezDef raya = null, almas = null;
        for (PezDef p : peces()) {
            if (p.id().equals("raya_electrica")) raya = p;
            if (p.id().equals("pez_fantasma_de_almas")) almas = p;
        }
        Set<String> cobre = Set.of("cebo_cobre_cargado");
        assertTrue(raya.aparece(Bioma.OCEANO, Clima.TORMENTA, null, cobre, 45));
        assertFalse(raya.aparece(Bioma.OCEANO, Clima.TORMENTA, null, cobre, 44), "demasiado arriba");
        assertFalse(raya.aparece(Bioma.OCEANO, Clima.LLUVIA, null, cobre, 60), "sin tormenta");
        assertFalse(raya.aparece(Bioma.OCEANO, Clima.TORMENTA, null, Set.of(), 60), "sin cebo");
        assertFalse(raya.aparece(Bioma.RIO, Clima.TORMENTA, null, cobre, 60), "otro bioma");
        assertTrue(almas.ghost());
        assertTrue(almas.aparece(Bioma.LAVA, Clima.DESPEJADO, Perfil.SubBioma.ALMAS, Set.of(), 30));
        assertFalse(almas.aparece(Bioma.LAVA, Clima.DESPEJADO, Perfil.SubBioma.DELTAS, Set.of(), 30), "otro sub-bioma");
    }
}
