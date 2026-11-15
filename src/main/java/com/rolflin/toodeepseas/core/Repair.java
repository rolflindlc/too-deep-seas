package com.rolflin.toodeepseas.core;

/** Reparación de accesorios en la mesa del pescador: cada unidad de material devuelve el 25 % de los usos y cuesta 1 nivel. */
public final class Repair {
    private Repair() {
    }

    /** Usos que devuelve una unidad de material. */
    public static int perUnit(int maxUses) {
        return Math.max(1, (int) Math.ceil(maxUses * 0.25));
    }

    /** Unidades que se van a gastar: las que hacen falta, sin pasar del material ni de los niveles disponibles. */
    public static int units(int maxUses, int uses, int materials, int levels) {
        if (maxUses <= 0 || uses >= maxUses) return 0;
        int needed = (int) Math.ceil((maxUses - Math.max(0, uses)) / (double) perUnit(maxUses));
        return Math.max(0, Math.min(needed, Math.min(materials, levels)));
    }

    public static int usesAfter(int maxUses, int uses, int units) {
        return Math.min(maxUses, Math.max(0, uses) + units * perUnit(maxUses));
    }
}
