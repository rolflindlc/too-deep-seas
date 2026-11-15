package com.rolflin.toodeepseas.core;

import com.rolflin.toodeepseas.core.Cofres.Banda;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Calidad;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CofresTest {
    @Test
    void laCalidadSaleDelTierDeLaCana() {
        assertEquals(Calidad.SENCILLO, Calidad.of(RodTier.MADERA));
        assertEquals(Calidad.SENCILLO, Calidad.of(RodTier.COBRE));
        assertEquals(Calidad.REFORZADO, Calidad.of(RodTier.HIERRO));
        assertEquals(Calidad.REFORZADO, Calidad.of(RodTier.ORO));
        assertEquals(Calidad.REFORZADO, Calidad.of(RodTier.PRISMARINA));
        assertEquals(Calidad.ORNAMENTADO, Calidad.of(RodTier.DIAMANTE));
        assertEquals(Calidad.ANCESTRAL, Calidad.of(RodTier.NETHERITA));
        assertEquals(Calidad.ANCESTRAL, Calidad.of(RodTier.VACIO));
        assertEquals(Calidad.SENCILLO, Calidad.of(null));
    }

    @Test
    void lasBandasDependenDelBioma() {
        assertEquals(Banda.SUPERFICIE, Banda.of(Bioma.OCEANO, 29.9));
        assertEquals(Banda.PROFUNDO, Banda.of(Bioma.OCEANO, 30));
        assertEquals(Banda.ABISAL, Banda.of(Bioma.RIO, 70));
        assertEquals(Banda.PROFUNDO, Banda.of(Bioma.LAVA, 25));
        assertEquals(Banda.ABISAL, Banda.of(Bioma.LAVA, 60));
        assertEquals(Banda.SUPERFICIE, Banda.of(Bioma.VACIO, 99));
        assertEquals(Banda.PROFUNDO, Banda.of(Bioma.VACIO, 499));
        assertEquals(Banda.ABISAL, Banda.of(Bioma.VACIO, 500));
    }

    @Test
    void elCofreMasSimpleTiraDosVecesDelBiomaYUnaDeLaCalidad() {
        List<Cofres.Tirada> t = Cofres.tiradas(Bioma.RIO, Calidad.SENCILLO, Banda.SUPERFICIE, Clima.DESPEJADO);
        assertEquals(List.of(new Cofres.Tirada("cofres/bioma/rio", 2), new Cofres.Tirada("cofres/calidad/sencillo", 1)), t);
    }

    @Test
    void elMejorCofreSumaProfundidadYClima() {
        List<Cofres.Tirada> t = Cofres.tiradas(Bioma.OCEANO, Calidad.ANCESTRAL, Banda.ABISAL, Clima.TORMENTA);
        assertEquals(List.of(
                new Cofres.Tirada("cofres/bioma/oceano", 5),
                new Cofres.Tirada("cofres/profundidad/oceano_profundo", 1),
                new Cofres.Tirada("cofres/profundidad/oceano_abisal", 2),
                new Cofres.Tirada("cofres/clima/oceano_lluvia", 1),
                new Cofres.Tirada("cofres/clima/oceano_tormenta", 1),
                new Cofres.Tirada("cofres/calidad/ancestral", 1)), t);
    }

    @Test
    void enLavaYVacioNoHayClima() {
        for (Bioma b : new Bioma[]{Bioma.LAVA, Bioma.VACIO}) {
            assertEquals(Cofres.tiradas(b, Calidad.REFORZADO, Banda.PROFUNDO, Clima.DESPEJADO),
                    Cofres.tiradas(b, Calidad.REFORZADO, Banda.PROFUNDO, Clima.TORMENTA));
        }
    }

    @Test
    void todasLasCombinacionesPidenTablasDeLaLista() {
        Set<String> known = new HashSet<>(Cofres.todasLasTablas());
        assertEquals(7 * 3 + 5 * 2 + 4, known.size());
        for (Bioma b : Bioma.values()) for (Calidad c : Calidad.values()) for (Banda d : Banda.values()) for (Clima w : Clima.values()) {
            for (Cofres.Tirada t : Cofres.tiradas(b, c, d, w)) {
                assertTrue(known.contains(t.table()), t.table());
                assertTrue(t.rolls() >= 1);
            }
        }
    }
}
