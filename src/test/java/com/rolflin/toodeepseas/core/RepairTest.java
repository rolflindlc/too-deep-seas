package com.rolflin.toodeepseas.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepairTest {
    @Test
    void cadaUnidadDevuelveUnCuarto() {
        assertEquals(10, Repair.perUnit(40));
        assertEquals(150, Repair.perUnit(600));
        assertEquals(1, Repair.perUnit(3));  // escama de guardián
        assertEquals(4, Repair.perUnit(15)); // silbato: se redondea hacia arriba
    }

    @Test
    void comoMuchoCuatroUnidadesReparanDelTodo() {
        for (int max : new int[]{3, 15, 25, 40, 90, 600, 1300}) {
            int units = Repair.units(max, 0, 64, 99);
            assertTrue(units >= 3 && units <= 4, "max " + max + ": " + units);
            assertEquals(max, Repair.usesAfter(max, 0, units), "max " + max);
        }
    }

    @Test
    void soloSeGastaLoQueHaceFalta() {
        assertEquals(0, Repair.units(40, 40, 64, 99));
        assertEquals(1, Repair.units(40, 35, 64, 99));
        assertEquals(40, Repair.usesAfter(40, 35, 1));
        assertEquals(2, Repair.units(40, 15, 2, 99));  // limita el material
        assertEquals(1, Repair.units(40, 15, 64, 1));  // limitan los niveles
        assertEquals(0, Repair.units(40, 15, 64, 0));
        assertEquals(25, Repair.usesAfter(40, 15, 1));
    }
}
