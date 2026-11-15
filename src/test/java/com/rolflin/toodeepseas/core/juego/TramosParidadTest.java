package com.rolflin.toodeepseas.core.juego;

import com.google.gson.JsonParser;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import com.rolflin.toodeepseas.core.RodStats;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que Java pone en cada tramo (rocas, peligros, franjas, peces y tesoros) tiene que ser lo mismo que pone el
 * prototipo web. {@code paridad/tramos.txt} lo genera {@code tools/paridad/gen_tramos.js}: 42 partidas.
 */
class TramosParidadTest {
    private static final double EPS = 1e-3;

    static List<PezDef> peces() throws IOException {
        List<PezDef> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(Path.of("src/main/resources/data/toodeepseas/fish"))) {
            for (Path p : (Iterable<Path>) s::iterator)
                out.add(PezDef.parse(p.getFileName().toString().replace(".json", ""), JsonParser.parseString(Files.readString(p)).getAsJsonObject()));
        }
        return out;
    }

    static Set<String> todosLosCebos(List<PezDef> peces) {
        Set<String> cebos = new HashSet<>();
        for (PezDef p : peces) if (p.bait() != null) cebos.add(p.bait());
        return cebos;
    }

    static Partida.Ajustes ajustes(int seed, Bioma b, Perfil.SubBioma sub, Clima c, double maxD, int luck, Set<String> cebos) {
        return new Partida.Ajustes(seed, b, sub, c, maxD, RodStats.BASE_DESCENT, RodStats.ACCEL_PER_BLOCK, RodStats.BASE_HSPEED, RodStats.BASE_DIVE,
                Partida.HR, luck, 0, 0, 0, cebos);
    }

    private static String n(double v) {
        return Double.toString(v);
    }

    private static List<String> lineas(Partida.Trozo c) {
        List<String> out = new ArrayList<>();
        for (Partida.Roca o : c.rocks) out.add(String.join(" ", "R", o.kind.name().toLowerCase(Locale.ROOT), n(o.x), n(o.y), n(o.r), n(o.s)));
        for (Partida.Peligro h : c.haz) {
            double[] v = switch (h.k) {
                case BUB -> new double[]{h.x, h.y, h.r, h.v, h.met ? 1 : 0, 0};
                case DRIFT -> new double[]{h.x0, h.y, h.amp, h.ph, h.sp, h.r};
                case NET -> new double[]{h.x0, h.y, h.ph, 0, 0, 0};
                case SEEK -> new double[]{h.x, h.y, h.r, h.bul ? 1 : 0, 0, 0};
                case CROSS -> new double[]{h.x, h.y, h.dir, h.v, h.ghast ? 1 : 0, 0};
                case GEY -> new double[]{h.x, h.y, h.dx, h.dy, h.range, h.tm};
                case COL -> new double[]{h.x, h.y, h.hh, h.tm, h.chim ? 1 : 0, 0};
                case FALL -> new double[]{h.x, h.y, 0, 0, 0, 0};
                case LION -> new double[]{h.x, h.y, h.tm, 0, 0, 0};
                case LUNGE -> new double[]{h.wx, h.y, h.dir, h.range, h.tm, h.stay ? 1 : 0};
            };
            StringBuilder sb = new StringBuilder("Z " + h.k.name().toLowerCase(Locale.ROOT));
            for (double x : v) sb.append(' ').append(x);
            out.add(sb.toString());
        }
        for (Partida.Franja o : c.cur) out.add(String.join(" ", "F", o.fx.name().toLowerCase(Locale.ROOT), n(o.y), n(o.h), n(o.dir), n(o.f), n(o.m)));
        for (Partida.Cosa it : c.items) out.add(String.join(" ", "I", n(it.id), n(it.x), n(it.y), n(it.vx), n(it.ph), n(it.wait), it.hid ? "1" : "0"));
        return out;
    }

    private static void igual(String esperado, String real, String donde) {
        String[] a = esperado.split(" "), b = real.split(" ");
        String msg = donde + ": " + esperado + " / " + real;
        assertEquals(a.length, b.length, msg);
        for (int i = 0; i < a.length; i++) {
            if (Character.isLetter(a[i].charAt(0))) assertEquals(a[i], b[i], msg);
            else assertEquals(Double.parseDouble(a[i]), Double.parseDouble(b[i]), EPS, msg);
        }
    }

    /** Devuelve {casos, elementos comparados}. */
    static int[] comparar() throws IOException {
        List<PezDef> peces = peces();
        List<Partida.Objeto> objs = Partida.objetos(peces);
        Set<String> cebos = todosLosCebos(peces);
        InputStream in = TramosParidadTest.class.getResourceAsStream("/paridad/tramos.txt");
        assertNotNull(in, "falta paridad/tramos.txt");
        int casos = 0, elementos = 0, pos = 0;
        Partida p = null;
        List<String> actual = List.of();
        String donde = "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = br.readLine()) != null; ) {
                String[] t = line.split(" ");
                if (t[0].equals("CASE")) {
                    assertEquals(actual.size(), pos, donde + ": sobran elementos en Java");
                    double maxD = t[5].equals("Infinity") ? Double.POSITIVE_INFINITY : Double.parseDouble(t[5]);
                    p = new Partida(ajustes(Integer.parseInt(t[4]), Bioma.byId(t[1]), Perfil.SubBioma.byId(t[2]), Clima.byId(t[3]), maxD, Integer.parseInt(t[6]), cebos), objs, false);
                    casos++;
                    actual = List.of();
                    pos = 0;
                } else if (t[0].equals("T")) {
                    assertEquals(actual.size(), pos, donde + ": sobran elementos en Java");
                    donde = "caso " + casos + " tramo " + t[1];
                    actual = lineas(p.genTrozo(Integer.parseInt(t[1])));
                    pos = 0;
                } else {
                    assertTrue(pos < actual.size(), donde + ": falta en Java " + line);
                    igual(line, actual.get(pos++), donde);
                    elementos++;
                }
            }
        }
        assertEquals(actual.size(), pos, donde + ": sobran elementos en Java");
        return new int[]{casos, elementos};
    }

    @Test
    void losTramosSonLosDelPrototipo() throws IOException {
        int[] n = comparar();
        assertEquals(42, n[0]);
        assertTrue(n[1] > 3000, "pocos elementos comparados: " + n[1]);
    }
}
