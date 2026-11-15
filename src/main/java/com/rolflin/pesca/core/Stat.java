package com.rolflin.pesca.core;

/** Estadísticas que puede tocar un accesorio. Los accesorios en sí llegan por datapack (fase 3). */
public enum Stat {
    /** Metros que se suman a la profundidad del sedal. */
    DEPTH,
    /** Fracción que se suma a la velocidad base de bajada (+0,2 = +20 %). */
    DESCENT,
    /** Fracción que se suma a la velocidad horizontal. */
    HSPEED,
    /** Píxeles que se suman al radio de captura. */
    HOOK_RADIUS,
    /** Radio del imán de tesoros, en píxeles (se queda el mayor). */
    MAGNET_RADIUS,
    /** Niveles efectivos de Suerte marina. */
    LUCK,
    /** Probabilidad de captura doble (0–1). */
    DUP_CHANCE,
    /** Multiplicador de bajada rápida con S (se queda el mayor). */
    DIVE,
    /** Fracción que se suma al coste de durabilidad (−0,3 = −30 %). */
    COST
}
