package com.rolflin.toodeepseas.core;

import java.util.List;

/**
 * Convierte el equipo en los parámetros del minijuego. Función pura: cliente y servidor
 * ejecutan este mismo código, así que no hay desincronización.
 */
public final class RodStats {
    // Valores base del prototipo
    public static final double BASE_DESCENT = 0.80;   // bloques/s al empezar
    public static final double ACCEL_PER_BLOCK = 0.04; // aceleración por bloque bajado
    public static final double ACCEL_UNTIL = 80;       // la bajada deja de acelerar a esta profundidad
    public static final double BASE_HSPEED = 80;       // px/s
    public static final double BASE_DIVE = 1.9;        // multiplicador con S
    public static final double BASE_HOOK_RADIUS = 3;   // px
    public static final double MIN_DEPTH = 10;         // el minijuego nunca tiene menos de 10 m

    // Topes totales (con encantamientos y bioma incluidos)
    public static final double MAX_DESCENT_SPEED = 4.0; // bloques/s, con S y franjas incluidas
    public static final double CAP_DEPTH = 120;
    public static final double CAP_HSPEED = 0.45;
    public static final double CAP_DESCENT = 0.40;
    public static final double CAP_HOOK_RADIUS = 8;
    public static final double CAP_MAGNET = 48;
    public static final int CAP_LUCK = 4;
    public static final double CAP_DUP = 0.30;

    /** Radio de Atracción por nivel (I–IV), la mitad que en el prototipo antiguo. */
    private static final double[] LURE_RADIUS = {0, 12, 18, 24, 32};

    /** Lo que el servidor lee de la caña y del mundo al lanzar. */
    public record Input(RodTier tier, List<Modifier> modifiers, int luck, int lure, int depthEnchant,
                        FishingMedium medium) {
    }

    public final RodTier tier;
    /** false si esta caña no puede pescar en ese medio (lava sin netherita, Vacío sin caña del vacío). */
    public final boolean canFish;
    /** Profundidad del sedal con bonus (m). */
    public final double maxDepth;
    /** Profundidad de la partida (m): la del sedal de la caña, sea cual sea la del agua. Infinita en el Vacío. */
    public final double effectiveDepth;
    public final double base;
    public final double k;
    public final double vh;
    public final double dive;
    public final double hookR;
    public final double attractR;
    public final double magnetR;
    public final int luck;
    public final double dupChance;
    public final double costMul;

    private RodStats(Input in) {
        double depth = 0, descent = 0, hspeed = 0, hook = 0, magnet = 0, dup = 0, cost = 0, diveMul = BASE_DIVE;
        int luckBonus = 0;
        for (Modifier m : in.modifiers()) {
            switch (m.stat()) {
                case DEPTH -> depth += m.value();
                case DESCENT -> descent += m.value();
                case HSPEED -> hspeed += m.value();
                case HOOK_RADIUS -> hook += m.value();
                case MAGNET_RADIUS -> magnet = Math.max(magnet, m.value());
                case LUCK -> luckBonus += (int) m.value();
                case DUP_CHANCE -> dup += m.value();
                case DIVE -> diveMul = Math.max(diveMul, m.value());
                case COST -> cost += m.value();
            }
        }
        this.tier = in.tier();
        this.canFish = switch (in.medium()) {
            case LAVA -> tier.canFishLava();
            case VOID -> tier.canFishVoid();
            default -> true;
        };
        // Encantamiento Profundidad: +5 / +10 / +15 m
        this.maxDepth = Math.min(CAP_DEPTH, tier.lineDepth + depth + 5.0 * clamp(in.depthEnchant(), 0, 3));
        this.effectiveDepth = in.medium() == FishingMedium.VOID
                ? Double.POSITIVE_INFINITY
                : Math.max(MIN_DEPTH, maxDepth);
        this.base = BASE_DESCENT * (1 + clamp(descent, -CAP_DESCENT, CAP_DESCENT));
        this.k = ACCEL_PER_BLOCK;
        if (tier == RodTier.PRISMARINA && in.medium() == FishingMedium.OCEAN) hspeed += 0.15;
        this.vh = BASE_HSPEED * (1 + Math.min(CAP_HSPEED, hspeed));
        this.dive = diveMul;
        this.hookR = BASE_HOOK_RADIUS + Math.min(CAP_HOOK_RADIUS, hook);
        int maxEnchant = 3 + tier.enchantLevelBonus();
        this.attractR = LURE_RADIUS[(int) clamp(in.lure(), 0, maxEnchant)];
        this.magnetR = Math.min(CAP_MAGNET, magnet);
        this.luck = Math.min(CAP_LUCK, (int) clamp(in.luck(), 0, maxEnchant) + luckBonus);
        this.dupChance = Math.min(CAP_DUP, dup);
        this.costMul = Math.max(0, 1 + cost);
    }

    public static RodStats compute(Input in) {
        return new RodStats(in);
    }

    /**
     * Velocidad de bajada en bloques/s a una profundidad dada.
     *
     * @param diving  S pulsada
     * @param bandMul multiplicador de la franja en la que está el anzuelo (rápidos, arena de almas...), 1 si ninguna
     */
    public double descentSpeed(double depth, boolean diving, double bandMul) {
        double v = base * (1 + k * Math.min(ACCEL_UNTIL, Math.max(0, depth))) * (diving ? dive : 1) * bandMul;
        return Math.min(MAX_DESCENT_SPEED, v);
    }

    /**
     * Coste de durabilidad de una partida: morir cuesta tantos puntos como objetos (mín. 1);
     * salir o recibir un golpe externo, la mitad redondeando hacia arriba (mín. 1).
     * En el Vacío se suma la Presión del vacío: +1 por cada 100 m alcanzados.
     */
    public int durabilityCost(int items, boolean died, double depthReached, FishingMedium medium) {
        int raw = died ? Math.max(1, items) : Math.max(1, (items + 1) / 2);
        int cost = Math.max(1, (int) Math.ceil(raw * costMul));
        if (medium == FishingMedium.VOID) cost += (int) Math.floor(depthReached / 100.0);
        return cost;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
