package com.rolflin.toodeepseas.core.juego;

import com.rolflin.toodeepseas.core.Cofres.Bioma;

/**
 * Perfil de terreno de un bioma ({@code TERRAIN} en el prototipo): cuántas cuevas tiene y qué forma.
 *
 * @param freq     frecuencia de cuevas
 * @param len      longitud de las cuevas
 * @param wm       ensanche en el centro (cavernas con cuello estrecho)
 * @param zig      frecuencia de zigzags
 * @param pathsMin número mínimo de caminos
 * @param pathsMax número máximo de caminos
 * @param rough    rugosidad de las paredes
 * @param pillars  pilares dentro de las cuevas
 * @param start    profundidad de la primera cueva (m)
 * @param vert     cuevas largas y verticales
 * @param banks    orillas de río entre 0 y 20 m
 * @param openW    anchura mínima del campo abierto (px)
 * @param none     sin cuevas
 * @param open     sin paredes (Vacío)
 */
public record Perfil(double freq, double len, double wm, double zig, int pathsMin, int pathsMax, double rough,
                     boolean pillars, double start, boolean vert, boolean banks, double openW, boolean none, boolean open) {

    /** Sub-biomas del Nether: en la lava hacen el papel del clima. */
    public enum SubBioma {
        YERMOS, DELTAS, ALMAS, CARMESI, DISTORSIONADO;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static SubBioma byId(String id) {
            for (SubBioma s : values()) if (s.id().equals(id)) return s;
            return null;
        }
    }

    private static Perfil cuevas(double freq, double len, double wm, double zig, int pMin, int pMax, double rough) {
        return new Perfil(freq, len, wm, zig, pMin, pMax, rough, false, 0, false, false, 48, false, false);
    }

    private Perfil conPilares() {
        return new Perfil(freq, len, wm, zig, pathsMin, pathsMax, rough, true, start, vert, banks, openW, none, open);
    }

    public static final Perfil OCEANO = cuevas(1, 1, 1, 1, 2, 3, 1);
    public static final Perfil CALIDO = cuevas(1.6, 0.5, 1, 1, 3, 4, 2.2);
    public static final Perfil HELADO = cuevas(0.5, 1, 1.6, 0.6, 1, 2, 0.3).conPilares();
    public static final Perfil RIO = new Perfil(0.6, 1, 1, 0.4, 1, 2, 1, false, 20, true, true, 48, false, false);
    public static final Perfil PANTANO = new Perfil(1, 1, 1, 1, 2, 3, 1, false, 0, false, false, 112, true, false);
    public static final Perfil DELTAS = cuevas(1.8, 1, 1, 1.3, 4, 5, 1).conPilares();
    public static final Perfil ALMAS = new Perfil(0.3, 1, 1, 0.5, 1, 2, 1, false, 0, false, false, 120, false, false);
    public static final Perfil CARMESI = cuevas(1, 1, 1, 1, 2, 3, 1.4);
    public static final Perfil DISTORSIONADO = cuevas(1, 1, 1, 1, 2, 3, 1.4);
    public static final Perfil YERMOS = cuevas(1, 1, 1, 1, 2, 3, 1);
    public static final Perfil VACIO = new Perfil(1, 1, 1, 1, 2, 3, 1, false, 0, false, false, 48, true, true);

    /** Perfil de un bioma de pesca; en la lava lo decide el sub-bioma (yermos si no se sabe). */
    public static Perfil de(Bioma bioma, SubBioma sub) {
        return switch (bioma) {
            case OCEANO -> OCEANO;
            case CALIDO -> CALIDO;
            case HELADO -> HELADO;
            case RIO -> RIO;
            case PANTANO -> PANTANO;
            case VACIO -> VACIO;
            case LAVA -> switch (sub == null ? SubBioma.YERMOS : sub) {
                case DELTAS -> DELTAS;
                case ALMAS -> ALMAS;
                case CARMESI -> CARMESI;
                case DISTORSIONADO -> DISTORSIONADO;
                case YERMOS -> YERMOS;
            };
        };
    }
}
