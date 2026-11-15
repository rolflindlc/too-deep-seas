package com.rolflin.pesca.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RodStatsTest {
    private static RodStats stats(RodTier tier, FishingMedium medium, double realDepth, int luck, int lure, int depthEnch, Modifier... mods) {
        return RodStats.compute(new RodStats.Input(tier, List.of(mods), luck, lure, depthEnch, medium, realDepth));
    }

    @Test
    void tablaDeTiers() {
        assertEquals(1, RodTier.MADERA.accessorySlots);
        assertEquals(6, RodTier.VACIO.accessorySlots);
        assertEquals(2, RodTier.PRISMARINA.baitSlots);
        assertEquals(22, RodTier.ORO.enchantability);
        int[] depth = {30, 45, 70, 60, 80, 95, 120, 120};
        for (RodTier t : RodTier.values()) assertEquals(depth[t.ordinal()], t.lineDepth);
    }

    @Test
    void nivelMaximoDeAccesorio() {
        int[] max = {1, 1, 2, 2, 2, 3, 3, 3};
        for (RodTier t : RodTier.values()) assertEquals(max[t.ordinal()], t.maxAccessoryLevel(), t.name());
    }

    @Test
    void profundidadEfectiva() {
        assertEquals(10, stats(RodTier.MADERA, FishingMedium.WATER, 1, 0, 0, 0).effectiveDepth);   // mínimo de 10 m
        assertEquals(30, stats(RodTier.MADERA, FishingMedium.WATER, 200, 0, 0, 0).effectiveDepth); // limita el sedal
        assertEquals(25, stats(RodTier.HIERRO, FishingMedium.WATER, 25, 0, 0, 0).effectiveDepth);  // limita el agua
        assertEquals(Double.POSITIVE_INFINITY, stats(RodTier.VACIO, FishingMedium.VOID, 0, 0, 0, 0).effectiveDepth);
    }

    @Test
    void bonusYTopeDeProfundidad() {
        assertEquals(75, stats(RodTier.COBRE, FishingMedium.WATER, 200, 0, 0, 0, new Modifier(Stat.DEPTH, 30)).maxDepth);
        assertEquals(60, stats(RodTier.COBRE, FishingMedium.WATER, 200, 0, 0, 3).maxDepth); // Profundidad III
        assertEquals(120, stats(RodTier.NETHERITA, FishingMedium.WATER, 200, 0, 0, 3, new Modifier(Stat.DEPTH, 30)).maxDepth);
    }

    @Test
    void dondeSePuedePescar() {
        assertFalse(stats(RodTier.DIAMANTE, FishingMedium.LAVA, 50, 0, 0, 0).canFish);
        assertTrue(stats(RodTier.NETHERITA, FishingMedium.LAVA, 50, 0, 0, 0).canFish);
        assertFalse(stats(RodTier.NETHERITA, FishingMedium.VOID, 50, 0, 0, 0).canFish);
        assertTrue(stats(RodTier.VACIO, FishingMedium.VOID, 50, 0, 0, 0).canFish);
        assertTrue(stats(RodTier.VACIO, FishingMedium.LAVA, 50, 0, 0, 0).canFish);
    }

    @Test
    void atraccionYSuerte() {
        double[] r = {0, 12, 18, 24};
        for (int l = 0; l <= 3; l++) assertEquals(r[l], stats(RodTier.HIERRO, FishingMedium.WATER, 50, 0, l, 0).attractR);
        assertEquals(24, stats(RodTier.HIERRO, FishingMedium.WATER, 50, 0, 4, 0).attractR); // IV solo con oro
        assertEquals(32, stats(RodTier.ORO, FishingMedium.WATER, 50, 0, 4, 0).attractR);
        assertEquals(4, stats(RodTier.ORO, FishingMedium.WATER, 50, 4, 0, 0).luck);
        assertEquals(4, stats(RodTier.HIERRO, FishingMedium.WATER, 50, 3, 0, 0, new Modifier(Stat.LUCK, 1)).luck);
        assertEquals(4, stats(RodTier.ORO, FishingMedium.WATER, 50, 4, 0, 0, new Modifier(Stat.LUCK, 2)).luck); // tope IV
    }

    @Test
    void topeDeVelocidadDeBajada() {
        RodStats s = stats(RodTier.NETHERITA, FishingMedium.WATER, 120, 0, 0, 0);
        assertEquals(0.8, s.descentSpeed(0, false, 1), 1e-9);
        assertEquals(0.8 * 4.2, s.descentSpeed(80, false, 1), 1e-9);
        assertEquals(0.8 * 4.2, s.descentSpeed(120, false, 1), 1e-9); // deja de acelerar a 80 m
        assertEquals(4.0, s.descentSpeed(80, true, 1), 1e-9);         // con S sin tope serían 6,4
        assertEquals(4.0, s.descentSpeed(80, false, 1.4), 1e-9);      // franja de rápidos
        RodStats plomada = stats(RodTier.NETHERITA, FishingMedium.WATER, 120, 0, 0, 0, new Modifier(Stat.DESCENT, 0.9));
        assertEquals(0.8 * 1.4, plomada.base, 1e-9);                    // tope de ±40 %
        for (double d = 0; d <= 200; d += 5) assertTrue(plomada.descentSpeed(d, true, 1.4) <= RodStats.MAX_DESCENT_SPEED);
    }

    @Test
    void topesDeAccesorios() {
        RodStats s = stats(RodTier.VACIO, FishingMedium.WATER, 120, 0, 0, 0,
                new Modifier(Stat.HSPEED, 0.36), new Modifier(Stat.HSPEED, 0.36), new Modifier(Stat.HOOK_RADIUS, 6),
                new Modifier(Stat.HOOK_RADIUS, 6), new Modifier(Stat.DUP_CHANCE, 0.3), new Modifier(Stat.DUP_CHANCE, 0.25),
                new Modifier(Stat.MAGNET_RADIUS, 60), new Modifier(Stat.DIVE, 2.8));
        assertEquals(80 * 1.45, s.vh, 1e-9);
        assertEquals(11, s.hookR, 1e-9);
        assertEquals(0.30, s.dupChance, 1e-9);
        assertEquals(48, s.magnetR, 1e-9);
        assertEquals(2.8, s.dive, 1e-9);
        assertEquals(80 * 1.15, stats(RodTier.PRISMARINA, FishingMedium.OCEAN, 50, 0, 0, 0).vh, 1e-9);
        assertEquals(80, stats(RodTier.PRISMARINA, FishingMedium.WATER, 50, 0, 0, 0).vh, 1e-9);
    }

    @Test
    void costeDeDurabilidad() {
        RodStats s = stats(RodTier.HIERRO, FishingMedium.WATER, 50, 0, 0, 0);
        assertEquals(1, s.durabilityCost(0, true, 20, FishingMedium.WATER));
        assertEquals(5, s.durabilityCost(5, true, 20, FishingMedium.WATER));
        assertEquals(3, s.durabilityCost(5, false, 20, FishingMedium.WATER));
        assertEquals(1, s.durabilityCost(0, false, 20, FishingMedium.WATER));
        RodStats seda = stats(RodTier.HIERRO, FishingMedium.WATER, 50, 0, 0, 0, new Modifier(Stat.COST, -0.45));
        assertEquals(3, seda.durabilityCost(5, true, 20, FishingMedium.WATER)); // 5 × 0,55 = 2,75 → 3
        assertEquals(1, seda.durabilityCost(1, false, 20, FishingMedium.WATER)); // mínimo 1
        RodStats v = stats(RodTier.VACIO, FishingMedium.VOID, 0, 0, 0, 0);
        assertEquals(4 + 3, v.durabilityCost(4, true, 350, FishingMedium.VOID)); // Presión del vacío
    }

    @Test
    void ranuras() {
        var sedal = new Loadout.Equipped("sedal_reforzado", Loadout.Category.SEDAL, 1);
        var seda = new Loadout.Equipped("sedal_seda", Loadout.Category.SEDAL, 1);
        var anzuelo = new Loadout.Equipped("anzuelo_afilado", Loadout.Category.ANZUELO, 2);
        var ojo = new Loadout.Equipped("ojo_nautilo", Loadout.Category.AMULETO, 1);
        var perla = new Loadout.Equipped("perla_encantada", Loadout.Category.AMULETO, 1);
        var geoda = new Loadout.Equipped("geoda_resonante", Loadout.Category.AMULETO, 1);
        assertNull(Loadout.validate(RodTier.MADERA, List.of(sedal)));
        assertEquals("ranuras", Loadout.validate(RodTier.MADERA, List.of(sedal, ojo)));
        assertEquals("categoria", Loadout.validate(RodTier.HIERRO, List.of(sedal, seda)));
        assertEquals("nivel", Loadout.validate(RodTier.COBRE, List.of(anzuelo)));
        assertNull(Loadout.validate(RodTier.HIERRO, List.of(anzuelo, ojo, perla)));
        assertEquals("categoria", Loadout.validate(RodTier.DIAMANTE, List.of(ojo, perla, geoda)));
        assertEquals("repetido", Loadout.validate(RodTier.DIAMANTE, List.of(ojo, ojo)));
        assertNotNull(Loadout.validate(RodTier.COBRE, List.of(sedal, ojo, perla)));
        assertTrue(Loadout.canAdd(RodTier.COBRE, List.of(sedal), ojo));
        assertFalse(Loadout.canAdd(RodTier.COBRE, List.of(sedal), seda));
    }
}
