package com.rolflin.toodeepseas.core;

/** Las 8 cañas del documento de progresión. Java puro: no depende de Minecraft. */
public enum RodTier {
    //            tier  ranuras cebos sedal(m) durab. encantab.
    MADERA      (0.0,  1,     1,    30,      64,    1),
    COBRE       (1.0,  2,     1,    45,      160,   8),
    HIERRO      (2.0,  3,     1,    70,      320,   10),
    ORO         (2.0,  2,     1,    60,      120,   22),
    PRISMARINA  (2.5,  3,     2,    80,      450,   12),
    DIAMANTE    (3.0,  4,     1,    95,      900,   10),
    NETHERITA   (4.0,  5,     1,    120,     1800,  15),
    VACIO       (5.0,  6,     1,    120,     2500,  15);

    public final double tier;
    public final int accessorySlots;
    public final int baitSlots;
    public final int lineDepth;
    public final int durability;
    public final int enchantability;

    RodTier(double tier, int accessorySlots, int baitSlots, int lineDepth, int durability, int enchantability) {
        this.tier = tier;
        this.accessorySlots = accessorySlots;
        this.baitSlots = baitSlots;
        this.lineDepth = lineDepth;
        this.durability = durability;
        this.enchantability = enchantability;
    }

    /** Nivel máximo de accesorio: min(III, max(I, T)). */
    public int maxAccessoryLevel() {
        return Math.min(3, Math.max(1, (int) Math.floor(tier)));
    }

    /** Netherita y Vacío pueden pescar en la lava del Nether. */
    public boolean canFishLava() {
        return this == NETHERITA || this == VACIO;
    }

    /** Solo la caña del vacío pesca en el Vacío del End. */
    public boolean canFishVoid() {
        return this == VACIO;
    }

    /** Oro: los encantamientos de la mesa salen 1 nivel por encima del máximo normal. */
    public int enchantLevelBonus() {
        return this == ORO ? 1 : 0;
    }

    /** Netherita y Vacío: el magma del agua (géiser y piscinas) solo empuja. */
    public boolean magmaImmune() {
        return this == NETHERITA || this == VACIO;
    }
}
