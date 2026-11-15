package com.rolflin.toodeepseas.core.juego;

import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.RodStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Terreno de una partida del minijuego: el túnel por el que baja el anzuelo y sus cuevas. Java puro.
 * Es el mismo generador del prototipo web ({@code baseTunnel}, {@code genSections}, {@code secSpans}, {@code tunnelAt}),
 * portado operación por operación: una prueba compara sus resultados con los del prototipo.
 * Las coordenadas van en píxeles de la pantalla del minijuego (176 de ancho, 24 px por bloque); {@code y} crece hacia abajo.
 */
public final class Terreno {
    /** Ancho de la pantalla del minijuego (px). */
    public static final double W = 176;
    /** Alto de la pantalla del minijuego (px). */
    public static final double H = 166;
    /** Píxeles por bloque. */
    public static final double PPB = 24;

    /** Parámetros de movimiento de los que depende la forma de las cuevas (para que siempre se puedan recorrer). */
    public record Movimiento(double base, double k, double vh, double dive) {
        public static final Movimiento PROTOTIPO = new Movimiento(RodStats.BASE_DESCENT, RodStats.ACCEL_PER_BLOCK, RodStats.BASE_HSPEED, RodStats.BASE_DIVE);

        public static Movimiento de(RodStats stats) {
            return new Movimiento(stats.base, stats.k, stats.vh, stats.dive);
        }

        /** Velocidad de bajada sin tope (px/s) a la altura {@code y}. */
        public double descentSpd(double y) {
            return base * (1 + k * Math.min(RodStats.ACCEL_UNTIL, Math.max(0, y / PPB))) * PPB;
        }

        /** Velocidad de bajada real (px/s): con S y franja, y con el tope de 4 bloques/s. */
        public double fallSpd(double y, boolean diving, double mul) {
            return Math.min(RodStats.MAX_DESCENT_SPEED * PPB, descentSpd(y) * (diving ? dive : 1) * mul);
        }

        /** Velocidad lateral (px/s): crece con la de bajada para que todo siga siendo esquivable. */
        public double vhAt(double spd) {
            return vh * Math.max(1, spd / 50);
        }
    }

    /** Pilar dentro de una cueva. */
    public record Pilar(double u0, double u1, double f, double w) {
    }

    /** Tramo de cueva: entre {@code y0} e {@code y1} el túnel se parte en uno o varios caminos. */
    public static final class Seccion {
        public final double y0, y1, cw, ex, xx, fi, fo, wm, rough;
        public final int sd;
        /** Cada camino es una lista de puntos {u (0–1 a lo largo del tramo), x}. */
        public final List<double[][]> paths;
        public final List<Pilar> pil = new ArrayList<>();

        Seccion(double y0, double y1, double cw, List<double[][]> paths, double ex, double xx, double fi, double fo, int sd, double wm, double rough) {
            this.y0 = y0;
            this.y1 = y1;
            this.cw = cw;
            this.paths = paths;
            this.ex = ex;
            this.xx = xx;
            this.fi = fi;
            this.fo = fo;
            this.sd = sd;
            this.wm = wm;
            this.rough = rough;
        }
    }

    /**
     * El túnel a una altura: borde izquierdo y derecho, centro y anchura. Dentro de una cueva, {@code iv} son los huecos
     * abiertos (uno por camino) y {@code sec} la cueva.
     */
    public record Tramo(double l, double r, double cx, double w, double[][] iv, Seccion sec) {
    }

    public final int seed;
    public final Perfil perfil;
    /** Profundidad de la partida en bloques (infinita en el Vacío). */
    public final double maxD;
    public final Movimiento mov;
    /** Bioma de agua que tiene zona de magma desde 95 m (todos menos el helado). */
    public final boolean zonaMagma;
    public final List<Seccion> sections;

    public Terreno(int seed, Bioma bioma, Perfil.SubBioma sub, double maxD, Movimiento mov) {
        this(seed, Perfil.de(bioma, sub), bioma.hasWeather && bioma != Bioma.HELADO, maxD, mov);
    }

    public Terreno(int seed, Perfil perfil, boolean zonaMagma, double maxD, Movimiento mov) {
        this.seed = seed;
        this.perfil = perfil;
        this.zonaMagma = zonaMagma;
        this.maxD = maxD;
        this.mov = mov;
        this.sections = genSections();
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double sm(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    private static Tramo tramo(double l, double r) {
        return new Tramo(l, r, (l + r) / 2, r - l, null, null);
    }

    /** El túnel sin cuevas: ancho arriba y más estrecho y sinuoso al bajar. */
    public Tramo baseTunnel(double y) {
        double d = y / PPB;
        Perfil p = perfil;
        if (p.open() || d < 2.5) return new Tramo(-10, W + 10, W / 2, W + 20, null, null);
        double open = Math.min(1, (d - 2.5) / 2.5);
        double w = Math.max(p.openW(), 150 - (d - 2.5) * 2.2) + (Azar.vnoise(seed + 7, y / 40) - 0.5) * 14;
        if (p.banks()) w = lerp(W * (0.6 + 0.2 * Azar.vnoise(seed + 41, y / 120)), w, sm((d - 17) / 6));
        w = lerp(W + 20, w, open);
        double amp = Math.max(0, (W - w) / 2 - 4) * Math.min(1, (d - 2.5) / 12);
        double cx = W / 2 + (Azar.vnoise(seed + 3, y / 200) - 0.5) * 2 * amp;
        double wl = (Azar.vnoise(seed + 11, y / 9) - 0.5) * 4 * open, wr = (Azar.vnoise(seed + 13, y / 9) - 0.5) * 4 * open;
        double wideL = Math.max(0, Azar.vnoise(seed + 31, y / 160) - 0.55) * 2.2 * 45 * open;
        double wideR = Math.max(0, Azar.vnoise(seed + 37, y / 160) - 0.55) * 2.2 * 45 * open;
        double l = Math.max(2, cx - w / 2 + wl - wideL), r = Math.min(W - 2, cx + w / 2 + wr + wideR);
        return tramo(l, r);
    }

    private static Tramo blendT(Tramo t, double cx, double g, double f) {
        return tramo(lerp(t.l(), cx - g / 2, f), lerp(t.r(), cx + g / 2, f));
    }

    private static double pathX(double[][] p, double u) {
        if (u <= 0) return p[0][1];
        if (u >= 1) return p[p.length - 1][1];
        int k = 0;
        while (k < p.length - 2 && u > p[k + 1][0]) k++;
        double[] a = p[k], b = p[k + 1];
        double den = b[0] - a[0];
        double f = (u - a[0]) / (den == 0 ? 1 : den);
        return lerp(a[1], b[1], Math.max(0, Math.min(1, f)));
    }

    /** Huecos abiertos de una cueva a la altura {@code y}, de izquierda a derecha. */
    public static double[][] secSpans(Seccion s, double y) {
        double u = Math.max(0, Math.min(1, (y - s.y0) / (s.y1 - s.y0))), fade = Math.sin(Math.PI * u);
        double rg = s.rough, wob = (Azar.vnoise(s.sd + 1, y / 40) - 0.5) * 8 * fade * Math.min(1, rg + 0.4);
        double bul = s.wm > 1 ? Math.pow(fade, 0.6) * (s.wm - 1) * s.cw : 0;
        double half = (s.cw + bul + (Azar.vnoise(s.sd + 2, y / 25) - 0.5) * 6 * fade) / 2;
        double n1 = (Azar.vnoise(s.sd + 3, y / 12) - 0.5) * 4 * fade * Math.min(1, rg);
        double n2 = (Azar.vnoise(s.sd + 4, y / 12) - 0.5) * 4 * fade * Math.min(1, rg);
        if (rg > 1) { // pared rugosa: solo hacia fuera, nunca estrecha el paso
            n1 = -Math.abs(Azar.vnoise(s.sd + 3, y / 5) - 0.5) * 4 * rg * fade;
            n2 = Math.abs(Azar.vnoise(s.sd + 4, y / 5) - 0.5) * 4 * rg * fade;
        }
        List<double[]> out = new ArrayList<>();
        double du = 14 / (s.y1 - s.y0);
        for (double[][] p : s.paths) {
            // esquinas redondeadas: media del recorrido en una ventana de ±14 px
            double cx = (pathX(p, u - du) + pathX(p, u) + pathX(p, u + du) + pathX(p, u - du / 2) + pathX(p, u + du / 2)) / 5 + wob;
            out.add(new double[]{Math.max(2, cx - half + n1), Math.min(W - 2, cx + half + n2)});
        }
        out.sort(Comparator.comparingDouble(a -> a[0]));
        List<double[]> m = new ArrayList<>();
        m.add(out.get(0).clone());
        for (int i = 1; i < out.size(); i++) {
            double[] last = m.get(m.size() - 1);
            if (out.get(i)[0] <= last[1] + 6) last[1] = Math.max(last[1], out.get(i)[1]); // sin láminas finas de roca entre caminos
            else m.add(out.get(i).clone());
        }
        // pilares: parten el hueco más ancho dejando paso por los dos lados
        for (Pilar p : s.pil) {
            if (u <= p.u0() || u >= p.u1()) continue;
            double[] big = m.get(0);
            for (double[] q : m) if (q[1] - q[0] > big[1] - big[0]) big = q;
            double pw = Math.min(p.w() * Math.sqrt(Math.sin(Math.PI * (u - p.u0()) / (p.u1() - p.u0()))), big[1] - big[0] - 28);
            if (pw < 1.5) continue;
            double a = Math.max(big[0] + 14, Math.min(big[1] - 14 - pw, big[0] + (big[1] - big[0]) * p.f() - pw / 2));
            int at = m.indexOf(big);
            m.set(at, new double[]{big[0], a});
            m.add(at + 1, new double[]{a + pw, big[1]});
        }
        return m.toArray(new double[0][]);
    }

    /** El túnel a la altura {@code y}, con cuevas y sus embudos de entrada y salida. */
    public Tramo tunnelAt(double y) {
        for (Seccion s : sections) {
            if (y < s.y0 - s.fi) break;
            if (y > s.y1 + s.fo) continue;
            if (y < s.y0) return blendT(baseTunnel(y), s.ex, s.cw, sm((y - (s.y0 - s.fi)) / s.fi));
            if (y <= s.y1) {
                double[][] iv = secSpans(s, y);
                double l = iv[0][0], r = iv[iv.length - 1][1];
                return new Tramo(l, r, (l + r) / 2, r - l, iv, s);
            }
            return blendT(baseTunnel(y), s.xx, s.cw, 1 - sm((y - s.y1) / s.fo));
        }
        return baseTunnel(y);
    }

    /** Huecos abiertos a la altura {@code y}: uno fuera de las cuevas, uno por camino dentro. */
    public double[][] spansAt(double y) {
        Tramo t = tunnelAt(y);
        return t.iv() != null ? t.iv() : new double[][]{{t.l(), t.r()}};
    }

    /** El hueco más cercano a {@code x}. */
    public double[] spanAt(double x, double y) {
        double[] best = null;
        double bd = 1e9;
        for (double[] s : spansAt(y)) {
            double d = x < s[0] ? s[0] - x : x > s[1] ? x - s[1] : 0;
            if (d < bd) {
                bd = d;
                best = s;
            }
        }
        return best;
    }

    /** ¿Cabe un círculo de radio {@code m} en ({@code x}, {@code y}) sin tocar roca? */
    public boolean inOpen(double x, double y, double m) {
        for (double[] s : spansAt(y)) if (x - m >= s[0] && x + m <= s[1]) return true;
        return false;
    }

    /** ¿Está {@code y} dentro de una cueva o de su entrada o salida (con margen)? Ahí nunca hay peligros. */
    public boolean inSafe(double y, double m) {
        for (Seccion s : sections) if (y > s.y0 - s.fi - 10 - m && y < s.y1 + s.fo + 10 + m) return true;
        return false;
    }

    // Tramos de cueva en diagonal: acompañan la bajada; los profundos hacen zigzag y a veces hay varios caminos
    private List<Seccion> genSections() {
        Perfil p = perfil;
        List<Seccion> out = new ArrayList<>();
        if (p.none()) return out;
        Azar r = new Azar(Azar.hash2(seed, 777));
        double d = Math.max(12, p.start()) + r.next() * 6 + (p.freq() < 1 ? (1 / p.freq() - 1) * 12 : 0);
        for (int guard = 0; guard < 90; guard++) {
            boolean magma = d >= 95 && zonaMagma; // zona de magma: más cuevas, más estrechas y con más zigzag
            double cw = Math.max(19, Math.max(22, 40 - d * 0.2) * (magma ? 0.8 : 1)), len = p.len() * (p.vert() ? 0.45 : 1), tall = p.vert() ? 2 : 1;
            // pendiente máxima segura según lo rápido que bajas y lo rápido que te mueves de lado (peor caso: con S)
            double ratio = Math.min(50, mov.fallSpd((d + 15) * PPB, true, 1)) / mov.vh();
            double slope = Math.max(0.3, Math.min(1.6, 0.6 / ratio - 0.35));
            double full = W - 20 - cw - r.next() * 14, dx = Math.min(full, slope * 168 * len), off = r.next() * (full - dx);
            boolean startLeft = baseTunnel(d * PPB - 100).cx() < W / 2;
            double left = 10 + cw / 2 + (startLeft ? off : full - dx - off), right = left + dx;
            double a = startLeft ? left : right, b = startLeft ? right : left;
            double roll = r.next(), alt = r.next(), alt2 = r.next();
            int np = p.pathsMin() + (r.next() < 0.5 ? p.pathsMax() - p.pathsMin() : 0);
            List<double[][]> paths = new ArrayList<>();
            double h;
            if (d >= 25 && roll < Math.min(0.85, 0.5 * p.zig() * (magma ? 1.4 : 1))) {
                // zigzag compacto: tramos cortos que cruzan de lado a lado
                int legs = d >= 75 && alt2 < 0.4 ? 4 : (d >= 45 && alt2 < 0.6 ? 3 : 2);
                double dz = Math.min(dx, slope * 105 * Math.max(0.6, len)), az = a, bz = a < b ? a + dz : a - dz;
                double[][] pts = new double[legs + 1][];
                for (int i = 0; i <= legs; i++) pts[i] = new double[]{(double) i / legs, i % 2 != 0 ? bz : az};
                h = legs * dz / slope;
                paths.add(pts);
                if (legs == 2 && alt < 0.5) paths.add(new double[][]{{0, az}, {1, az}}); // atajo pegado a la pared
            } else if (np >= 2) {
                // varios caminos: una diagonal suave y otros que bajan rectos y cruzan en tramos distintos
                int q = Math.max(2, np - 1), st = (int) Math.floor(alt * q);
                h = q * dx / slope * tall;
                paths.add(new double[][]{{0, a}, {1, b}});
                for (int i = 0; i < np - 1; i++) {
                    int k = (st + i) % q;
                    List<double[]> pp = new ArrayList<>();
                    pp.add(new double[]{0, a});
                    if (k > 0) pp.add(new double[]{(double) k / q, a});
                    pp.add(new double[]{(double) (k + 1) / q, b});
                    if (k + 1 < q) pp.add(new double[]{1, b});
                    paths.add(pp.toArray(new double[0][]));
                }
            } else {
                h = dx / slope * tall;
                paths.add(new double[][]{{0, a}, {1, b}});
            }
            if (d * PPB + h > (maxD - 5) * PPB && paths.size() + paths.get(0).length > 2) {
                h = dx / slope;
                paths = new ArrayList<>();
                paths.add(new double[][]{{0, a}, {1, b}});
            }
            double y0 = d * PPB, y1 = y0 + h, ex = a;
            double[][] first = paths.get(0);
            double xx = first[first.length - 1][1];
            if (y1 > (maxD - 5) * PPB) break;
            double fi = Math.max(70, Math.min(200, shift(baseTunnel(y0 - 120), ex, cw) * 1.6 * ratio + 30));
            double fo = Math.max(50, Math.min(200, shift(baseTunnel(y1 + 120), xx, cw) * 1.6 * ratio + 20));
            if (!out.isEmpty()) {
                Seccion last = out.get(out.size() - 1);
                if (y0 - fi < last.y1 + last.fo + 20) {
                    d += 4;
                    continue;
                }
            }
            Seccion sec = new Seccion(y0, y1, cw, paths, ex, xx, fi, fo, Azar.hash2(seed, 900 + out.size()), p.wm(), p.rough());
            if (p.pillars() && y1 - y0 > 110) {
                int n = 1 + (r.next() < 0.5 ? 1 : 0);
                for (int i = 0; i < n; i++) {
                    double u0 = 0.12 + r.next() * 0.5;
                    double u1 = Math.min(0.9, u0 + 0.18 + r.next() * 0.2);
                    double f = 0.35 + r.next() * 0.3;
                    double w = 7 + r.next() * 6;
                    sec.pil.add(new Pilar(u0, u1, f, w));
                }
            }
            out.add(sec);
            // más espacio abierto en el abismo (territorio del monstruo)
            d = (y1 + fo) / PPB + (6 + r.next() * 8) / Math.pow(p.freq() * (magma ? 1.3 : 1), 1.5) + (d >= 62 ? 10 : 0);
        }
        return out;
    }

    private static double shift(Tramo t, double c, double cw) {
        return Math.max(Math.abs(t.l() - (c - cw / 2)), Math.abs(t.r() - (c + cw / 2)));
    }
}
