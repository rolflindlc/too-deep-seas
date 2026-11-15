package com.rolflin.toodeepseas.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cofres del tesoro (como las cajas de pesca de Terraria). Java puro.
 * Un cofre queda definido por cuatro cosas: el bioma donde se pescó, la calidad (la da el tier de la caña),
 * la profundidad a la que estaba y el clima que hacía. Al abrirlo se tira de varias tablas de botín
 * ({@code data/toodeepseas/loot_tables/cofres/...}), una por cada una de esas cuatro cosas.
 */
public final class Cofres {
    private Cofres() {
    }

    /** Los 7 sitios donde se pesca, con los mismos ids que el prototipo. */
    public enum Bioma {
        OCEANO(true, 30, 70),
        CALIDO(true, 30, 70),
        HELADO(true, 30, 70),
        RIO(true, 30, 70),
        PANTANO(true, 30, 70),
        /** Lava del Nether: las bandas coinciden con las zonas del bastión (25 m) y de las fortalezas (60 m). */
        LAVA(false, 25, 60),
        /** Vacío del End: no tiene fondo, las bandas son mucho más largas. */
        VACIO(false, 100, 500);

        /** En lava y Vacío no hay clima. */
        public final boolean hasWeather;
        public final int deepFrom;
        public final int abyssFrom;

        Bioma(boolean hasWeather, int deepFrom, int abyssFrom) {
            this.hasWeather = hasWeather;
            this.deepFrom = deepFrom;
            this.abyssFrom = abyssFrom;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Bioma byId(String id) {
            for (Bioma b : values()) if (b.id().equals(id)) return b;
            return null;
        }
    }

    /** Calidad del cofre: depende del tier de la caña con la que se pescó. */
    public enum Calidad {
        SENCILLO, REFORZADO, ORNAMENTADO, ANCESTRAL;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Madera y cobre: sencillo · hierro, oro y prismarina: reforzado · diamante: ornamentado · netherita y vacío: ancestral. */
        public static Calidad of(RodTier tier) {
            if (tier == null || tier.tier < 2) return SENCILLO;
            if (tier.tier < 3) return REFORZADO;
            return tier.tier < 4 ? ORNAMENTADO : ANCESTRAL;
        }

        public static Calidad byIndex(int i) {
            return values()[Math.max(0, Math.min(values().length - 1, i))];
        }
    }

    /** Banda de profundidad a la que estaba el cofre. */
    public enum Banda {
        SUPERFICIE, PROFUNDO, ABISAL;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Banda of(Bioma bioma, double depth) {
            if (depth >= bioma.abyssFrom) return ABISAL;
            return depth >= bioma.deepFrom ? PROFUNDO : SUPERFICIE;
        }

        public static Banda byIndex(int i) {
            return values()[Math.max(0, Math.min(values().length - 1, i))];
        }
    }

    public enum Clima {
        DESPEJADO, LLUVIA, TORMENTA;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Clima byIndex(int i) {
            return values()[Math.max(0, Math.min(values().length - 1, i))];
        }
    }

    /** Una tabla de botín (ruta dentro de {@code loot_tables}, sin el espacio de nombres) y cuántas veces se tira. */
    public record Tirada(String table, int rolls) {
    }

    /** En lava y Vacío el clima no cuenta. */
    public static Clima climaEfectivo(Bioma bioma, Clima clima) {
        return bioma.hasWeather && clima != null ? clima : Clima.DESPEJADO;
    }

    /**
     * Qué tablas se tiran al abrir un cofre:
     * <ul>
     * <li>la del bioma, de 2 a 5 veces según la calidad;</li>
     * <li>la de profundidad: 1 vez la de «profundo» desde la segunda banda y 2 veces la de «abisal» en la tercera;</li>
     * <li>la del clima: 1 vez la de lluvia si llovía o había tormenta, y 1 vez la de tormenta si la había;</li>
     * <li>1 vez la de la calidad (equipo de pesca del mod).</li>
     * </ul>
     */
    public static List<Tirada> tiradas(Bioma bioma, Calidad calidad, Banda banda, Clima clima) {
        clima = climaEfectivo(bioma, clima);
        List<Tirada> out = new ArrayList<>();
        out.add(new Tirada("cofres/bioma/" + bioma.id(), 2 + calidad.ordinal()));
        if (banda.ordinal() >= Banda.PROFUNDO.ordinal()) out.add(new Tirada("cofres/profundidad/" + bioma.id() + "_profundo", 1));
        if (banda == Banda.ABISAL) out.add(new Tirada("cofres/profundidad/" + bioma.id() + "_abisal", 2));
        if (clima.ordinal() >= Clima.LLUVIA.ordinal()) out.add(new Tirada("cofres/clima/" + bioma.id() + "_lluvia", 1));
        if (clima == Clima.TORMENTA) out.add(new Tirada("cofres/clima/" + bioma.id() + "_tormenta", 1));
        out.add(new Tirada("cofres/calidad/" + calidad.id(), 1));
        return out;
    }

    /** Todas las tablas que puede pedir un cofre (para comprobar que existen). */
    public static List<String> todasLasTablas() {
        List<String> out = new ArrayList<>();
        for (Bioma b : Bioma.values()) {
            out.add("cofres/bioma/" + b.id());
            out.add("cofres/profundidad/" + b.id() + "_profundo");
            out.add("cofres/profundidad/" + b.id() + "_abisal");
            if (b.hasWeather) {
                out.add("cofres/clima/" + b.id() + "_lluvia");
                out.add("cofres/clima/" + b.id() + "_tormenta");
            }
        }
        for (Calidad c : Calidad.values()) out.add("cofres/calidad/" + c.id());
        return out;
    }
}
