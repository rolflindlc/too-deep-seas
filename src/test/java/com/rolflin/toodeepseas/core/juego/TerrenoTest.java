package com.rolflin.toodeepseas.core.juego;

import com.rolflin.toodeepseas.core.Cofres.Bioma;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrenoTest {
    private static final Perfil[] PERFILES = {Perfil.OCEANO, Perfil.CALIDO, Perfil.HELADO, Perfil.RIO, Perfil.PANTANO, Perfil.DELTAS,
            Perfil.ALMAS, Perfil.CARMESI, Perfil.DISTORSIONADO, Perfil.YERMOS, Perfil.VACIO};

    /** El anzuelo mide 6 px: ningún hueco puede ser más estrecho que 12 px, en ningún bioma ni semilla. */
    @Test
    void siempreSeCabe() {
        for (Perfil p : PERFILES) {
            for (int seed = 1; seed <= 80; seed++) {
                Terreno t = new Terreno(seed * 7919, p, p == Perfil.OCEANO || p == Perfil.CALIDO || p == Perfil.RIO, 120, Terreno.Movimiento.PROTOTIPO);
                for (double y = 0; y <= 120 * Terreno.PPB; y += 3) {
                    double[][] iv = t.spansAt(y);
                    assertTrue(iv.length >= 1 && iv.length <= 6, "huecos: " + iv.length);
                    for (int i = 0; i < iv.length; i++) {
                        assertTrue(iv[i][1] - iv[i][0] >= 12, "hueco de " + (iv[i][1] - iv[i][0]) + " px en y=" + y + ", semilla " + seed * 7919);
                        if (i > 0) assertTrue(iv[i][0] > iv[i - 1][1], "huecos solapados en y=" + y);
                    }
                    assertTrue(t.inOpen(t.spanAt(Terreno.W / 2, y)[0] + 6, y, 3), "el anzuelo no cabe en y=" + y);
                }
            }
        }
    }

    @Test
    void lasCuevasNoSeSolapanYAcabanAntesDelFondo() {
        for (Perfil p : PERFILES) {
            for (int seed = 1; seed <= 80; seed++) {
                for (double maxD : new double[]{10, 34, 70, 120}) {
                    Terreno t = new Terreno(seed * 31337, p, false, maxD, Terreno.Movimiento.PROTOTIPO);
                    double fin = -1e9;
                    for (Terreno.Seccion s : t.sections) {
                        assertTrue(s.y1 > s.y0 && s.y0 - s.fi >= fin + 20, "cuevas solapadas");
                        assertTrue(s.y1 <= (maxD - 5) * Terreno.PPB, "cueva por debajo del fondo");
                        assertTrue(t.inSafe(s.y0 - 5, 0) && t.inSafe((s.y0 + s.y1) / 2, 0) && t.inSafe(s.y1 + 5, 0), "inSafe");
                        fin = s.y1 + s.fo;
                    }
                }
            }
        }
    }

    @Test
    void pantanoYVacioNoTienenCuevasYElVacioNoTieneParedes() {
        assertTrue(new Terreno(5, Bioma.PANTANO, null, 120, Terreno.Movimiento.PROTOTIPO).sections.isEmpty());
        Terreno vacio = new Terreno(5, Bioma.VACIO, null, Double.POSITIVE_INFINITY, Terreno.Movimiento.PROTOTIPO);
        assertTrue(vacio.sections.isEmpty());
        assertEquals(Terreno.W + 20, vacio.tunnelAt(50000).w(), 1e-9);
        assertTrue(!new Terreno(5, Bioma.LAVA, Perfil.SubBioma.DELTAS, 120, Terreno.Movimiento.PROTOTIPO).sections.isEmpty());
    }

    @Test
    void laMismaSemillaDaElMismoTerreno() {
        Terreno a = new Terreno(424242, Bioma.CALIDO, null, 120, Terreno.Movimiento.PROTOTIPO);
        Terreno b = new Terreno(424242, Bioma.CALIDO, null, 120, Terreno.Movimiento.PROTOTIPO);
        Terreno c = new Terreno(424243, Bioma.CALIDO, null, 120, Terreno.Movimiento.PROTOTIPO);
        boolean distinto = false;
        for (double y = 0; y < 2800; y += 17) {
            assertEquals(a.tunnelAt(y).l(), b.tunnelAt(y).l(), 0.0);
            assertEquals(a.tunnelAt(y).r(), b.tunnelAt(y).r(), 0.0);
            distinto |= a.tunnelAt(y).l() != c.tunnelAt(y).l();
        }
        assertTrue(distinto, "dos semillas dan el mismo terreno");
    }

    @Test
    void laBajadaTieneElTopeDeCuatroBloquesPorSegundo() {
        Terreno.Movimiento m = Terreno.Movimiento.PROTOTIPO;
        assertEquals(0.8 * Terreno.PPB, m.fallSpd(0, false, 1), 1e-9);
        assertEquals(4 * Terreno.PPB, m.fallSpd(100 * Terreno.PPB, true, 1.4), 1e-9);
        assertEquals(80, m.vhAt(30), 1e-9);
        assertEquals(160, m.vhAt(100), 1e-9);
    }
}
