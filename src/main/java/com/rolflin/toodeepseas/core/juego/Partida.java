package com.rolflin.toodeepseas.core.juego;

import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import com.rolflin.toodeepseas.core.juego.Perfil.SubBioma;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Una partida del minijuego: genera el mundo por tramos ({@code genChunk} del prototipo) y lo simula a paso fijo
 * ({@code update}, {@code updHaz}, {@code updItem}, {@code checkHit}). Java puro y determinista: con los mismos ajustes
 * y las mismas teclas, cliente y servidor obtienen exactamente la misma partida, así que el servidor la repite para
 * validar el botín. Todo el azar sale de la semilla y las funciones trigonométricas son las de {@link StrictMath}.
 */
public final class Partida {
    public static final double W = Terreno.W, H = Terreno.H, PPB = Terreno.PPB;
    /** Alto de un tramo de generación (px). */
    public static final double CH = 160;
    /** Paso fijo de la simulación (s). */
    public static final double DT = 1.0 / 60;
    /** Radio del anzuelo para chocar (px). El radio de captura puede ser mayor ({@link Ajustes#hookR}). */
    public static final double HR = 3;
    /** Teclas de un paso. */
    public static final int IZQ = 1, DER = 2, BAJAR = 4, RECOGER = 8;
    private static final int[] ZONES = {0, 20, 45, 70, 95};
    private static final int[] LAVA_Z = {0, 25, 60, 95};

    /** Lo que el servidor mide al lanzar y envía al cliente. */
    public record Ajustes(int seed, Bioma bioma, SubBioma sub, Clima clima, double maxD, double base, double k, double vh, double dive,
                          double hookR, int luck, int lure, double attractR, double magnetR, Set<String> cebos) {
        public Ajustes {
            if (!bioma.hasWeather) clima = Clima.DESPEJADO; // en el Nether y el End no hay clima
            if (sub == null) sub = SubBioma.YERMOS;
            cebos = Set.copyOf(cebos);
        }
    }

    /** Algo que puede aparecer: un pez, un tesoro o (los dos nulos) el pez globo hinchable, que solo es un obstáculo. */
    public record Objeto(int rarity, int minDepth, PezDef pez, Catalogo.Tesoro tesoro) {
        public boolean esPez() {
            return tesoro == null;
        }

        public boolean puff() {
            return pez == null && tesoro == null;
        }

        public PezDef.Mov mov() {
            return pez != null ? pez.movement() : PezDef.Mov.VERTICAL;
        }
    }

    /** La lista de objetos en el orden del prototipo: peces por su {@code order}, los tesoros y el pez globo hinchable. */
    public static List<Objeto> objetos(Collection<PezDef> peces) {
        List<PezDef> sorted = new ArrayList<>(peces);
        sorted.sort((a, b) -> a.order() != b.order() ? Integer.compare(a.order(), b.order()) : a.id().compareTo(b.id()));
        List<Objeto> out = new ArrayList<>();
        for (PezDef p : sorted) out.add(new Objeto(p.rarity(), p.minDepth(), p, null));
        for (Catalogo.Tesoro t : Catalogo.TESOROS) out.add(new Objeto(t.rarity(), t.minDepth(), null, t));
        out.add(new Objeto(0, 0, null, null));
        return List.copyOf(out);
    }

    public enum Estado {CAST, PLAY, REEL, FIN, MUERTO}

    /** Por qué has muerto: {@code clave} para el texto y {@code tipo} para la animación. */
    public record Causa(String clave, String tipo) {
    }

    /** Peligros a la deriva. */
    public enum Deriva {
        MEDUSA("medusa", "shock", 4.5, false, false, false, 0xE0B8FF), MEDUSA_U("medusa_urticante", "shock", 4.5, false, false, false, 0xFF8AD8),
        TEMPANO("tempano", "freeze", 5, false, false, false, 0xEAF6FF), TRONCO("tronco", "log", 5, false, false, false, 0x7A5530),
        MAGMA("cubo_magma", "burn", 4.5, false, false, true, 0xB8381A), CHISPA("chispa", "burn", 2.2, false, false, false, 0xFFD060),
        ALMA("llama_alma", "soul", 3.5, false, false, false, 0x5FE0F0), ISLA("isla", "rock", 7, true, false, false, 0xD4D69A),
        NUBE("nube", "burn", 17, true, true, false, 0xD060E0);
        public final String clave, tipo;
        public final double r;
        public final boolean wide, slow, bounce;
        public final int color;

        Deriva(String clave, String tipo, double r, boolean wide, boolean slow, boolean bounce, int color) {
            this.clave = clave;
            this.tipo = tipo;
            this.r = r;
            this.wide = wide;
            this.slow = slow;
            this.bounce = bounce;
            this.color = color;
        }
    }

    public enum RocaTipo {ROCA, ERIZO, CRISTAL, TRONCO, RAIZF, FOSIL, CORALF, ANEMONA}

    public enum Fx {PUSH, SLOWX, BLIZZARD, FAST, SLOWY, FOG, INVERT}

    public enum K {BUB, DRIFT, NET, SEEK, CROSS, GEY, COL, FALL, LION, LUNGE}

    public static final int WAIT = 0, WARN = 1, GO = 2, IDLE = 3, ON = 4, OUT = 5, HOLD = 6, BACK = 7, SHOOT = 8, HIDE = 9;
    public static final int CALM = 1, PWARN = 2, BIG = 3;

    public static final class Roca {
        public double x, y, r;
        public int s;
        public RocaTipo kind;
    }

    public static final class Franja {
        public double y, h, f, m;
        public int dir;
        public Fx fx;
    }

    public static final class Decor {
        public double x, y;
        public int side, s, z;
    }

    public static final class Peligro {
        public K k;
        public double x, y, yy, r, v, life, x0, amp, ph, sp, tm, tw, dx, dy, range, emit, hh, w, wx, ext;
        public boolean act, dead, met, bul, ghast, lava, hot, chim, stay;
        public int st, side, dir;
        public Deriva def;
        /** Chorros del géiser o espinas del pez león: {x, y, vx, vy, r, d}. */
        public List<double[]> jets = new ArrayList<>();
    }

    /** Centro de un banco de peces. */
    public static final class Banco {
        public double x, y, vx, sc;
        int tk = -1;
    }

    /** Un pez o un tesoro dentro del mundo. */
    public static final class Cosa {
        public int id, face, ast, pst;
        public double x, y, bx, by, vx, vy, sp, r, tm, ph, amp, fs, a, w, R, wait, fr, ox, oy, acd, hx = Double.NaN, hy, tx, ty, fleeing, ptm, vph;
        public boolean got, hid, zz;
        public Banco g;
        Azar rng;
    }

    public static final class Trozo {
        public final int i;
        public final List<Roca> rocks = new ArrayList<>();
        public final List<Peligro> haz = new ArrayList<>();
        public final List<Franja> cur = new ArrayList<>();
        public final List<Cosa> items = new ArrayList<>();
        public final List<Decor> decor = new ArrayList<>();

        Trozo(int i) {
            this.i = i;
        }
    }

    public final Ajustes aj;
    public final List<Objeto> objs;
    public final Terreno terreno;
    public final Terreno.Movimiento mov;
    public final double floorY;
    private final int seed;
    private final int puffId;
    /** Con sedal y recogida animada (cliente). El servidor no los necesita. */
    private final boolean visual;
    private final Azar azar;
    public final Map<Integer, Trozo> chunks = new HashMap<>();
    /** Sedal: puntos donde se ha enganchado, de arriba abajo. */
    public final List<double[]> rope = new ArrayList<>();
    /** Índices de {@link #objs} de lo que llevas cogido. */
    public final List<Integer> botin = new ArrayList<>();

    public Estado estado = Estado.CAST;
    public Causa causa;
    public double hookX = W / 2, hookY = -H / 2 + 8, hookVx, hookVy = 20, cam = -H / 2;
    /** Tiempo total y tiempo jugando (s). */
    public double t, runT;
    public double slowT, invT, fog;
    public boolean diving, atFloor;
    public int zona, pasos;
    /** Profundidad (m) a la que terminó la partida. */
    public double profFinal;
    /** Última captura (índice de objeto) y paso en que ocurrió, para el aviso en pantalla. */
    public int ultimaCaptura = -1, pasoCaptura = -1, pasoRed = -1000;
    public double reelV, reelVmax, reelOt;
    public boolean reelOut;
    /** Solo para pruebas: nada mata. */
    boolean inmortal;

    public Partida(Ajustes aj, List<Objeto> objs, boolean visual) {
        this.aj = aj;
        this.objs = objs;
        this.visual = visual;
        this.seed = aj.seed();
        this.mov = new Terreno.Movimiento(aj.base(), aj.k(), aj.vh(), aj.dive());
        this.terreno = new Terreno(seed, aj.bioma(), aj.sub(), aj.maxD(), mov);
        this.floorY = aj.maxD() * PPB;
        this.azar = new Azar(Azar.hash2(seed, 4242));
        int puff = -1;
        for (int i = 0; i < objs.size(); i++) if (objs.get(i).puff()) puff = i;
        this.puffId = puff;
        rope.add(new double[]{W / 2, -H / 2 - 8});
    }

    private static double sin(double a) {
        return StrictMath.sin(a);
    }

    private static double cos(double a) {
        return StrictMath.cos(a);
    }

    private static double hyp(double a, double b) {
        return Math.sqrt(a * a + b * b);
    }

    private static double sign(double v) {
        return v > 0 ? 1 : v < 0 ? -1 : 0;
    }

    private static double lerp(double a, double b, double f) {
        return a + (b - a) * f;
    }

    public static int zoneAt(double d) {
        int z = 0;
        for (int i = 0; i < ZONES.length; i++) if (d >= ZONES[i]) z = i;
        return z;
    }

    /** Zona con cartel: 5 en el agua, 4 en la lava y una cada 100 m en el Vacío. */
    public int zoneIdx(double d) {
        if (aj.bioma() == Bioma.VACIO) return (int) Math.floor(d / 100);
        if (aj.bioma() == Bioma.LAVA) {
            int z = 0;
            for (int i = 0; i < LAVA_Z.length; i++) if (d >= LAVA_Z[i]) z = i;
            return z;
        }
        return zoneAt(d);
    }

    public double profundidad() {
        return Math.max(0, hookY / PPB);
    }

    // ---------- Botín ----------

    private static double[] rarityWeights(double e) {
        return new double[]{Math.max(10, 100 - e * 1.8), e >= 3 ? 20 + e * 1.5 : 0, e >= 12 ? (e - 12) * 1.6 : 0, e >= 28 ? (e - 28) * 1.0 : 0, e >= 45 ? (e - 45) * 0.5 : 0};
    }

    private boolean permitido(Objeto o, double d) {
        if (o.puff()) return false;
        if (o.pez() != null) return o.pez().aparece(aj.bioma(), aj.clima(), aj.sub(), aj.cebos(), d);
        return o.tesoro().aparece(aj.bioma(), aj.clima(), d);
    }

    private int pickItem(Azar r, double d) {
        double e = d + aj.luck() * 5;
        double[] w = rarityWeights(e);
        double s = 0;
        for (double v : w) s += v;
        double x = r.next() * s;
        int tier = 0;
        for (int i = 0; i < 5; i++) {
            if (x < w[i]) {
                tier = i;
                break;
            }
            x -= w[i];
        }
        if (tier < 4 && r.next() < aj.luck() * 0.12) tier++;
        if (aj.bioma() == Bioma.VACIO) tier = Math.max(tier, Math.min(4, 1 + (int) Math.floor(d / 100))); // la rareza mínima sube cada 100 m
        double u = r.next();
        for (int n = 0; n < 5; n++) {
            int tr = n <= tier ? tier - n : n; // primero de la rareza elegida hacia abajo; después, hacia arriba
            if (n > tier && tr < 1) continue;
            List<Integer> pool = new ArrayList<>();
            for (int i = 0; i < objs.size(); i++) if (objs.get(i).rarity() == tr && permitido(objs.get(i), d)) pool.add(i);
            if (!pool.isEmpty()) return pool.get((int) Math.floor(u * pool.size()));
        }
        return -1;
    }

    // ---------- Generación por tramos ----------

    private static Deriva[] driftKinds(Bioma b, Clima w, double d) {
        switch (b) {
            case OCEANO:
                return w != Clima.TORMENTA ? new Deriva[]{Deriva.MEDUSA} : new Deriva[0];
            case CALIDO:
                return w != Clima.TORMENTA ? new Deriva[]{Deriva.MEDUSA_U} : new Deriva[0];
            case HELADO:
                return d < 45 ? new Deriva[]{Deriva.TEMPANO} : new Deriva[0];
            case RIO:
            case PANTANO:
                return new Deriva[]{Deriva.TRONCO};
            case LAVA:
                return d < 25 ? new Deriva[]{Deriva.MAGMA, Deriva.CHISPA} : (d >= 60 && d < 95 ? new Deriva[]{Deriva.ALMA} : new Deriva[0]);
            default:
                return d >= 50 ? new Deriva[]{Deriva.ISLA, Deriva.ISLA, Deriva.NUBE} : new Deriva[]{Deriva.ISLA};
        }
    }

    private void mkItem(Trozo c, Azar r, int id, double x, double y, boolean cave) {
        Objeto def = objs.get(id);
        int sgn = r.next() < 0.5 ? -1 : 1;
        double sp = 8 + r.next() * 12;
        if (def.pez() != null && def.mov() == PezDef.Mov.SCHOOL) {
            // banco: 3–6 peces en formación alrededor de un centro común
            int n = 3 + (int) Math.floor(r.next() * 4);
            Banco g = new Banco();
            g.x = x;
            g.y = y;
            g.vx = sgn * sp;
            for (int i = 0; i < n; i++) {
                Cosa it = nueva(c, r, def, id, x, y, sgn, sp, cave);
                it.g = g;
                it.ox = ((i % 3) - 1) * 7 + (r.next() - 0.5) * 3;
                it.oy = (Math.floor(i / 3.0) - 0.5) * 7 + (r.next() - 0.5) * 3;
                it.x = x + it.ox;
                it.y = y + it.oy;
                c.items.add(it);
            }
        } else {
            Cosa it = nueva(c, r, def, id, x, y, sgn, sp, cave);
            if (def.pez() != null && def.mov() == PezDef.Mov.AMBUSH) {
                it.hid = true;
                it.ast = HIDE;
                it.acd = 0;
            }
            c.items.add(it);
        }
    }

    private Cosa nueva(Trozo c, Azar r, Objeto def, int id, double x, double y, int sgn, double sp, boolean cave) {
        Cosa it = new Cosa();
        it.id = id;
        it.x = it.bx = x;
        it.y = it.by = y;
        it.vx = def.esPez() ? sgn * sp : 0;
        it.sp = sp;
        it.r = 3.5;
        it.face = sgn;
        it.ph = r.next() * 6.283;
        it.amp = cave ? 4 + r.next() * 3 : 7 + r.next() * 5;
        it.fs = 1.5 + r.next();
        it.a = r.next() * 6.283;
        it.w = (r.next() < 0.5 ? -1 : 1) * (1.4 + r.next());
        it.R = cave ? 4 + r.next() * 2 : 7 + r.next() * 4;
        it.wait = 0.4 + r.next() * 1.2;
        it.fr = def.pez() != null && def.pez().ghost() ? 36 : 30;
        it.rng = new Azar(Azar.hash2(seed, (int) (y * 16) + c.items.size() * 7919));
        return it;
    }

    private boolean freeY(double y, double m, double limit) {
        return y < limit && y > PPB * 4.5 && !terreno.inSafe(y, m);
    }

    Trozo genTrozo(int i) {
        Trozo c = new Trozo(i);
        double y0 = i * CH;
        if (y0 < 0) return c;
        Azar r = new Azar(Azar.hash2(seed, i + 1000));
        double d = (y0 + CH / 2) / PPB, limit = floorY - 14;
        Bioma b = aj.bioma();
        Clima wea = aj.clima();
        boolean storm = wea == Clima.TORMENTA, rain = wea != Clima.DESPEJADO, water = b.hasWeather, lava = b == Bioma.LAVA, voidB = b == Bioma.VACIO;
        boolean almas = lava && aj.sub() == SubBioma.ALMAS;
        // densidad: el clima multiplica (despejado ×1, lluvia ×1,15, tormenta ×1,3); en el Vacío sigue 1 − e^(−d/300)
        double vd = 1 - StrictMath.exp(-d / 300);
        double hz = (storm ? 1.3 : rain ? 1.15 : 1) * (voidB ? 0.25 + 1.6 * vd : 1);
        // Obstáculos estáticos (no hay en el Vacío)
        if (d >= 5 && !voidB) {
            int nr = (r.next() < Math.min(0.8, 0.3 + d * 0.012) ? 1 : 0) + (d > 25 && r.next() < 0.4 ? 1 : 0) + ((b == Bioma.PANTANO || almas) && r.next() < 0.7 ? 1 : 0);
            for (int k = 0; k < nr; k++) {
                double ry = y0 + 15 + r.next() * (CH - 30), f = r.next();
                int s = (int) (r.next() * 1e6);
                double kr = r.next();
                int z = zoneAt(ry / PPB);
                double rr = 4 + r.next() * 5;
                RocaTipo kind = z >= 2 && kr < 0.5 ? RocaTipo.CRISTAL : (z >= 1 && kr < 0.45 ? RocaTipo.ERIZO : RocaTipo.ROCA);
                if (b == Bioma.PANTANO) kind = kr < 0.5 ? RocaTipo.TRONCO : RocaTipo.RAIZF;
                else if (lava) {
                    kind = almas ? RocaTipo.FOSIL : RocaTipo.ROCA;
                    if (almas) rr += 3;
                } else if (b == Bioma.CALIDO && ry / PPB >= 10 && kr < 0.3) kind = RocaTipo.CORALF; // coral de fuego: quema
                else if (b == Bioma.CALIDO && wea == Clima.DESPEJADO && kr >= 0.3 && kr < 0.55) { // anémona: solo mata cerrada
                    kind = RocaTipo.ANEMONA;
                    rr = 5;
                }
                if (ry > limit || terreno.inSafe(ry, rr + 6)) continue;
                Terreno.Tramo tt = terreno.tunnelAt(ry);
                double x = tt.l() + rr + (tt.r() - tt.l() - 2 * rr) * f;
                if (Math.max(x - rr - tt.l(), tt.r() - x - rr) < 26) continue;
                boolean cerca = false;
                for (Roca o : c.rocks) if (Math.abs(o.y - ry) < o.r + rr + 12) cerca = true;
                if (cerca) continue;
                Roca o = new Roca();
                o.x = x;
                o.y = ry;
                o.r = rr;
                o.s = s;
                o.kind = kind;
                c.rocks.add(o);
            }
        }
        // Burbujas que suben (solo en agua); en el pantano, desde 10 m, algunas son de metano y explotan
        if (d >= 2 && water) {
            int nb = (int) Math.floor((0.5 + Math.min(d, 40) * 0.04) * hz + r.next());
            for (int k = 0; k < nb; k++) {
                double by = y0 + r.next() * CH, f = r.next(), rr = 2.5 + r.next() * 3, v = 16 + r.next() * 18;
                boolean met = b == Bioma.PANTANO && d >= 10 && r.next() < 0.4;
                if (by > limit || by < PPB * 4.5 || terreno.inSafe(by, 8)) continue; // nunca en la pantalla inicial ni en cuevas
                Terreno.Tramo tt = terreno.tunnelAt(by);
                Peligro h = new Peligro();
                h.k = K.BUB;
                h.x = tt.l() + 6 + (tt.r() - tt.l() - 12) * f;
                h.y = by;
                h.r = met ? rr + 1 : rr;
                h.v = met ? v * 0.7 : v;
                h.met = met;
                c.haz.add(h);
            }
        }
        // Peligros a la deriva
        Deriva[] dk = driftKinds(b, wea, d);
        if (dk.length > 0 && d >= 6) {
            int nd = (r.next() < Math.min(0.9, (0.35 + d * 0.01) * hz) ? 1 : 0) + ((b == Bioma.RIO || b == Bioma.PANTANO) && rain && r.next() < 0.7 ? 1 : 0)
                    + ((lava || voidB) && r.next() < 0.5 * hz ? 1 : 0);
            for (int k = 0; k < nd; k++) {
                double dy = y0 + 20 + r.next() * (CH - 40), f = r.next(), a = r.next(), ph = r.next() * 6.283, sp = 0.8 + r.next() * 0.8;
                Deriva def = dk[(int) Math.floor(r.next() * dk.length)];
                boolean cerca = false;
                for (Peligro h : c.haz) if (h.k == K.DRIFT && Math.abs(h.y - dy) < def.r + h.r + 16) cerca = true;
                if (!freeY(dy, 20, limit) || cerca) continue;
                Terreno.Tramo tt = terreno.tunnelAt(dy);
                double maxA = Math.max(0, tt.w() / 2 - 10 - def.r);
                Peligro h = new Peligro();
                h.k = K.DRIFT;
                h.def = def;
                h.x0 = tt.cx() + (f - 0.5) * maxA * 0.6;
                h.y = dy;
                h.amp = def.wide ? Math.min(maxA * 0.8, 20 + a * 45) : Math.min(maxA * 0.7, 8 + a * 25);
                h.ph = ph;
                h.sp = def.slow ? sp * 0.3 : (def.wide ? sp * 0.5 : sp);
                h.r = def.r;
                h.x = tt.cx();
                h.yy = dy;
                c.haz.add(h);
            }
        }
        // Red de pesca perdida (océano y río, hasta 20 m): no mata, te frena 1 s
        if ((b == Bioma.OCEANO || b == Bioma.RIO) && d >= 5 && d < 20 && r.next() < 0.35) {
            double ny = y0 + 20 + r.next() * (CH - 40), f = r.next(), ph = r.next() * 6.283;
            if (freeY(ny, 14, limit)) {
                Terreno.Tramo tt = terreno.tunnelAt(ny);
                Peligro h = new Peligro();
                h.k = K.NET;
                h.x0 = tt.l() + 18 + (tt.w() - 36) * f;
                h.y = ny;
                h.ph = ph;
                h.r = 6;
                h.x = tt.cx();
                h.yy = ny;
                c.haz.add(h);
            }
        }
        // Perseguidores lentos: sanguijuelas (pantano) y balas de shulker (Vacío: avisan 1 s, duran 4 s)
        if ((b == Bioma.PANTANO && d >= 5 && r.next() < Math.min(0.6, (0.25 + d * 0.006) * hz)) || (voidB && d >= 8 && r.next() < 0.75 * vd + 0.08)) {
            double sy = y0 + 20 + r.next() * (CH - 40), f = r.next();
            if (freeY(sy, 14, limit)) {
                Terreno.Tramo tt = terreno.tunnelAt(sy);
                Peligro h = new Peligro();
                h.k = K.SEEK;
                h.x = tt.l() + 10 + (tt.w() - 20) * f;
                h.y = h.yy = sy;
                h.r = voidB ? 2.6 : 3;
                h.st = WAIT;
                h.bul = voidB;
                c.haz.add(h);
            }
        }
        // Peligros que cruzan: depredadores en el agua (desde 22 m) y proyectiles de ghast (lava, 25–60 m)
        if (!voidB && (lava ? d >= 25 && d < 60 && r.next() < 0.75 : d >= 22 && r.next() < Math.min(0.8, (0.25 + (d - 22) * 0.01) * hz))) {
            double cy = y0 + 20 + r.next() * (CH - 40);
            int dir = r.next() < 0.5 ? 1 : -1;
            double v = 80 + r.next() * 40;
            if (cy < limit && !terreno.inSafe(cy, 15)) {
                Peligro h = new Peligro();
                h.k = K.CROSS;
                h.y = cy;
                h.dir = dir;
                h.v = v;
                h.x = dir > 0 ? -14 : W + 14;
                h.st = WAIT;
                h.r = 3.5;
                h.ghast = lava;
                c.haz.add(h);
            }
        }
        // Géiseres en las paredes: de agua desde 20 m; de lava en el bastión hundido
        if ((water ? d >= 20 : lava && d >= 25 && d < 60) && r.next() < Math.min(0.55, (0.18 + d * 0.006) * hz)) {
            double gy = y0 + 15 + r.next() * (CH - 30);
            int side = r.next() < 0.5 ? -1 : 1;
            double kind = r.next(), cd = 0.4 + r.next() * 1.2;
            Terreno.Tramo tt = terreno.tunnelAt(gy);
            boolean roca = false;
            for (Roca o : c.rocks) if (Math.abs(o.y - gy) < 22) roca = true;
            if (gy < limit && !terreno.inSafe(gy, 30) && tt.w() >= 70 && tt.l() > 0 && tt.r() < W && !roca) {
                double a = kind < 0.45 ? 0 : (kind < 0.72 ? -0.6 : 0.6);
                Peligro h = new Peligro();
                h.k = K.GEY;
                h.x = side < 0 ? tt.l() : tt.r();
                h.y = gy;
                h.dx = -side * cos(a);
                h.dy = sin(a);
                h.side = side;
                h.range = a == 0 ? tt.w() * 0.6 : Math.min(95, tt.w() * 0.9);
                h.st = IDLE;
                h.tm = cd;
                h.lava = lava;
                h.hot = lava || (zoneAt(gy / PPB) >= 4 && b != Bioma.HELADO);
                c.haz.add(h);
            }
        }
        // Columnas: rayo submarino (tormenta, hasta 45 m) y chimeneas hidrotermales (océano y cálido, desde 95 m)
        boolean ray = storm && (b == Bioma.OCEANO || b == Bioma.RIO || b == Bioma.PANTANO) && d >= 5 && d < 45, chim = (b == Bioma.OCEANO || b == Bioma.CALIDO) && d >= 95;
        if ((ray && r.next() < 0.6) || (chim && r.next() < 0.6)) {
            double ky = y0 + 40 + r.next() * (CH - 80), f = r.next(), cd = 0.2 + r.next() * 1.2;
            if (freeY(ky, 46, limit)) {
                Terreno.Tramo tt = terreno.tunnelAt(ky);
                Peligro h = new Peligro();
                h.k = K.COL;
                h.x = tt.l() + 14 + (tt.w() - 28) * f;
                h.y = ky;
                h.hh = chim ? 26 : 36;
                h.w = chim ? 5 : 4;
                h.st = IDLE;
                h.tm = cd;
                h.chim = chim;
                c.haz.add(h);
            }
        }
        // Carámbanos (helado, desde 20 m): caen desde el borde superior con sombra previa
        if (b == Bioma.HELADO && d >= 20 && r.next() < Math.min(0.6, (0.2 + d * 0.005) * hz)) {
            double fy = y0 + 20 + r.next() * (CH - 40), f = r.next();
            if (freeY(fy, 60, limit)) {
                Terreno.Tramo tt = terreno.tunnelAt(fy);
                Peligro h = new Peligro();
                h.k = K.FALL;
                h.x = tt.l() + 10 + (tt.w() - 20) * f;
                h.y = h.yy = fy;
                h.st = WAIT;
                c.haz.add(h);
            }
        }
        // Pez león de espinas (cálido, desde 20 m): cada 2 s dispara espinas en 8 direcciones
        if (b == Bioma.CALIDO && d >= 20 && r.next() < 0.3 * hz) {
            double ly = y0 + 20 + r.next() * (CH - 40), f = r.next();
            if (freeY(ly, 34, limit)) {
                Terreno.Tramo tt = terreno.tunnelAt(ly);
                boolean roca = false;
                for (Roca o : c.rocks) if (Math.abs(o.y - ly) < 40) roca = true;
                if (tt.w() >= 70 && !roca) {
                    Peligro h = new Peligro();
                    h.k = K.LION;
                    h.x = tt.l() + tt.w() * (f < 0.5 ? 0.26 : 0.74);
                    h.y = ly;
                    h.r = 3.2;
                    h.tm = 1 + r.next();
                    h.range = 22;
                    c.haz.add(h);
                }
            }
        }
        // Anguilas abisales que salen de la pared (agua, desde 70 m) y raíces que crecen (pantano con lluvia)
        boolean eel = water && d >= 70 && r.next() < Math.min(0.7, 0.3 + (d - 70) * 0.01);
        boolean root = b == Bioma.PANTANO && rain && d >= 5 && r.next() < 0.5;
        if (eel || root) {
            double ly = y0 + 15 + r.next() * (CH - 30);
            int side = r.next() < 0.5 ? -1 : 1;
            double cd = 0.3 + r.next() * 1.0;
            Terreno.Tramo tt = terreno.tunnelAt(ly);
            boolean ocupado = false;
            for (Roca o : c.rocks) if (Math.abs(o.y - ly) < 22) ocupado = true;
            for (Peligro h : c.haz) if (h.k == K.GEY && Math.abs(h.y - ly) < 25) ocupado = true;
            if (ly < limit && ly > PPB * 4.5 && !terreno.inSafe(ly, 30) && tt.w() >= 60 && !ocupado) {
                Peligro h = new Peligro();
                h.k = K.LUNGE;
                h.wx = h.x = side < 0 ? tt.l() : tt.r();
                h.y = ly;
                h.dir = -side;
                h.range = tt.w() * (eel ? 0.75 : 0.42);
                h.st = IDLE;
                h.tm = cd;
                h.stay = !eel;
                c.haz.add(h);
            }
        }
        // Decoración de la zona (solo visual)
        for (int k = 0; k < 7; k++) {
            double dy = y0 + r.next() * CH;
            int side = r.next() < 0.5 ? -1 : 1, sd = (int) (r.next() * 1e6);
            if (dy < PPB * 3 || dy > floorY || !visual) continue;
            double[][] iv = terreno.spansAt(dy);
            double x = side < 0 ? iv[0][0] : iv[iv.length - 1][1];
            if (x < 3 || x > W - 3) continue;
            Decor o = new Decor();
            o.x = x;
            o.y = dy;
            o.side = side;
            o.s = sd;
            o.z = zoneAt(dy / PPB);
            c.decor.add(o);
        }
        // Franjas que no matan (nunca se solapan)
        List<Fx> fxs = new ArrayList<>();
        List<Double> ps = new ArrayList<>();
        if (water && d >= 6) {
            fxs.add(Fx.PUSH);
            ps.add((storm ? 0.55 : 0.25) + d * 0.004 + (b == Bioma.OCEANO && rain && d < 45 ? 0.25 : 0));
        }
        if (b == Bioma.HELADO && wea == Clima.DESPEJADO && d >= 45) {
            fxs.add(Fx.SLOWX);
            ps.add(0.4);
        }
        if (b == Bioma.HELADO && storm && d >= 4 && d < 20) {
            fxs.add(Fx.BLIZZARD);
            ps.add(0.7);
        }
        if (b == Bioma.RIO && rain && d >= 4 && d < 30) {
            fxs.add(Fx.FAST);
            ps.add(0.5);
        }
        if (b == Bioma.PANTANO && d >= 20) {
            fxs.add(Fx.FOG);
            ps.add(0.4);
        }
        if (lava && d >= 60 && d < 95) {
            fxs.add(Fx.FAST);
            ps.add(0.45);
        }
        if (lava && d >= 95) {
            fxs.add(Fx.SLOWY);
            ps.add(0.55);
        }
        if (voidB && d >= 6) {
            fxs.add(Fx.PUSH);
            ps.add(0.15 + 0.6 * vd);
            fxs.add(Fx.INVERT);
            ps.add(0.1 + 0.4 * vd);
        }
        for (int n = 0; n < fxs.size(); n++) {
            Fx fx = fxs.get(n);
            double cy = y0 + r.next() * (CH - 40), h = fx == Fx.INVERT ? 12 : 30 + r.next() * 30;
            int dir = r.next() < 0.5 ? 1 : -1;
            double f = (25 + r.next() * 20) * (storm || fx == Fx.BLIZZARD || (fx == Fx.PUSH && b == Bioma.OCEANO && rain) ? 1.5 : 1), roll = r.next();
            boolean solapa = false;
            for (Franja o : c.cur) if (cy < o.y + o.h + 8 && cy + h > o.y - 8) solapa = true;
            if (roll < ps.get(n) && cy < limit && cy > PPB * 4 && !terreno.inSafe(cy, 0) && !terreno.inSafe(cy + h, 0) && !solapa) {
                Franja o = new Franja();
                o.y = cy;
                o.h = h;
                o.dir = dir;
                o.f = f;
                o.fx = fx;
                o.m = lava ? 1.3 : 1.4;
                c.cur.add(o);
            }
        }
        // Peces y tesoros
        if (d >= 1) {
            int ni = 1 + (r.next() < 0.45 ? 1 : 0) + (rain && r.next() < 0.25 ? 1 : 0);
            for (int k = 0; k < ni; k++) {
                double iy = y0 + r.next() * CH, f = r.next();
                if (iy > limit || iy < PPB) continue;
                int id = pickItem(r, iy / PPB);
                double[][] iv = terreno.spansAt(iy);
                double[] q = iv[Math.min(iv.length - 1, (int) Math.floor(f * iv.length))];
                double fx = (f * iv.length) % 1;
                double x = q[1] - q[0] > 12 ? q[0] + 6 + (q[1] - q[0] - 12) * fx : (q[0] + q[1]) / 2;
                boolean roca = false;
                for (Roca o : c.rocks) if (hyp(o.x - x, o.y - iy) < o.r + 6) roca = true;
                if (id < 0 || roca) continue;
                mkItem(c, r, id, Math.max(8, Math.min(W - 8, x)), iy, false);
            }
        }
        // Pez globo peligro (océano y cálido, desde 5 m): solo obstáculo, no se pesca
        if ((b == Bioma.OCEANO || b == Bioma.CALIDO) && d >= 5 && r.next() < Math.min(0.5, 0.2 + d * 0.004) * hz) {
            double py = y0 + 20 + r.next() * (CH - 40), f = r.next();
            if (freeY(py, 12, limit)) {
                Terreno.Tramo tt = terreno.tunnelAt(py);
                double x = tt.l() + 12 + (tt.w() - 24) * f;
                boolean roca = false;
                for (Roca o : c.rocks) if (hyp(o.x - x, o.y - py) < o.r + 14) roca = true;
                if (!roca && puffId >= 0) mkItem(c, r, puffId, x, py, false);
            }
        }
        // peces y tesoros extra dentro de las cuevas
        for (Terreno.Seccion s : terreno.sections) {
            double a = Math.max(y0, s.y0 + 8), e = Math.min(y0 + CH, s.y1 - 8);
            if (e - a < 20) continue;
            int n = 1 + (r.next() < 0.5 ? 1 : 0);
            for (int k = 0; k < n; k++) {
                double iy = a + r.next() * (e - a), f = r.next();
                int id = pickItem(r, iy / PPB);
                double[][] iv = Terreno.secSpans(s, iy);
                double[] q = iv[Math.min(iv.length - 1, (int) Math.floor(f * iv.length))];
                if (id >= 0) mkItem(c, r, id, (q[0] + q[1]) / 2, iy, true);
            }
        }
        return c;
    }

    public int primerTrozo() {
        return (int) Math.floor((cam - 60) / CH);
    }

    public int ultimoTrozo() {
        return (int) Math.floor((cam + H + CH) / CH);
    }

    private void ensureChunks() {
        int a = primerTrozo(), b = ultimoTrozo();
        for (int i = a; i <= b; i++) if (!chunks.containsKey(i)) chunks.put(i, genTrozo(i));
        for (Iterator<Integer> it = chunks.keySet().iterator(); it.hasNext(); ) if (it.next() < a - 2) it.remove();
    }

    /** Los tramos que están a la vista, de arriba abajo. */
    public List<Trozo> visibles() {
        List<Trozo> out = new ArrayList<>();
        for (int i = primerTrozo(); i <= ultimoTrozo(); i++) {
            Trozo c = chunks.get(i);
            if (c != null) out.add(c);
        }
        return out;
    }

    // ---------- Sedal: se engancha en las esquinas y se suelta al quedar recto ----------

    private boolean ropeOpen(double x, double y) {
        return y < PPB * 2.4 || terreno.inOpen(x, y, 0);
    }

    private boolean ropeClear(double[] p, double qx, double qy) {
        double dx = qx - p[0], dy = qy - p[1];
        int n = (int) Math.ceil(hyp(dx, dy) / 2.5);
        for (int i = 1; i < n; i++) {
            double f = (double) i / n;
            if (!ropeOpen(p[0] + dx * f, p[1] + dy * f)) return false;
        }
        return true;
    }

    private double[] ropeCorner(double[] p, double qx, double qy) {
        double dx = qx - p[0], dy = qy - p[1];
        int n = (int) Math.ceil(hyp(dx, dy) / 2.5);
        double[] best = null;
        double bd = 0.3;
        for (int i = 1; i < n; i++) {
            double f = (double) i / n, x = p[0] + dx * f, y = p[1] + dy * f;
            if (y < PPB * 2.4) continue;
            boolean inside = false;
            double near = 1e9, ex = 0;
            for (double[] sp : terreno.spansAt(y)) {
                if (x >= sp[0] && x <= sp[1]) {
                    inside = true;
                    break;
                }
                double dl = Math.abs(x - sp[0]), dr = Math.abs(x - sp[1]);
                if (dl < near) {
                    near = dl;
                    ex = sp[0] + 0.7;
                }
                if (dr < near) {
                    near = dr;
                    ex = sp[1] - 0.7;
                }
            }
            if (!inside && near > bd) {
                bd = near;
                best = new double[]{ex, y};
            }
        }
        return best;
    }

    private void updRope() {
        double hx = hookX, hy = hookY - 3;
        while (rope.size() > 1 && ropeClear(rope.get(rope.size() - 2), hx, hy)) rope.remove(rope.size() - 1);
        for (int n = 0; n < 4; n++) {
            double[] c = ropeCorner(rope.get(rope.size() - 1), hx, hy);
            if (c == null) break;
            rope.add(c);
        }
    }

    // ---------- Partida ----------

    private void startReel() {
        estado = Estado.REEL;
        profFinal = profundidad();
        reelV = 60;
        reelOut = false;
        reelOt = 0;
        reelVmax = Math.max(420, hookY / 7); // la subida dura como mucho ~8 s
    }

    private void updReel(double dt) {
        runT += dt;
        if (!visual) {
            estado = Estado.FIN;
            return;
        }
        if (!reelOut) {
            ensureChunks();
            // el anzuelo vuelve por el mismo recorrido del sedal, soltándose de cada enganche al pasar
            reelV = Math.min(reelVmax, reelV + Math.max(380, reelVmax * 2) * dt);
            double step = reelV * dt;
            for (int g = 0; g < 20 && step > 0; g++) {
                boolean ultimo = rope.size() <= 1;
                double tx = rope.get(rope.size() - 1)[0], ty = (ultimo ? -40 : rope.get(rope.size() - 1)[1]) + 3;
                double ddx = tx - hookX, ddy = ty - hookY, dist = hyp(ddx, ddy);
                if (dist <= step) {
                    hookX = tx;
                    hookY = ty;
                    step -= dist;
                    if (!ultimo) rope.remove(rope.size() - 1);
                    else break;
                } else {
                    hookX += ddx / dist * step;
                    hookY += ddy / dist * step;
                    step = 0;
                }
            }
            cam = hookY - H / 2;
            if (hookY <= 0) {
                reelOut = true;
                reelOt = 0;
            }
        } else {
            reelOt += dt;
            hookY -= Math.max(80, reelV * 0.5) * dt;
            if (reelOt > 0.75) estado = Estado.FIN;
        }
        for (Trozo c : visibles()) {
            for (Peligro h : c.haz) updHaz(h, dt);
            for (Cosa it : c.items) updItem(it, dt);
        }
    }

    private void morir(Causa c) {
        estado = Estado.MUERTO;
        causa = c;
        profFinal = profundidad();
    }

    /** Avanza un paso de {@link #DT} segundos con esas teclas ({@link #IZQ}, {@link #DER}, {@link #BAJAR}, {@link #RECOGER}). */
    public void paso(int teclas) {
        if (estado == Estado.FIN || estado == Estado.MUERTO) return;
        final double dt = DT;
        pasos++;
        t = pasos * dt;
        if (estado == Estado.CAST) {
            hookVy += 260 * dt;
            hookY += hookVy * dt;
            if (hookY >= 0) {
                hookY = 0;
                estado = Estado.PLAY;
            }
            return;
        }
        if (estado == Estado.PLAY && (teclas & RECOGER) != 0) startReel();
        if (estado == Estado.REEL) {
            updReel(dt);
            return;
        }
        runT += dt;
        double d = hookY / PPB, stop = floorY - 10;
        diving = (teclas & BAJAR) != 0;
        // franjas que no matan: modifican el movimiento mientras el anzuelo está dentro
        double push = 0, mx = 1, my = 1, fogT = 0;
        for (Trozo c : visibles())
            for (Franja cu : c.cur) {
                if (hookY <= cu.y || hookY >= cu.y + cu.h) continue;
                switch (cu.fx) {
                    case PUSH -> push += cu.dir * cu.f;
                    case BLIZZARD -> {
                        push += cu.f * 1.2 * sin(t * 2.3 + cu.y);
                        fogT = 0.6;
                    }
                    case SLOWX -> mx = 0.5;
                    case FAST -> my = cu.m;
                    case SLOWY -> my = 0.6;
                    case FOG -> fogT = 1;
                    case INVERT -> invT = 3;
                }
            }
        if (slowT > 0) {
            slowT -= dt;
            mx *= 0.3;
        }
        if (invT > 0) invT -= dt;
        fog += (fogT - fog) * Math.min(1, dt * 3);
        double spd = mov.fallSpd(hookY, diving, my);
        if (hookY < stop) hookY = Math.min(stop, hookY + spd * dt);
        else atFloor = true;
        zona = Math.max(zona, zoneIdx(d));
        cam = hookY - H / 2;
        ensureChunks();
        int dir = (((teclas & DER) != 0 ? 1 : 0) - ((teclas & IZQ) != 0 ? 1 : 0)) * (invT > 0 ? -1 : 1);
        hookVx += (dir * mov.vhAt(spd) * mx - hookVx) * Math.min(1, dt * 12);
        hookX += (hookVx + push) * dt;
        hookX = Math.max(HR, Math.min(W - HR, hookX));
        List<Trozo> vis = visibles();
        for (Trozo c : vis) {
            for (Peligro h : c.haz) updHaz(h, dt);
            for (Cosa it : c.items) updItem(it, dt);
        }
        if (visual) updRope();
        Causa hit = checkHit(vis);
        if (hit != null && !inmortal) {
            morir(hit);
            return;
        }
        for (Trozo c : vis)
            for (Cosa it : c.items) {
                if (!it.got && it.id != puffId && !it.hid && it.pst != BIG && hyp(it.x - hookX, it.y - hookY) < aj.hookR() + it.r + 1) {
                    it.got = true;
                    botin.add(it.id);
                    ultimaCaptura = it.id;
                    pasoCaptura = pasos;
                    if (it.g != null) it.g.sc = 1; // el resto del banco se dispersa 1 s
                }
            }
    }

    private void updHaz(Peligro h, double dt) {
        if (h.dead) return;
        boolean play = estado == Estado.PLAY;
        switch (h.k) {
            case BUB -> {
                if (!h.act) {
                    if (h.y < cam + H + 8 && h.y > cam + H - 4) h.act = true;
                    else if (h.y <= cam + H - 4) {
                        h.dead = true;
                        return;
                    } else return;
                }
                h.y -= h.v * dt;
                h.x += sin(t * 3 + h.v) * 6 * dt;
                Terreno.Tramo tt = terreno.tunnelAt(h.y);
                if (h.x - h.r < tt.l() - 1 || h.x + h.r > tt.r() + 1 || h.y < -2 || terreno.inSafe(h.y, 0)) h.dead = true;
                if (h.met && !h.dead) { // metano: a los 3 s parpadea y explota
                    h.life += dt;
                    if (h.life > 3.25) h.dead = true;
                }
            }
            case NET -> {
                h.x = h.x0 + 10 * sin(h.ph + t * 0.6);
                h.yy = h.y + sin(t + h.ph) * 3;
            }
            case SEEK -> {
                if (h.st == WAIT) {
                    if (h.y <= cam) h.dead = true;
                    else if (play && h.y < cam + H - 6) {
                        if (h.bul) {
                            h.st = WARN;
                            h.tm = 1;
                            h.side = hookX < W / 2 ? 1 : -1;
                        } else h.st = GO;
                    }
                } else if (h.st == WARN) {
                    h.tm -= dt;
                    if (h.tm <= 0) {
                        h.st = GO;
                        h.life = 4;
                        h.x = h.side > 0 ? W + 4 : -4;
                        h.y = hookY + 30;
                    }
                } else {
                    double dx = hookX - h.x, dy = hookY - h.y, dist = hyp(dx, dy), v = h.bul ? 30 : 15;
                    if (dist == 0) dist = 1;
                    h.x += dx / dist * v * dt;
                    h.y += dy / dist * v * dt;
                    if (h.bul) {
                        if (play) h.y += mov.fallSpd(hookY, diving, 1) * dt;
                        h.life -= dt;
                        if (h.life <= 0) h.dead = true;
                    } else if (terreno.inSafe(h.y, 4) || h.y < cam - 20) h.dead = true;
                }
                h.yy = h.y;
            }
            case COL -> {
                boolean vis = h.y > cam - 40 && h.y < cam + H + 40;
                if (h.st == IDLE) {
                    if (vis) h.tm -= dt;
                    if (h.tm <= 0) {
                        h.st = WARN;
                        h.tm = 1;
                    }
                } else if (h.st == WARN) {
                    h.tm -= dt;
                    if (h.tm <= 0) {
                        h.st = ON;
                        h.tm = h.chim ? 1.1 : 0.4;
                    }
                } else {
                    h.tm -= dt;
                    if (h.tm <= 0) {
                        h.st = IDLE;
                        h.tm = 1.5 + azar.next() * 1.5;
                    }
                }
            }
            case FALL -> {
                if (h.st == WAIT) {
                    if (play && hookY > h.y - 30) {
                        h.st = WARN;
                        h.tm = 0.9;
                    }
                } else if (h.st == WARN) {
                    h.tm -= dt;
                    if (h.tm <= 0) {
                        h.st = GO;
                        h.yy = cam - 6;
                    }
                } else {
                    h.yy += (mov.fallSpd(hookY, diving && play, 1) + 110) * dt;
                    if (h.yy > cam + H + 12 || terreno.inSafe(h.yy, 0) || (h.yy > PPB * 3 && !terreno.inOpen(h.x, h.yy, 1))) h.dead = true;
                }
            }
            case LION -> {
                boolean vis = h.y > cam - 20 && h.y < cam + H + 20;
                if (vis) h.tm -= dt;
                if (h.tm <= 0) {
                    h.tm = 2;
                    for (int i = 0; i < 8; i++) {
                        double a = i / 8.0 * 6.283 + 0.39;
                        h.jets.add(new double[]{h.x, h.y, cos(a) * 55, sin(a) * 55, 1, 0});
                    }
                }
                for (double[] q : h.jets) {
                    q[0] += q[2] * dt;
                    q[1] += q[3] * dt;
                    q[5] += 55 * dt;
                }
                h.jets.removeIf(q -> q[5] >= h.range);
            }
            case LUNGE -> {
                boolean vis = h.y > cam - 20 && h.y < cam + H + 20;
                if (h.st == IDLE) {
                    if (vis) h.tm -= dt;
                    if (h.tm <= 0) {
                        h.st = WARN;
                        h.tm = 0.9;
                    }
                } else if (h.st == WARN) {
                    h.tm -= dt;
                    if (h.tm <= 0) h.st = OUT;
                } else if (h.st == OUT) {
                    h.ext = Math.min(h.range, h.ext + 170 * dt);
                    if (h.ext >= h.range) {
                        h.st = HOLD;
                        h.tm = 0.35;
                    }
                } else if (h.st == HOLD) {
                    h.tm -= dt;
                    if (h.tm <= 0 && !h.stay) h.st = BACK; // las raíces se quedan
                } else if (h.st == BACK) {
                    h.ext = Math.max(0, h.ext - 90 * dt);
                    if (h.ext <= 0) {
                        h.st = IDLE;
                        h.tm = 2.2 + azar.next() * 1.5;
                    }
                }
                h.x = h.wx + h.dir * h.ext;
            }
            case GEY -> {
                boolean vis = h.y > cam - 30 && h.y < cam + H + 30;
                if (h.st == IDLE) {
                    if (vis) h.tm -= dt;
                    if (h.tm <= 0) {
                        h.st = WARN;
                        h.tm = 1.1;
                    }
                } else if (h.st == WARN) {
                    h.tm -= dt;
                    if (h.tm <= 0) {
                        h.st = SHOOT;
                        h.tm = 1.1;
                    }
                } else if (h.st == SHOOT) {
                    h.tm -= dt;
                    h.emit += dt * 30;
                    while (h.emit >= 1) {
                        h.emit--;
                        double sp = (azar.next() - 0.5) * 0.25, ca = cos(sp), sa = sin(sp);
                        double dx = h.dx * ca - h.dy * sa, dy = h.dx * sa + h.dy * ca, v = 100 + azar.next() * 35;
                        h.jets.add(new double[]{h.x + h.dx * 3, h.y + h.dy * 3, dx * v, dy * v, 1.2 + azar.next(), 0});
                    }
                    if (h.tm <= 0) {
                        h.st = IDLE;
                        h.tm = 2 + azar.next() * 2;
                    }
                }
                for (double[] j : h.jets) {
                    j[0] += j[2] * dt;
                    j[1] += j[3] * dt;
                    j[3] -= 12 * dt;
                    j[5] += hyp(j[2], j[3]) * dt;
                }
                h.jets.removeIf(j -> {
                    if (j[5] > h.range) return true;
                    Terreno.Tramo tt = terreno.tunnelAt(j[1]);
                    return !(j[0] > tt.l() - 1 && j[0] < tt.r() + 1);
                });
            }
            case DRIFT -> {
                h.x = h.x0 + h.amp * sin(h.ph + h.sp * t);
                h.yy = h.y + (h.def.bounce ? -Math.abs(sin(t * 2.4 + h.ph)) * 13 : sin(t * 1.5 + h.ph) * 3);
            }
            case CROSS -> {
                double dy = h.y - hookY, spd = mov.fallSpd(hookY, diving, 1);
                if (play && h.st == WAIT && dy < spd * 2 + 15 && dy > -20) {
                    h.st = WARN;
                    h.tw = 0.9;
                } else if (h.st == WARN) {
                    h.tw -= dt;
                    if (h.tw <= 0) h.st = GO;
                } else if (h.st == GO) {
                    h.x += h.dir * h.v * dt;
                    if (h.x < -30 || h.x > W + 30) h.dead = true;
                }
            }
        }
    }

    private static double clampV(double v, double a) {
        return Math.max(-a, Math.min(a, v));
    }

    private static void keepNear(Cosa it, double lim) {
        double off = it.y - it.by;
        if (Math.abs(off) > lim) {
            it.y = it.by + sign(off) * lim;
            it.vy = -it.vy * 0.3;
        }
    }

    private void updItem(Cosa it, double dt) {
        if (it.got) return;
        Objeto def = objs.get(it.id);
        boolean play = estado == Estado.PLAY;
        // Atracción: tirón hacia el anzuelo (los tesoros, además, con el Anzuelo imán)
        double px = 0, py = 0;
        boolean lured = false;
        double radio = def.esPez() ? aj.attractR() : Math.max(aj.attractR(), aj.magnetR());
        if (radio > 0) {
            double dx = hookX - it.x, dy = hookY - it.y, dist = hyp(dx, dy);
            if (dist < radio && dist > 0.1) {
                double f = 35 * Math.max(1, aj.lure());
                px = dx / dist * f;
                py = dy / dist * f;
                lured = true;
            }
        }
        if (!def.esPez()) {
            it.vx += px * dt;
            it.vy += py * dt;
            it.x += it.vx * dt;
            it.y += it.vy * dt;
            it.vx *= StrictMath.pow(0.1, dt);
            it.vy *= StrictMath.pow(0.1, dt);
            return;
        }
        it.tm += dt;
        double ox = it.x, oy = it.y;
        PezDef.Mov m = def.mov();
        switch (m) {
            case SWIM -> {
                it.vx += px * dt;
                it.vy += py * dt;
                if (!lured) it.vx += (sign(it.vx != 0 ? it.vx : it.face) * it.sp - it.vx) * Math.min(1, dt * 1.5);
                it.vx = clampV(it.vx, 60);
                it.vy *= StrictMath.pow(0.1, dt);
                it.x += it.vx * dt;
                it.y += it.vy * dt;
                it.by += py * dt;
            }
            case VERTICAL -> {
                it.bx += (it.vx * 0.25 + px) * dt;
                it.by += py * dt;
                it.x = it.bx;
                it.y = it.by + sin(it.tm * it.fs + it.ph) * it.amp;
            }
            case SINE -> {
                it.vx += px * dt;
                if (!lured) it.vx += (sign(it.vx != 0 ? it.vx : it.face) * it.sp - it.vx) * Math.min(1, dt * 1.5);
                it.vx = clampV(it.vx, 60);
                it.by += py * dt;
                it.x += it.vx * dt;
                it.y = it.by + sin(it.tm * 3 + it.ph) * 6;
            }
            case DART -> {
                if (it.tm > it.wait) {
                    double ang = (it.rng.next() < 0.5 ? 0 : Math.PI) + (it.rng.next() - 0.5) * 1.4;
                    it.vx = cos(ang) * 75;
                    it.vy = sin(ang) * 75;
                    it.tm = 0;
                    it.wait = 0.7 + it.rng.next() * 1.0;
                }
                it.vx = it.vx * StrictMath.pow(0.04, dt) + px * dt * 3;
                it.vy = it.vy * StrictMath.pow(0.04, dt) + py * dt * 3;
                it.x += it.vx * dt;
                it.y += it.vy * dt;
                it.by += py * dt;
                keepNear(it, 14);
            }
            case CIRCLE -> {
                it.a += it.w * dt;
                it.bx += px * dt;
                it.by += py * dt;
                double[] s0 = terreno.spanAt(it.bx, it.by);
                double rr = Math.min(it.R, Math.max(2, (s0[1] - s0[0]) / 2 - 6));
                it.bx = Math.max(s0[0] + rr + 5, Math.min(s0[1] - rr - 5, it.bx));
                it.x = it.bx + cos(it.a) * rr;
                it.y = it.by + sin(it.a) * rr * 0.7;
            }
            case ZIGZAG -> {
                if (!it.zz) {
                    it.zz = true;
                    it.vx = sign(it.vx != 0 ? it.vx : 1) * (18 + it.sp);
                    it.vy = (it.rng.next() < 0.5 ? -1 : 1) * 14;
                }
                it.x += (it.vx + px) * dt;
                it.y += (it.vy + py) * dt;
                it.by += py * dt;
                if (Math.abs(it.y - it.by) > 14) {
                    it.y = it.by + sign(it.y - it.by) * 14;
                    it.vy = -it.vy;
                }
            }
            case JET -> {
                if (it.tm > it.wait) {
                    it.vy = it.y > it.by - 10 ? -50 : 35;
                    it.tm = 0;
                    it.wait = 1.1 + it.rng.next() * 0.9;
                }
                it.vy *= StrictMath.pow(0.15, dt);
                it.x += (it.vx * 0.3 + px) * dt;
                it.y += (it.vy + 4 + py) * dt;
                it.by += py * dt;
                keepNear(it, 18);
            }
            case FLEE -> {
                double dx = it.x - hookX, dy = it.y - hookY, dist = hyp(dx, dy);
                if (dist == 0) dist = 0.1;
                if (dist < it.fr && play) {
                    it.vx += dx / dist * 160 * dt;
                    it.vy += dy / dist * 110 * dt;
                    it.fleeing = 0.35;
                } else {
                    it.vx += (sign(it.vx != 0 ? it.vx : it.face) * it.sp * 0.6 - it.vx) * Math.min(1, dt * 1.2);
                    it.vy *= StrictMath.pow(0.1, dt);
                }
                it.vx += px * dt;
                it.vy += py * dt;
                double sp = hyp(it.vx, it.vy);
                if (sp > 70) {
                    it.vx *= 70 / sp;
                    it.vy *= 70 / sp;
                }
                it.x += it.vx * dt;
                it.y += it.vy * dt;
                it.by += py * dt;
                keepNear(it, 24);
                if (it.fleeing > 0) it.fleeing -= dt;
            }
            case SCHOOL -> {
                // el centro del banco nada de lado a lado; cada pez sigue su hueco en la formación
                Banco g = it.g;
                if (g.tk != pasos) {
                    g.tk = pasos;
                    g.x += g.vx * dt;
                    if (g.sc > 0) g.sc -= dt;
                    double[] sp1 = terreno.spanAt(g.x, g.y);
                    double mg = Math.min(12, (sp1[1] - sp1[0]) / 2 - 1);
                    if (g.x < sp1[0] + mg) {
                        g.x = sp1[0] + mg;
                        g.vx = Math.abs(g.vx);
                    }
                    if (g.x > sp1[1] - mg) {
                        g.x = sp1[1] - mg;
                        g.vx = -Math.abs(g.vx);
                    }
                }
                double kk = g.sc > 0 ? 2.8 : 1; // dispersión durante 1 s al coger uno
                it.vx = (g.x + it.ox * kk - it.x) * 5 + px;
                it.vy = (g.y + it.oy * kk + sin(it.tm * 2 + it.ph) * 1.5 - it.y) * 5 + py;
                it.x += it.vx * dt;
                it.y += it.vy * dt;
            }
            case AMBUSH -> {
                // escondido en la pared; sale disparado cuando pasas cerca, espera un momento y vuelve
                if (Double.isNaN(it.hx)) {
                    double[] s1 = terreno.spanAt(it.x, it.y);
                    boolean left = it.x - s1[0] < s1[1] - it.x;
                    it.hx = left ? s1[0] + 5 : s1[1] - 5;
                    it.hy = it.y;
                    it.x = it.hx;
                    it.face = left ? 1 : -1;
                }
                double ax = hookX - it.hx, ay = hookY - it.hy;
                if (it.ast == HIDE) {
                    it.x = it.hx;
                    it.y = it.hy;
                    it.acd -= dt;
                    if (play && it.acd <= 0 && Math.abs(ay) < 24 && Math.abs(ax) < 64) {
                        double len = hyp(ax, ay);
                        if (len == 0) len = 1;
                        double rr = Math.min(44, len + 6);
                        it.tx = it.hx + ax / len * rr;
                        it.ty = it.hy + ay / len * rr * 0.6;
                        it.ast = OUT;
                        it.hid = false;
                    }
                } else if (it.ast == OUT) {
                    double ex = it.tx - it.x, ey = it.ty - it.y, len = hyp(ex, ey);
                    if (len < 2) {
                        it.ast = HOLD;
                        it.acd = 0.6;
                    } else {
                        it.x += ex / len * 95 * dt;
                        it.y += ey / len * 95 * dt;
                    }
                } else if (it.ast == HOLD) {
                    it.acd -= dt;
                    it.x += px * dt;
                    it.y += py * dt;
                    if (it.acd <= 0) it.ast = BACK;
                } else {
                    double ex = it.hx - it.x, ey = it.hy - it.y, len = hyp(ex, ey);
                    if (len < 2) {
                        it.ast = HIDE;
                        it.hid = true;
                        it.acd = 1.3;
                    } else {
                        it.x += ex / len * 45 * dt;
                        it.y += ey / len * 45 * dt;
                    }
                }
            }
        }
        // pez globo: a veces vibra cada vez más rápido y se hincha (pincha)
        if (def.puff()) {
            if (it.pst == 0) {
                it.pst = CALM;
                it.ptm = 1.5 + it.rng.next() * 3;
                it.vph = 0;
            }
            boolean vis = it.y > cam - 10 && it.y < cam + H + 10, safe = terreno.inSafe(it.y, 0);
            if (vis && (play || estado == Estado.REEL) && !safe) {
                it.ptm -= dt;
                if (it.pst == CALM && it.ptm <= 0) {
                    it.pst = PWARN;
                    it.ptm = 1.4;
                } else if (it.pst == PWARN && it.ptm <= 0) {
                    it.pst = BIG;
                    it.ptm = 2.4;
                } else if (it.pst == BIG && it.ptm <= 0) {
                    it.pst = CALM;
                    it.ptm = 2.5 + it.rng.next() * 3;
                }
            } else if (it.pst != CALM && safe) {
                it.pst = CALM;
                it.ptm = 2;
            }
            if (it.pst == PWARN) it.vph += dt * (18 + (1 - it.ptm / 1.4) * 70);
        }
        // paredes, techo/suelo de cuevas y fondo
        if (!terreno.inOpen(it.x, it.y, 1) && terreno.inOpen(it.x, oy, 1)) {
            it.y = oy;
            it.vy = -it.vy * 0.5;
        }
        double[] sp0 = terreno.spanAt(it.x, it.y);
        if (it.x < sp0[0] + 5) {
            it.x = sp0[0] + 5;
            it.vx = Math.abs(it.vx);
            it.bx = Math.max(it.bx, it.x);
        }
        if (it.x > sp0[1] - 5) {
            it.x = sp0[1] - 5;
            it.vx = -Math.abs(it.vx);
            it.bx = Math.min(it.bx, it.x);
        }
        if (it.y > floorY - 6) {
            it.y = floorY - 6;
            it.vy = -Math.abs(it.vy);
        }
        double mdx = it.x - ox;
        if (it.g != null) it.face = it.g.vx > 0 ? 1 : -1;
        else if (!(m == PezDef.Mov.AMBUSH && it.ast == HIDE) && Math.abs(mdx) > 0.002) it.face = mdx > 0 ? 1 : -1;
    }

    /** Radio del pez globo: pequeño salvo cuando está hinchado. */
    public static double puffR(Cosa it) {
        if (it.pst != BIG) return 2.3;
        double el = 2.4 - it.ptm, k = Math.min(1, Math.min(el / 0.2, it.ptm / 0.3));
        return lerp(2.3, 6.5, Math.max(0, k));
    }

    public boolean anemClosed(Roca o) {
        return ((t * 0.45 + (o.s % 100) / 100.0) % 1) < 0.38;
    }

    private static final String[] CRUZA = {"pez_espada", "barracuda", "foca", "lucio", "anguila_fango", "ghast", "ghast"};

    private Causa checkHit(List<Trozo> vis) {
        double hx = hookX, hy = hookY;
        if (hy > PPB * 2.4) {
            for (double yy : new double[]{hy - HR, hy, hy + HR}) if (!terreno.inOpen(hx, yy, HR * 0.8)) return new Causa("pared", "rock");
        }
        if (runT < 1.0) return null; // margen de gracia al empezar
        for (Trozo c : vis) {
            for (Cosa it : c.items)
                if (!it.got && it.pst == BIG && hyp(it.x - hx, it.y - hy) < puffR(it) + HR - 1) return new Causa("pez_globo", "spike");
            for (Roca o : c.rocks)
                if (hyp(o.x - hx, o.y - hy) < o.r + HR - 0.5) {
                    switch (o.kind) {
                        case ANEMONA:
                            if (!anemClosed(o)) continue;
                            return new Causa("anemona", "spike");
                        case ERIZO:
                            return new Causa("erizo", "spike");
                        case CORALF:
                            return new Causa("coral_fuego", "burn");
                        case CRISTAL:
                            return new Causa("cristal", "rock");
                        case TRONCO:
                            return new Causa("tronco_sumergido", "rock");
                        case RAIZF:
                            return new Causa("raiz", "rock");
                        case FOSIL:
                            return new Causa("fosil", "rock");
                        default:
                            return new Causa("roca", "rock");
                    }
                }
            for (Peligro h : c.haz) {
                if (h.dead) continue;
                switch (h.k) {
                    case BUB -> {
                        if (h.act && hyp(h.x - hx, h.y - hy) < (h.met && h.life > 3 ? 9 : h.r) + HR - 0.8)
                            return h.met ? new Causa("burbuja_metano", "blast") : new Causa("burbuja", "bubble");
                    }
                    case DRIFT -> {
                        if (hyp(h.x - hx, h.yy - hy) < h.r + HR - 1) return new Causa(h.def.clave, h.def.tipo);
                    }
                    case NET -> {
                        if (hyp(h.x - hx, h.yy - hy) < h.r + HR) {
                            h.dead = true;
                            slowT = 1;
                            pasoRed = pasos;
                        }
                    }
                    case SEEK -> {
                        if (h.st == GO && hyp(h.x - hx, h.y - hy) < h.r + HR - 0.8)
                            return h.bul ? new Causa("bala_shulker", "shock") : new Causa("sanguijuela", "leech");
                    }
                    case COL -> {
                        if (h.st == ON && Math.abs(h.x - hx) < h.w + HR - 1 && Math.abs(h.y - hy) < h.hh)
                            return h.chim ? new Causa("chimenea", "burn") : new Causa("rayo", "shock");
                    }
                    case FALL -> {
                        if (h.st == GO && hyp(h.x - hx, h.yy - hy) < 2.6 + HR) return new Causa("carambano", "spike");
                    }
                    case LION -> {
                        if (hyp(h.x - hx, h.y - hy) < h.r + HR - 0.5) return new Causa("pez_leon", "spike");
                        for (double[] q : h.jets) if (hyp(q[0] - hx, q[1] - hy) < 1 + HR - 0.5) return new Causa("espina", "spike");
                    }
                    case GEY -> {
                        for (double[] j : h.jets)
                            if (hyp(j[0] - hx, j[1] - hy) < j[4] + HR - 0.5)
                                return h.lava ? new Causa("geiser_lava", "burn") : new Causa(h.hot ? "fumarola" : "geiser", "blast");
                    }
                    case LUNGE -> {
                        if (h.ext > 2 && Math.abs(h.y - hy) < 2.6 + HR - 0.5 && hx > Math.min(h.wx, h.x) - 2 && hx < Math.max(h.wx, h.x) + 2)
                            return h.stay ? new Causa("raiz_crecida", "rock") : new Causa("anguila", "bite");
                    }
                    case CROSS -> {
                        if (h.st == GO) for (double o : new double[]{6, 0, -5})
                            if (hyp(h.x + h.dir * o - hx, h.y - hy) < HR + 2.6) return new Causa(CRUZA[aj.bioma().ordinal()], h.ghast ? "ghast" : "bite");
                    }
                }
            }
        }
        return null;
    }
}
