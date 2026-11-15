package com.rolflin.toodeepseas.core.juego;

import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La simulación del minijuego: determinista, repetible por el servidor y jugable hasta el fondo en todos los biomas. */
class PartidaTest {
    /** Jugador de pruebas: se mueve hacia el centro del hueco que tiene debajo y a ratos baja rápido. */
    private static int bot(Partida p, Azar azar) {
        double[] sp = p.terreno.spanAt(p.hookX, p.hookY + 30);
        double cx = (sp[0] + sp[1]) / 2;
        int teclas = p.hookX < cx - 2 ? Partida.DER : p.hookX > cx + 2 ? Partida.IZQ : 0;
        if (p.invT > 0 && teclas != 0) teclas ^= Partida.IZQ | Partida.DER;
        if (azar.next() < 0.3) teclas |= Partida.BAJAR;
        return teclas;
    }

    @Test
    void seLlegaAlFondoEnTodosLosBiomas() throws IOException {
        List<PezDef> peces = TramosParidadTest.peces();
        List<Partida.Objeto> objs = Partida.objetos(peces);
        Set<String> cebos = TramosParidadTest.todosLosCebos(peces);
        EnumSet<Partida.K> vistos = EnumSet.noneOf(Partida.K.class);
        int capturas = 0;
        for (Bioma b : Bioma.values())
            for (Clima c : b.hasWeather ? Clima.values() : new Clima[]{Clima.DESPEJADO})
                for (Perfil.SubBioma sub : b == Bioma.LAVA ? Perfil.SubBioma.values() : new Perfil.SubBioma[]{null})
                    for (int seed = 1; seed <= 3; seed++) {
                        double maxD = b == Bioma.VACIO ? Double.POSITIVE_INFINITY : 120;
                        Partida p = new Partida(TramosParidadTest.ajustes(seed * 7919, b, sub, c, maxD, 1, cebos), objs, true);
                        p.inmortal = true;
                        Azar azar = new Azar(seed);
                        String donde = b + " " + c + " " + sub + " " + seed;
                        for (int i = 0; i < 60 * 300 && p.estado != Partida.Estado.FIN; i++) {
                            int teclas = bot(p, azar);
                            if (p.atFloor || p.profundidad() > 650) teclas |= Partida.RECOGER;
                            p.paso(teclas);
                            if (i % 30 == 0) for (Partida.Trozo t : p.visibles()) for (Partida.Peligro h : t.haz) vistos.add(h.k);
                            assertFalse(Double.isNaN(p.hookX) || Double.isNaN(p.hookY), donde + ": anzuelo sin posición");
                            assertTrue(p.hookX >= Partida.HR && p.hookX <= Partida.W - Partida.HR, donde + ": anzuelo fuera de la pantalla");
                        }
                        assertEquals(Partida.Estado.FIN, p.estado, donde + ": la partida no termina");
                        assertTrue(p.profFinal > (b == Bioma.VACIO ? 600 : 100), donde + ": no llega al fondo (" + p.profFinal + ")");
                        for (int id : p.botin) assertFalse(objs.get(id).puff(), "se ha pescado el pez globo hinchable");
                        capturas += p.botin.size();
                    }
        // el pez león de espinas es muy raro (las cuevas del océano cálido casi no le dejan sitio): no se exige
        vistos.add(Partida.K.LION);
        assertEquals(EnumSet.allOf(Partida.K.class), vistos, "no han salido todos los tipos de peligro");
        assertTrue(capturas > 100, "apenas se pesca nada: " + capturas);
    }

    @Test
    void elServidorRepiteLaPartidaDelCliente() throws IOException {
        List<PezDef> peces = TramosParidadTest.peces();
        List<Partida.Objeto> objs = Partida.objetos(peces);
        Set<String> cebos = TramosParidadTest.todosLosCebos(peces);
        int muertes = 0, salidas = 0, objetos = 0;
        for (Bioma b : Bioma.values())
            for (int seed = 1; seed <= 12; seed++) {
                Clima c = Clima.values()[seed % 3];
                double maxD = b == Bioma.VACIO ? Double.POSITIVE_INFINITY : 12 + seed * 5;
                Partida.Ajustes aj = TramosParidadTest.ajustes(seed * 104729 + b.ordinal(), b, Perfil.SubBioma.values()[seed % 5], c, maxD, seed % 4, cebos);
                Grabacion g = new Grabacion();
                Partida cliente = new Partida(aj, objs, true);
                Azar azar = new Azar(seed);
                int max = 60 * 60;
                for (int i = 0; i < max && (cliente.estado == Partida.Estado.CAST || cliente.estado == Partida.Estado.PLAY); i++) {
                    int teclas = bot(cliente, azar);
                    if (cliente.atFloor || i == max - 1) teclas |= Partida.RECOGER;
                    g.add(teclas);
                    cliente.paso(teclas);
                }
                Partida servidor = new Partida(aj, objs, false);
                Grabacion.repetir(servidor, g.bytes());
                String donde = b + " semilla " + seed;
                assertEquals(cliente.botin, servidor.botin, donde + ": botín distinto");
                assertEquals(cliente.causa, servidor.causa, donde + ": final distinto");
                assertEquals(cliente.profFinal, servidor.profFinal, 0.0, donde + ": profundidad distinta");
                assertTrue(servidor.estado == Partida.Estado.FIN || servidor.estado == Partida.Estado.MUERTO, donde + ": el servidor no termina");
                if (servidor.causa != null) muertes++;
                else salidas++;
                objetos += servidor.botin.size();
            }
        assertTrue(muertes > 5 && salidas > 5 && objetos > 20, "la prueba tiene que cubrir muertes, salidas y capturas: " + muertes + " / " + salidas + " / " + objetos);
    }

    @Test
    void sinOrdenDeRecogerSeRecogeDondeEstaba() throws IOException {
        List<PezDef> peces = TramosParidadTest.peces();
        Partida p = new Partida(TramosParidadTest.ajustes(5, Bioma.PANTANO, null, Clima.DESPEJADO, 60, 0, Set.of()), Partida.objetos(peces), false);
        Grabacion g = new Grabacion();
        for (int i = 0; i < 200; i++) g.add(0);
        assertEquals(200, g.pasos());
        Grabacion.repetir(p, g.bytes());
        assertEquals(Partida.Estado.FIN, p.estado);
        assertTrue(p.profFinal > 0.5 && p.profFinal < 10, "profundidad " + p.profFinal);
    }
}
