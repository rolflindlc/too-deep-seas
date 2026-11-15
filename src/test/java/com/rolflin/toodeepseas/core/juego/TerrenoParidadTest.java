package com.rolflin.toodeepseas.core.juego;

import com.rolflin.toodeepseas.core.RodStats;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El terreno de Java tiene que ser el mismo que el del prototipo web. {@code paridad/terreno.txt} lo genera
 * {@code tools/paridad/gen_terreno.js} ejecutando el prototipo: 11 biomas × 4 semillas y equipos.
 */
class TerrenoParidadTest {
    private static final Map<String, Perfil> PERFILES = Map.ofEntries(
            Map.entry("oceano", Perfil.OCEANO), Map.entry("calido", Perfil.CALIDO), Map.entry("helado", Perfil.HELADO),
            Map.entry("rio", Perfil.RIO), Map.entry("pantano", Perfil.PANTANO), Map.entry("deltas", Perfil.DELTAS),
            Map.entry("almas", Perfil.ALMAS), Map.entry("carmesi", Perfil.CARMESI), Map.entry("distorsionado", Perfil.DISTORSIONADO),
            Map.entry("yermos", Perfil.YERMOS), Map.entry("vacio", Perfil.VACIO));
    private static final double EPS = 1e-4;

    /** Devuelve {casos, cuevas, alturas comprobadas}. */
    static int[] comparar() throws IOException {
        InputStream in = TerrenoParidadTest.class.getResourceAsStream("/paridad/terreno.txt");
        assertNotNull(in, "falta paridad/terreno.txt");
        int casos = 0, cuevas = 0, alturas = 0, sec = 0;
        Terreno t = null;
        String caso = "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = br.readLine()) != null; ) {
                String[] p = line.split(" ");
                switch (p[0]) {
                    case "CASE" -> {
                        if (t != null) assertEquals(sec, t.sections.size(), "número de cuevas en " + caso);
                        caso = line;
                        Perfil perfil = PERFILES.get(p[1]);
                        assertNotNull(perfil, p[1]);
                        Terreno.Movimiento mov = new Terreno.Movimiento(Double.parseDouble(p[5]), RodStats.ACCEL_PER_BLOCK, Double.parseDouble(p[6]), RodStats.BASE_DIVE);
                        t = new Terreno(Integer.parseInt(p[3]), perfil, p[2].equals("1"), Double.parseDouble(p[4]), mov);
                        sec = 0;
                        casos++;
                    }
                    case "SEC" -> {
                        assertTrue(sec < t.sections.size(), "sobran cuevas en el prototipo: " + caso);
                        Terreno.Seccion s = t.sections.get(sec++);
                        double[] got = {s.y0, s.y1, s.cw, s.ex, s.xx, s.fi, s.fo, s.paths.size(), s.pil.size()};
                        for (int i = 0; i < got.length; i++) assertEquals(Double.parseDouble(p[i + 1]), got[i], EPS, "cueva " + sec + " campo " + i + " en " + caso);
                        cuevas++;
                    }
                    case "Y" -> {
                        double y = Double.parseDouble(p[1]);
                        double[][] iv = t.spansAt(y);
                        assertEquals(Integer.parseInt(p[2]), iv.length, "huecos a y=" + y + " en " + caso);
                        for (int i = 0; i < iv.length; i++) {
                            assertEquals(Double.parseDouble(p[3 + 2 * i]), iv[i][0], EPS, "izquierda a y=" + y + " en " + caso);
                            assertEquals(Double.parseDouble(p[4 + 2 * i]), iv[i][1], EPS, "derecha a y=" + y + " en " + caso);
                        }
                        alturas++;
                    }
                    default -> throw new IllegalStateException(line);
                }
            }
        }
        if (t != null) assertEquals(sec, t.sections.size(), "número de cuevas en " + caso);
        return new int[]{casos, cuevas, alturas};
    }

    @Test
    void elTerrenoEsIgualQueElDelPrototipo() throws IOException {
        int[] n = comparar();
        assertEquals(44, n[0]);
        assertTrue(n[1] >= 80, "cuevas comparadas: " + n[1]);
        assertTrue(n[2] > 5000, "alturas comparadas: " + n[2]);
    }

    @Test
    void elAzarEsElDelPrototipo() {
        // Valores sacados del prototipo: mulberry32(12345)() tres veces, hash2(12345, 777) y vnoise(12352, 3.7)
        Azar r = new Azar(12345);
        assertEquals(0.9797282677609473, r.next(), 1e-15);
        assertEquals(0.3067522644996643, r.next(), 1e-15);
        assertEquals(0.484205421525985, r.next(), 1e-15);
        assertEquals(12345, Azar.semillaDe("12345"));
        assertEquals((int) 4000000000L, Azar.semillaDe("4000000000"));
    }
}
