package com.rolflin.toodeepseas.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import com.rolflin.toodeepseas.core.juego.Grabacion;
import com.rolflin.toodeepseas.core.juego.Partida;
import com.rolflin.toodeepseas.core.juego.Partida.Cosa;
import com.rolflin.toodeepseas.core.juego.Partida.Estado;
import com.rolflin.toodeepseas.core.juego.Partida.Franja;
import com.rolflin.toodeepseas.core.juego.Partida.Fx;
import com.rolflin.toodeepseas.core.juego.Partida.Objeto;
import com.rolflin.toodeepseas.core.juego.Partida.Peligro;
import com.rolflin.toodeepseas.core.juego.Partida.Roca;
import com.rolflin.toodeepseas.core.juego.Partida.Trozo;
import com.rolflin.toodeepseas.core.juego.Perfil;
import com.rolflin.toodeepseas.core.juego.Terreno;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.juego.Sesiones;
import com.rolflin.toodeepseas.net.FinMinijuegoPacket;
import com.rolflin.toodeepseas.net.Network;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * La pantalla del minijuego (176×166): baja el anzuelo con A/D (o las flechas), S para bajar rápido y Espacio, E, Esc o
 * clic derecho para recoger y salir con el botín. La partida corre aquí, a paso fijo, y al terminar se envían las teclas
 * al servidor, que la repite. El dibujo es provisional: rectángulos, sin texturas.
 */
public class MinijuegoScreen extends Screen {
    private static final int W = 176, H = 166;
    /** Subdivisiones por píxel del minijuego, para que el movimiento sea suave. */
    private static final int Q = 4;
    private static final double PPB = Partida.PPB;
    private static final int[] RAREZA = {0xFFD9D9D9, 0xFF5FE05F, 0xFF58B0FF, 0xFFC98BFF, 0xFFFFAC2A};
    private static final int[][] AGUA = {{38, 92, 170}, {28, 150, 178}, {70, 112, 175}, {52, 104, 150}, {62, 88, 52}, {206, 84, 20}, {24, 13, 42}};
    private static final int[][] FONDO = {null, null, null, null, null, {84, 14, 6}, {5, 2, 12}};
    private static final int[][] ROCA = {{78, 86, 100}, {196, 108, 118}, {150, 196, 226}, {100, 94, 82}, {74, 62, 42}, {112, 50, 50}, {219, 222, 158}};
    private static final int[][] ROCA_SUB = {{112, 50, 50}, {74, 74, 86}, {88, 68, 54}, {124, 34, 42}, {36, 104, 98}};
    private static final int[][] ARENA = {{196, 186, 140}, {232, 214, 160}, {222, 232, 242}, {150, 130, 100}, {92, 82, 52}, {58, 24, 22}, {0, 0, 0}};
    private static final int[] CRUZA = {0xFFA9BDD0, 0xFFC8D0B0, 0xFF8A8F99, 0xFF7C8F4A, 0xFF6A5A38, 0xFFF4F4F4, 0xFFF4F4F4};
    private static final int[][] TINTE = {{0, 0, 0}, {0, 14, 4}, {26, 0, 40}, {0, 0, 12}, {46, 4, 0}};
    private static final int[] ZONAS = {0, 20, 45, 70, 95};

    private final Partida p;
    private final Grabacion grabacion = new Grabacion();
    private final Bioma bioma;
    private boolean enviado, pedirRecoger;
    private long ultimo = -1;
    private double acc, finT, cartelT = 2.2;
    private int zonaVista, capturaVista = -1;
    private GuiGraphics g;

    public MinijuegoScreen(Partida.Ajustes ajustes) {
        super(Component.translatable("minijuego.toodeepseas.titulo"));
        this.p = new Partida(ajustes, Sesiones.objetos(), true);
        this.bioma = ajustes.bioma();
    }

    /** Golpe externo (lo pide el servidor) o tecla de recoger. */
    public void recoger() {
        pedirRecoger = true;
    }

    private boolean terminada() {
        return p.estado == Estado.FIN || (p.estado == Estado.MUERTO && finT > 1.2);
    }

    // ---------- Juego ----------

    private int teclas() {
        long win = Minecraft.getInstance().getWindow().getWindow();
        int m = 0;
        if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_A) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT)) m |= Partida.IZQ;
        if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_D) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT)) m |= Partida.DER;
        if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_S) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_DOWN)) m |= Partida.BAJAR;
        if (pedirRecoger) m |= Partida.RECOGER;
        return m;
    }

    private void paso() {
        if (p.estado == Estado.FIN || p.estado == Estado.MUERTO) {
            finT += Partida.DT;
            return;
        }
        Estado antes = p.estado;
        int m = teclas();
        if (!enviado) grabacion.add(m);
        p.paso(m);
        if (antes == Estado.CAST && p.estado == Estado.PLAY) sonido(SoundEvents.FISHING_BOBBER_SPLASH, 1F);
        if (p.pasoCaptura == p.pasos) sonido(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9F + p.objs.get(p.ultimaCaptura).rarity() * 0.15F);
        if (p.zona > zonaVista) {
            zonaVista = p.zona;
            cartelT = 2.6;
        }
        if (cartelT > 0) cartelT -= Partida.DT;
        if (!enviado && p.estado != Estado.CAST && p.estado != Estado.PLAY) {
            enviar();
            if (p.estado == Estado.MUERTO) sonido(SoundEvents.ITEM_BREAK, 0.8F);
            else sonido(SoundEvents.FISHING_BOBBER_RETRIEVE, 1F);
        }
    }

    private static void sonido(net.minecraft.sounds.SoundEvent evento, float tono) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(evento, tono));
    }

    private void enviar() {
        enviado = true;
        byte[] datos = grabacion.bytes();
        if (datos.length > FinMinijuegoPacket.MAX_BYTES) datos = java.util.Arrays.copyOf(datos, FinMinijuegoPacket.MAX_BYTES - FinMinijuegoPacket.MAX_BYTES % 3);
        Network.CHANNEL.sendToServer(new FinMinijuegoPacket(datos));
    }

    @Override
    public void removed() {
        if (!enviado) enviar(); // pantalla cerrada a la fuerza: el servidor recoge donde estaba
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (terminada()) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_E
                || Minecraft.getInstance().options.keyInventory.matches(key, scan)) pedirRecoger = true;
        return true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (terminada()) onClose();
        else if (button == 1) pedirRecoger = true;
        return true;
    }

    // ---------- Dibujo ----------

    private static int rgb(double r, double gr, double b, double a) {
        return ((int) Math.max(0, Math.min(255, a * 255)) << 24) | ((int) Math.max(0, Math.min(255, r)) << 16) | ((int) Math.max(0, Math.min(255, gr)) << 8)
                | (int) Math.max(0, Math.min(255, b));
    }

    private static int alfa(int color, double a) {
        return (color & 0xFFFFFF) | ((int) Math.max(0, Math.min(255, a * 255)) << 24);
    }

    private static double lerp(double a, double b, double f) {
        return a + (b - a) * f;
    }

    private void rect(double x, double y, double w, double h, int color) {
        int x0 = (int) Math.round(x * Q), y0 = (int) Math.round(y * Q), x1 = (int) Math.round((x + w) * Q), y1 = (int) Math.round((y + h) * Q);
        if (x1 > x0 && y1 > y0) g.fill(x0, y0, x1, y1, color);
    }

    private void disco(double cx, double cy, double r, int color) {
        for (double dy = -r; dy < r; dy += 0.5) {
            double hw = Math.sqrt(Math.max(0, r * r - (dy + 0.25) * (dy + 0.25)));
            rect(cx - hw, cy + dy, 2 * hw, 0.5, color);
        }
    }

    private void linea(double x1, double y1, double x2, double y2, double grosor, int color) {
        double dx = x2 - x1, dy = y2 - y1;
        int n = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / Math.max(0.5, grosor)) + 1;
        for (int i = 0; i <= n; i++) {
            double f = (double) i / n;
            rect(x1 + dx * f - grosor / 2, y1 + dy * f - grosor / 2, grosor, grosor, color);
        }
    }

    private static int zona(double d) {
        return Partida.zoneAt(d);
    }

    private double[] agua(double d) {
        int[] a = AGUA[bioma.ordinal()], deep = FONDO[bioma.ordinal()];
        if (deep != null) {
            double f = Math.min(1, d / (bioma == Bioma.VACIO ? 60 : 130));
            return new double[]{lerp(a[0], deep[0], f), lerp(a[1], deep[1], f), lerp(a[2], deep[2], f)};
        }
        double f = Math.min(1, d / 55), k = p.aj.clima() == Clima.DESPEJADO ? 1 : 0.82;
        int z = zona(d);
        int[] ta = TINTE[Math.max(0, z - 1)], tb = TINTE[z];
        double m = Math.min(1, (d - ZONAS[z]) / 5);
        double[] t = {lerp(ta[0], tb[0], m), lerp(ta[1], tb[1], m), lerp(ta[2], tb[2], m)};
        if (bioma == Bioma.HELADO && z >= 4) t = new double[]{0, 8, 36};
        return new double[]{lerp(a[0] * k, 8, f) + t[0], lerp(a[1] * k, 10, f) + t[1], lerp(a[2] * k, 22, f) + t[2]};
    }

    private int roca(double wy, double brillo) {
        int[] c = bioma == Bioma.LAVA ? ROCA_SUB[p.aj.sub().ordinal()] : ROCA[bioma.ordinal()];
        double f = (1 - Math.min(0.55, wy / PPB / 90)) * brillo;
        return rgb(c[0] * f, c[1] * f, c[2] * f, 1);
    }

    private void mundo() {
        double cam = p.cam, t = p.t;
        // cielo y agua
        int cielo = bioma == Bioma.LAVA ? 0xFF2A0D0B : bioma == Bioma.VACIO ? 0xFF150B26
                : p.aj.clima() == Clima.TORMENTA ? 0xFF40464F : p.aj.clima() == Clima.LLUVIA ? 0xFF7A8796 : 0xFF8EC8FF;
        for (int y = 0; y < H; y += 2) {
            double wy = cam + y;
            if (wy < 0) rect(0, y, W, 2, cielo);
            else {
                double[] c = agua(wy / PPB);
                rect(0, y, W, 2, rgb(c[0], c[1], c[2], 1));
            }
        }
        double sy = -cam;
        if (sy > -3 && sy < H + 3) for (int x = 0; x < W; x += 2) rect(x, sy + Math.sin(x * 0.25 + t * 3) * 0.8 - 0.5, 2, 1, 0x66FFFFFF);
        if (bioma == Bioma.VACIO) for (int i = 0; i < 40; i++) { // estrellas
            double y = ((i * 53 % H) - cam * 0.12) % H;
            if (y < 0) y += H;
            rect(i * 37 % W, y, 1, 1, alfa(0xE6D7FF, 0.3 + 0.5 * Math.abs(Math.sin(t * 0.8 + i))));
        }
        List<Trozo> vis = p.visibles();
        // franjas
        for (Trozo c : vis)
            for (Franja cu : c.cur) {
                double y0 = cu.y - cam;
                if (y0 > H || y0 + cu.h < 0) continue;
                if (cu.fx == Fx.PUSH) {
                    int col = bioma == Bioma.VACIO ? 0x80D796FF : 0x40DCF0FF;
                    for (int j = 0; j < 8; j++) {
                        double yy = y0 + (j + 0.5) * cu.h / 8, xx = (((j * 61 + (int) cu.y * 7) % W + t * cu.f * cu.dir) % W + W) % W;
                        rect(xx, yy, 8, 0.5, col);
                        rect(xx + (cu.dir > 0 ? 7 : 0), yy - 0.5, 1, 1.5, col);
                    }
                } else {
                    boolean lv = bioma == Bioma.LAVA;
                    int col = switch (cu.fx) {
                        case SLOWX -> 0x2996D7FF;
                        case BLIZZARD -> 0x24EBF5FF;
                        case FAST -> lv ? 0x4D785F4B : 0x24C8EBFF;
                        case SLOWY -> 0x66280E0A;
                        case FOG -> 0x3896A578;
                        default -> 0x80E65AFF;
                    };
                    rect(0, y0, W, cu.h, col);
                    rect(0, y0, W, 0.5, alfa(col, 0.6));
                    rect(0, y0 + cu.h - 0.5, W, 0.5, alfa(col, 0.6));
                }
            }
        // paredes
        for (int y = 0; y < H; y++) {
            double wy = cam + y;
            if (wy < PPB * 2.4) continue;
            if (wy > p.floorY + 4) break;
            double[][] iv = p.terreno.spansAt(wy);
            int rc = roca(wy, 1), borde = roca(wy, 1.65);
            double px = 0;
            for (double[] s : iv) {
                if (s[0] > px) rect(px, y, s[0] - px, 1, rc);
                px = s[1];
            }
            if (px < W) rect(px, y, W - px, 1, rc);
            for (double[] s : iv) {
                if (s[0] > 0.5) rect(s[0] - 1.5, y, 1.5, 1, borde);
                if (s[1] < W - 0.5) rect(s[1], y, 1.5, 1, borde);
            }
        }
        // fondo
        double fy = p.floorY - cam;
        if (fy < H + 6) {
            int[] a = ARENA[bioma.ordinal()];
            double k = 0.75 - Math.min(0.4, p.aj.maxD() / 150);
            for (int x = 0; x < W; x += 2) rect(x, fy - Math.sin(x * 0.15) * 1.5 - 1.5, 2, H, rgb(a[0] * k, a[1] * k, a[2] * k, 1));
        }
        for (Trozo c : vis) {
            for (Partida.Decor o : c.decor) decor(o, cam, t);
            for (Roca o : c.rocks) if (o.y - cam > -14 && o.y - cam < H + 14) roca(o, o.y - cam, t);
            for (Cosa it : c.items) if (!it.got && it.y - cam > -10 && it.y - cam < H + 10) cosa(it, it.y - cam, t);
            for (Peligro h : c.haz) if (!h.dead) peligro(h, cam, t);
        }
        // sedal y anzuelo
        double hx = p.hookX, hy = p.hookY - cam;
        if (p.estado != Estado.MUERTO) {
            double lx = p.rope.get(0)[0], ly = p.rope.get(0)[1] - cam;
            for (int i = 1; i <= p.rope.size(); i++) {
                double nx = i < p.rope.size() ? p.rope.get(i)[0] : hx, ny = i < p.rope.size() ? p.rope.get(i)[1] - cam : hy - 3;
                double a = Math.max(-4, ly), b = Math.min(H + 4, ny);
                if (b > a && ny != ly) linea(lx + (nx - lx) * (a - ly) / (ny - ly), a, lx + (nx - lx) * (b - ly) / (ny - ly), b, 0.5, 0xFFDCDCDC);
                lx = nx;
                ly = ny;
            }
            if (p.aj.attractR() > 0 && p.estado == Estado.PLAY) for (int i = 0; i < 16; i++) {
                double a = i / 16.0 * 6.283 + t;
                rect(hx + Math.cos(a) * p.aj.attractR(), hy + Math.sin(a) * p.aj.attractR(), 0.75, 0.75, 0x50FFDC78);
            }
            anzuelo(hx, hy, 0xFFD5DDE3);
        } else if (finT < 1.2) {
            anzuelo(hx + Math.sin(finT * 40) * 1.5 * (1 - finT), hy + finT * finT * 30, (int) (finT * 12) % 2 == 0 ? 0xFFFFFFFF : 0xFFFF6A5A);
            for (int i = 0; i < 10; i++) {
                double a = i / 10.0 * 6.283, d = finT * 45;
                rect(hx + Math.cos(a) * d, hy + Math.sin(a) * d, 1, 1, alfa(0xFFE0A0, 1 - finT / 1.2));
            }
        }
        // oscuridad y niebla
        double d = p.profundidad();
        double osc = bioma == Bioma.VACIO ? 0 : bioma == Bioma.LAVA ? Math.min(0.2, Math.max(0, d - 15) / 200) : Math.min(zona(d) >= 3 ? 0.4 : 0.3, Math.max(0, d - 15) / 120);
        if (bioma == Bioma.PANTANO) osc = Math.max(osc, 0.15);
        osc = Math.max(osc, p.fog * 0.6);
        if (osc > 0.01) {
            // se ve mejor cerca del anzuelo: cuatro bandas alrededor en vez de un velo uniforme
            double r = lerp(46, 26, p.fog);
            int col = alfa(0, osc), col2 = alfa(0, osc * 0.45);
            rect(0, 0, W, hy - r, col);
            rect(0, hy + r, W, H, col);
            rect(0, hy - r, hx - r, 2 * r, col);
            rect(hx + r, hy - r, W, 2 * r, col);
            rect(hx - r, hy - r, 2 * r, 2 * r, col2);
            disco(hx, hy, r * 0.6, alfa(0xFFFFFF, 0.04));
        }
        if (p.estado == Estado.MUERTO && finT < 0.25) rect(0, 0, W, H, alfa(0xFF3020, 0.5 * (1 - finT / 0.25)));
    }

    private void anzuelo(double x, double y, int color) {
        rect(x - 0.5, y - 3, 1, 4, color);
        rect(x - 2.5, y + 1, 2.5, 1, color);
        rect(x - 3, y - 0.5, 1, 2, color);
    }

    private void decor(Partida.Decor o, double cam, double t) {
        double y = o.y - cam, x = o.x;
        if (y < -14 || y > H + 14) return;
        int dir = -o.side;
        if (bioma == Bioma.LAVA) {
            rect(x + (dir > 0 ? 0 : -2), y, 2, 1, alfa(0xFF7828, 0.55 + 0.35 * Math.sin(t * 3 + o.s)));
            return;
        }
        switch (o.z) {
            case 0 -> { // algas
                int col = bioma == Bioma.HELADO ? 0xD0EBF8FF : bioma == Bioma.PANTANO ? 0xF05A4628 : 0xE046A050;
                for (int k = 0; k < 4; k++) rect(x + dir * (k * 0.6 + 0.3) + Math.sin(t * 2 + o.s % 10) * 0.2 * k, y - k * 2.2, 1, 2.2, col);
            }
            case 1 -> {
                int col = bioma == Bioma.CALIDO ? new int[]{0xFFFF7A8A, 0xFFFFA04A, 0xFFC06AFF}[o.s % 3] : bioma == Bioma.HELADO ? 0xE6E6F6FF : 0xF06E8C32;
                for (int k = 0; k < 6; k++) rect(x + dir * (1 + Math.sin(k + t * 1.5 + o.s) * 1.2), y - k * 2.5, 1.2, 2.5, col);
            }
            case 2 -> {
                disco(x + dir * 2, y, 4, alfa(0xC88CFF, 0.15 + 0.1 * Math.sin(t * 2 + o.s)));
                rect(x + (dir > 0 ? 0 : -4), y - 1, 4, 2, 0xF0CD96FF);
            }
            case 3 -> rect(x + dir, y, 1, 1, alfa(0x5AE6FF, 0.4 + 0.6 * Math.abs(Math.sin(t * 1.5 + o.s))));
            default -> rect(x + (dir > 0 ? 0 : -3), y, 3, 1, bioma == Bioma.HELADO ? 0xE6E6F6FF : alfa(0xFF7828, 0.55 + 0.35 * Math.sin(t * 3 + o.s)));
        }
    }

    private void roca(Roca o, double y, double t) {
        switch (o.kind) {
            case TRONCO -> {
                disco(o.x, y, o.r, 0xFF5A4026);
                disco(o.x, y, o.r * 0.5, 0xFF33230F);
                disco(o.x, y, o.r * 0.25, 0xFF5A4026);
            }
            case RAIZF -> {
                for (int i = 0; i < 5; i++) {
                    double a = i / 5.0 * 6.283 + (o.s % 7);
                    linea(o.x, y, o.x + Math.cos(a) * o.r, y + Math.sin(a) * o.r, 1.2, 0xFF4A3520);
                }
                disco(o.x, y, o.r * 0.45, 0xFF3A2A18);
            }
            case FOSIL -> {
                rect(o.x - o.r * 0.95, y - o.r * 0.2 - 0.6, o.r * 1.9, 1.2, 0xFFD8D0B8);
                for (int i = -2; i <= 2; i++) rect(o.x + i * o.r * 0.38 - 0.5, y - o.r * 0.2, 1, o.r * 1.1, 0xFFD8D0B8);
                disco(o.x - o.r * 0.6, y - o.r * 0.5, o.r * 0.32, 0xFFE6DEC8);
            }
            case CORALF -> {
                disco(o.x, y, o.r + 3, alfa(0xFF6E28, 0.2 + 0.1 * Math.sin(t * 3 + o.s)));
                for (int i = 0; i < 6; i++) {
                    double a = i / 6.0 * 6.283 + (o.s % 5);
                    linea(o.x, y, o.x + Math.cos(a) * o.r, y + Math.sin(a) * o.r, 1.2, 0xFFFF6A2A);
                }
                disco(o.x, y, 1.4, 0xFFFFD070);
            }
            case ANEMONA -> {
                boolean cerrada = p.anemClosed(o);
                double ph = (t * 0.45 + (o.s % 100) / 100.0) % 1;
                if (cerrada) {
                    disco(o.x, y, o.r, 0xFFC83A8A);
                    for (int i = 0; i < 10; i++) {
                        double a = i / 10.0 * 6.283;
                        rect(o.x + Math.cos(a) * (o.r + 1) - 0.5, y + Math.sin(a) * (o.r + 1) - 0.5, 1, 1, 0xFFFFD0E8);
                    }
                } else {
                    int col = ph > 0.86 && Math.sin(t * 30) > 0 ? 0xFFFFFFFF : 0x90FFA0D2; // parpadea justo antes de cerrarse
                    for (int i = 0; i < 10; i++) {
                        double a = i / 10.0 * 6.283;
                        linea(o.x + Math.cos(a) * 1.5, y + Math.sin(a) * 1.5, o.x + Math.cos(a) * (o.r + 2), y + Math.sin(a) * (o.r + 2), 0.7, col);
                    }
                    disco(o.x, y, 1.8, 0x99C83C8C);
                }
            }
            case ERIZO -> {
                for (int i = 0; i < 12; i++) {
                    double a = i / 12.0 * 6.283;
                    linea(o.x + Math.cos(a) * o.r * 0.5, y + Math.sin(a) * o.r * 0.5, o.x + Math.cos(a) * (o.r + 1), y + Math.sin(a) * (o.r + 1), 0.7, 0xFF2A1A2E);
                }
                disco(o.x, y, o.r * 0.6, 0xFF4A2A52);
            }
            case CRISTAL -> {
                disco(o.x, y, o.r + 2, alfa(0xBE78FF, 0.12 + 0.08 * Math.sin(t * 2 + o.s)));
                for (int i = 0; i < 5; i++) {
                    double a = -1.57 + (i - 2) * 0.55, len = o.r * (i % 2 != 0 ? 0.8 : 1.2);
                    linea(o.x, y + o.r * 0.3, o.x + Math.cos(a) * len, y + Math.sin(a) * len, 1.4, 0xFFA070E0);
                }
                rect(o.x - o.r * 0.9, y + o.r * 0.3, o.r * 1.8, o.r * 0.6, 0xFF6A4A9A);
            }
            default -> {
                disco(o.x, y, o.r, roca(o.y, 1));
                rect(o.x - o.r * 0.4, y - o.r * 0.6, o.r * 0.6, 1, roca(o.y, 1.35));
            }
        }
    }

    private void cosa(Cosa it, double y, double t) {
        Objeto def = p.objs.get(it.id);
        double x = it.x;
        int f = it.face == 0 ? 1 : it.face;
        if (def.rarity() >= 2 && !it.hid) { // aro del color de la rareza
            int col = alfa(RAREZA[def.rarity()], 0.45 + 0.3 * Math.sin(t * 4 + it.x));
            double r = it.r + 2.5;
            for (int i = 0; i < 12; i++) {
                double a = i / 12.0 * 6.283 + t;
                rect(x + Math.cos(a) * r - 0.4, y + Math.sin(a) * r - 0.4, 0.8, 0.8, col);
            }
        }
        if (def.tesoro() != null) {
            int idx = Math.max(0, Math.min(TESORO_FORMA.length - 1, def.tesoro().order()));
            tesoro(TESORO_FORMA[idx], 0xFF000000 | TESORO_COLOR[idx], x, y, t);
            if (Math.sin(t * 3 + x) > 0.7) {
                rect(x + 3, y - 4, 1, 3, 0xFFFFFFFF);
                rect(x + 2, y - 3, 3, 1, 0xFFFFFFFF);
            }
            return;
        }
        if (def.puff()) {
            if (it.pst == Partida.PWARN) x += Math.sin(it.vph) * (0.6 + 0.8 * (1 - it.ptm / 1.4));
            double r = Partida.puffR(it);
            boolean grande = it.pst == Partida.BIG;
            for (int i = 0; i < (grande ? 10 : 8); i++) {
                double a = i / (grande ? 10.0 : 8.0) * 6.283 + (grande ? t * 0.5 : 0);
                linea(x + Math.cos(a) * r * 0.8, y + Math.sin(a) * r * 0.8, x + Math.cos(a) * (r + 2), y + Math.sin(a) * (r + 2), 0.6, 0xFFF6EEC0);
            }
            disco(x, y, r, it.pst == Partida.PWARN && Math.sin(it.vph) > 0 ? 0xFFFFF0A0 : 0xFFE6D34A);
            rect(x + f * r * 0.45 - 0.5, y - 1.2, 1.2, 1.2, 0xFF111111);
            return;
        }
        if (it.hid) { // emboscada: escondido, solo se le ven los ojos
            int col = alfa(0xFFF096, 0.5 + 0.3 * Math.sin(t * 2 + it.ph));
            rect(x - f * 1.5, y - 1.6, 1, 1, col);
            rect(x - f * 1.5, y + 0.6, 1, 1, col);
            return;
        }
        int col = 0xFF000000 | def.pez().color();
        if (def.pez().ghost()) col = alfa(col, 0.45 + 0.3 * Math.sin(t * 3 + it.ph));
        String forma = def.pez().shape() == null ? "" : def.pez().shape();
        switch (forma) {
            case "raya" -> {
                rect(x - 4, y - 0.8, 8, 1.6, col);
                rect(x - 2, y - 1.6, 4, 3.2, col);
                rect(x - f * 7, y - 0.3, 3, 0.6, col);
            }
            case "anguila" -> {
                for (int i = 0; i < 9; i++) rect(x - f * (i - 4) - 0.6, y + Math.sin(t * 6 + i * 0.8 + it.ph) * 1.1 - 0.8, 1.2, 1.6, col);
            }
            case "calamar", "medusa" -> {
                rect(x - 2, y - 3, 4, 3.5, col);
                rect(x - 1.5, y - 3.6, 3, 0.8, col);
                for (int i = -1; i <= 1; i++) rect(x + i * 1.4 - 0.3 + Math.sin(t * 4 + i + it.ph) * 0.4, y + 0.5, 0.6, 3, col);
            }
            case "caballito" -> {
                rect(x - 1, y - 3, 2, 5, col);
                rect(x + f * 0.5, y - 3, 2, 1.2, col);
                rect(x - f * 1.5, y + 1.5, 1.5, 1, col);
            }
            case "rape" -> {
                rect(x - 3, y - 2.2, 6, 4.4, col);
                rect(x - f * 5, y - 1, 2, 2, col);
                rect(x + f * 2, y - 4, 0.6, 2, col);
                rect(x + f * 2.6, y - 4.6, 1.2, 1.2, 0xFFFFF0A0);
            }
            default -> {
                int base = 0xFF000000 | def.pez().color();
                double ga = def.pez().ghost() ? 0.45 + 0.3 * Math.sin(t * 3 + it.ph) : 1;
                int oscuro = alfa(mul(base, 0.62), ga), claro = alfa(mul(base, 1.3), ga);
                double wag = Math.sin(t * 9 + it.ph);
                for (int i = 0; i < 3; i++) { // cola que se mueve
                    double h = 1.1 + i * 0.9;
                    rect(x - f * (4.4 + i * 1.1) - 0.55, y - h + wag * i * 0.45, 1.1, 2 * h, i == 2 ? oscuro : col);
                }
                rect(x - 1.5, y - 3.5, 3.2, 1.3, oscuro); // aleta dorsal
                elipse(x, y, 4.4, 2.5, col);
                elipse(x + f * 0.4, y + 1.1, 3.2, 1.0, claro); // vientre
                rect(x - f * 0.5 - 0.3, y - 0.2, 1.2, 1.8, oscuro); // aleta lateral
                rect(x + f * 1.3 - 0.2, y - 1.6, 0.5, 3.2, alfa(0x000000, 0.25 * ga)); // agalla
                rect(x + f * 2.3 - 0.9, y - 1.5, 1.8, 1.8, 0xFFFFFFFF); // ojo
            }
        }
        rect(x + f * (forma.isEmpty() ? 2.5 : 1.4) - 0.4, y - 1, 0.9, 0.9, 0xFF101010);
    }

    private static int mul(int c, double f) {
        return 0xFF000000 | ((int) Math.min(255, ((c >> 16) & 255) * f) << 16) | ((int) Math.min(255, ((c >> 8) & 255) * f) << 8) | (int) Math.min(255, (c & 255) * f);
    }

    private void elipse(double cx, double cy, double rx, double ry, int color) {
        for (double dy = -ry; dy < ry; dy += 0.5) {
            double q = (dy + 0.25) / ry, k = 1 - q * q;
            if (k <= 0) continue;
            double hw = rx * Math.sqrt(k);
            rect(cx - hw, cy + dy, 2 * hw, 0.5, color);
        }
    }

    // Cada tesoro del catálogo (en el orden del prototipo) tiene su forma y su color
    private static final String[] TESORO_FORMA = {"botella", "pepita", "cofre", "concha", "corona", "perla", "cristal", "tridente", "gota", "corazon", "moneda",
            "coral", "ancla", "geoda", "caliz", "anfora", "idolo", "gema", "lagrima", "corona_rey", "huevo", "cuarzo", "pepitas", "escama", "arca", "plantilla",
            "restos", "fragmento", "shulker", "frasco", "cabeza", "elitros", "trofeo"};
    private static final int[] TESORO_COLOR = {0x7FD1B9, 0xFFD24A, 0xA0672E, 0xF0D8B0, 0xD8C040, 0x3A3050, 0xA8E8FF, 0x5FA090, 0x90FF60, 0x3AD0FF, 0xFFD23A,
            0xFF4A5A, 0x8A949C, 0xC08AFF, 0xE8C040, 0xB8683A, 0x3AFF9A, 0x5A7AFF, 0xE0F0FF, 0xFFD23A, 0x7AD0C0, 0xF0E6E0, 0xFFD24A, 0xFF7A2A, 0x3A3038, 0x4A4450,
            0x6A4A40, 0xB070FF, 0x9A6A9A, 0xE070D0, 0x3A3A44, 0x8A7AB8, 0xD0B0FF};

    /** Dibuja un tesoro: cada uno con su forma, su color, su sombra y su brillo. */
    private void tesoro(String forma, int c, double x, double y, double t) {
        int d = mul(c, 0.55), l = mul(c, 1.45);
        double pulso = 0.5 + 0.5 * Math.sin(t * 3 + x);
        switch (forma) {
            case "botella" -> {
                rect(x - 2.2, y - 1.2, 4.4, 5.4, d);
                rect(x - 1.7, y - 0.7, 3.4, 4.4, alfa(c, 0.9));
                rect(x - 0.9, y - 3.6, 1.8, 2.8, c);
                rect(x - 0.9, y - 4.4, 1.8, 1, 0xFFA0672E);
                rect(x - 0.6, y - 0.2, 1.2, 2.6, 0xFFF5ECD0);
            }
            case "pepita" -> {
                disco(x, y + 0.3, 2.7, d);
                disco(x - 0.3, y, 2.2, c);
                rect(x - 1.3, y - 1.4, 1.2, 0.9, l);
            }
            case "cofre" -> {
                rect(x - 4.4, y - 3.4, 8.8, 6.8, 0xFF2A1C0E);
                rect(x - 3.9, y - 2.9, 7.8, 5.8, c);
                rect(x - 3.9, y - 2.9, 7.8, 2, l);
                rect(x - 3.9, y - 0.9, 7.8, 0.8, 0xFF2A1C0E);
                rect(x - 3.9, y - 0.3, 7.8, 1, 0xFFE8C24A);
                rect(x - 0.7, y - 0.6, 1.4, 2.2, 0xFFFFE070);
            }
            case "concha" -> {
                for (double dy = -3.6; dy < 0; dy += 0.5) {
                    double hw = Math.sqrt(Math.max(0, 3.8 * 3.8 - (dy + 0.25) * (dy + 0.25)));
                    rect(x - hw, y + 1.8 + dy, 2 * hw, 0.5, c);
                }
                rect(x - 3.8, y + 1.8, 7.6, 0.8, d);
                for (int i = -2; i <= 2; i++) linea(x, y + 1.8, x + i * 1.5, y - 1.6, 0.4, d);
            }
            case "corona" -> {
                rect(x - 4, y, 8, 2.6, c);
                rect(x - 4, y - 3, 1.6, 3.2, c);
                rect(x - 0.8, y - 3.8, 1.6, 4, c);
                rect(x + 2.4, y - 3, 1.6, 3.2, c);
                rect(x - 4, y + 2.2, 8, 0.6, d);
                rect(x - 0.5, y + 0.6, 1, 1, 0xFFD83A3A);
            }
            case "perla" -> {
                disco(x, y, 3, 0xFF9088B8);
                disco(x, y, 2.5, c);
                rect(x - 1.4, y - 1.6, 1.1, 1.1, 0xFFFFFFFF);
                rect(x + 0.4, y + 0.6, 0.6, 0.6, 0xFF6A5C90);
            }
            case "cristal" -> {
                rect(x - 2.2, y - 2, 4.4, 4.8, d);
                rect(x - 1.8, y - 2, 3.6, 4.4, c);
                rect(x - 1.4, y - 3.6, 2.8, 1.8, c);
                rect(x - 0.7, y - 4.4, 1.4, 1, c);
                rect(x - 1.2, y - 2.5, 0.8, 4.5, l);
            }
            case "tridente" -> {
                rect(x - 0.6, y - 1, 1.2, 5.6, c);
                rect(x - 2.6, y - 1.6, 5.2, 1.1, c);
                rect(x - 2.6, y - 4.6, 1.1, 3.2, c);
                rect(x - 0.6, y - 5.2, 1.2, 3.8, c);
                rect(x + 1.5, y - 4.6, 1.1, 3.2, c);
                rect(x - 0.4, y + 1, 0.8, 0.8, d);
                rect(x - 2.4, y - 3, 0.6, 0.6, d);
            }
            case "gota" -> {
                disco(x, y + 1, 2.7, d);
                disco(x, y + 1, 2.3, c);
                rect(x - 0.7, y - 3.4, 1.4, 2.4, c);
                rect(x - 1.4, y, 0.9, 1.2, l);
                rect(x + 0.6, y + 1.6, 0.7, 0.7, d);
                disco(x, y + 1, 4.2, alfa(c, 0.12 + 0.1 * pulso));
            }
            case "corazon" -> {
                disco(x, y, 4.8, alfa(c, 0.12 + 0.12 * pulso));
                disco(x, y, 3.2, d);
                disco(x, y, 2.8, c);
                disco(x, y, 1.6, l);
                disco(x, y, 0.8, c);
                rect(x - 2, y - 2.2, 1, 0.8, 0xFFFFFFFF);
            }
            case "moneda" -> {
                disco(x, y, 3, 0xFFB8901A);
                disco(x, y, 2.5, c);
                disco(x, y, 1.6, 0xFFB8901A);
                disco(x, y, 1.2, c);
                rect(x - 1.8, y - 1.8, 0.9, 0.9, l);
            }
            case "coral" -> {
                linea(x, y + 3.2, x, y - 1, 1.3, c);
                linea(x, y - 1, x - 2.6, y - 3.6, 1.2, c);
                linea(x, y - 1, x + 2.6, y - 3.2, 1.2, c);
                linea(x, y + 1.2, x + 2.2, y + 0.2, 1, c);
                rect(x - 3.2, y - 4.2, 1, 1, l);
                rect(x + 2.2, y - 3.8, 1, 1, l);
                rect(x - 2, y + 3, 4, 0.8, d);
            }
            case "ancla" -> {
                rect(x - 0.6, y - 3.6, 1.2, 6.6, c);
                rect(x - 2.2, y - 2.6, 4.4, 1, c);
                rect(x - 1, y - 4.6, 2, 1, c);
                for (double a = 0.3; a < Math.PI - 0.3; a += 0.25) rect(x + Math.cos(a) * 2.8 - 0.5, y + 0.5 + Math.sin(a) * 2.6 - 0.5, 1, 1, c);
                rect(x - 3.4, y + 0.6, 1, 1, c);
                rect(x + 2.4, y + 0.6, 1, 1, c);
                rect(x - 0.4, y - 3.4, 0.5, 4, l);
            }
            case "geoda" -> {
                disco(x, y, 3.6, 0xFF6E6A78);
                disco(x, y, 2.6, d);
                disco(x, y, 2, c);
                rect(x - 0.7, y - 1.8, 1.2, 2.6, l);
                rect(x + 0.6, y - 0.8, 1, 1.8, l);
                rect(x - 1.8, y - 0.4, 0.9, 1.4, l);
            }
            case "caliz" -> {
                rect(x - 3, y - 3.2, 6, 1.2, c);
                rect(x - 2.5, y - 2, 5, 1, c);
                rect(x - 1.5, y - 1, 3, 1, c);
                rect(x - 0.6, y, 1.2, 2.5, c);
                rect(x - 2.2, y + 2.4, 4.4, 1, d);
                rect(x - 2.4, y - 2.8, 1, 0.7, l);
                rect(x - 0.7, y - 2.4, 1.4, 0.8, 0xFFC03030);
            }
            case "anfora" -> {
                rect(x - 1.2, y - 3.8, 2.4, 2.8, c);
                rect(x - 1.8, y - 4.4, 3.6, 0.9, d);
                elipse(x, y + 1, 2.8, 3, c);
                rect(x - 3.5, y - 2.6, 1, 2.8, d);
                rect(x + 2.5, y - 2.6, 1, 2.8, d);
                rect(x - 2.8, y + 0.3, 5.6, 0.7, d);
                rect(x - 1, y - 2, 2, 1.3, 0xFFC03030);
                rect(x - 1.8, y + 1.4, 0.9, 1.6, l);
            }
            case "idolo" -> {
                disco(x, y, 4.5, alfa(c, 0.1 + 0.15 * pulso));
                rect(x - 2.8, y - 4, 5.6, 7.6, d);
                rect(x - 2.3, y - 3.5, 4.6, 6.6, c);
                rect(x - 1.5, y - 2.2, 1, 1, 0xFF101010);
                rect(x + 0.5, y - 2.2, 1, 1, 0xFF101010);
                rect(x - 1.2, y, 2.4, 0.8, 0xFF101010);
                rect(x - 1.2, y + 1.6, 2.4, 0.6, d);
            }
            case "gema" -> {
                rect(x - 2, y - 3.2, 4, 1.3, l);
                rect(x - 3.4, y - 1.9, 6.8, 1.5, c);
                rect(x - 2.6, y - 0.4, 5.2, 1, c);
                rect(x - 1.8, y + 0.6, 3.6, 1, d);
                rect(x - 1, y + 1.6, 2, 1, d);
                rect(x - 0.4, y + 2.6, 0.8, 0.8, d);
                rect(x - 1.6, y - 2.8, 1, 0.8, 0xFFFFFFFF);
                disco(x, y, 5, alfa(c, 0.08 + 0.1 * pulso));
            }
            case "lagrima" -> {
                disco(x, y + 1.2, 2.6, c);
                rect(x - 1, y - 2.6, 2, 2, c);
                rect(x - 0.5, y - 4, 1, 1.6, c);
                rect(x - 1.6, y + 0.2, 0.9, 1.6, 0xFFFFFFFF);
                disco(x, y + 0.5, 5, alfa(c, 0.1 + 0.1 * pulso));
            }
            case "corona_rey" -> {
                rect(x - 4.6, y, 9.2, 3, c);
                for (int i = -2; i <= 2; i++) rect(x + i * 2 - 0.7, y - 3.2 - (i == 0 ? 1.4 : 0), 1.4, 3.4 + (i == 0 ? 1.4 : 0), c);
                rect(x - 4.6, y + 2.4, 9.2, 0.6, d);
                rect(x - 3.2, y + 0.8, 1, 1, 0xFFD83A3A);
                rect(x - 0.5, y + 0.8, 1, 1, 0xFF3AD870);
                rect(x + 2.2, y + 0.8, 1, 1, 0xFF3A7AFF);
                rect(x - 0.6, y - 4.8, 1.2, 1.2, 0xFFFFFFFF);
            }
            case "huevo" -> {
                elipse(x, y, 2.8, 3.6, d);
                elipse(x, y, 2.4, 3.2, c);
                rect(x - 1.4, y - 2.3, 1, 1.3, l);
                rect(x + 0.5, y + 0.4, 1, 1, 0xFF1C4A4A);
                rect(x - 1, y + 1.6, 1, 1, 0xFF1C4A4A);
                rect(x - 0.4, y - 0.8, 0.8, 0.8, 0xFF1C4A4A);
            }
            case "cuarzo" -> {
                rect(x - 3.2, y - 0.8, 1.9, 4, c);
                rect(x - 2.8, y - 1.8, 1.1, 1.2, l);
                rect(x - 1, y - 3.4, 2.2, 6.4, l);
                rect(x - 0.5, y - 4.4, 1.2, 1.2, 0xFFFFFFFF);
                rect(x + 1.4, y - 0.4, 1.9, 3.4, c);
                rect(x + 1.8, y - 1.4, 1.1, 1.2, l);
                rect(x - 3.2, y + 2.8, 6.6, 0.7, d);
            }
            case "pepitas" -> {
                disco(x - 2, y + 1.3, 1.9, d);
                disco(x - 2, y + 1.2, 1.5, c);
                disco(x + 2, y + 1.4, 1.9, d);
                disco(x + 2, y + 1.3, 1.5, c);
                disco(x, y - 1.3, 2.2, d);
                disco(x, y - 1.4, 1.8, c);
                rect(x - 0.8, y - 2.2, 0.9, 0.7, l);
            }
            case "escama" -> {
                disco(x, y, 4.5, alfa(0xFF7A2A, 0.12 + 0.12 * pulso));
                elipse(x, y, 3.1, 3.8, d);
                elipse(x, y, 2.7, 3.4, c);
                linea(x, y - 3, x, y + 3, 0.5, d);
                linea(x - 2, y - 1.2, x + 2, y - 1.2, 0.5, d);
                linea(x - 1.6, y + 1.2, x + 1.6, y + 1.2, 0.5, d);
                rect(x - 1.6, y - 2.6, 1, 0.8, l);
            }
            case "arca" -> {
                rect(x - 4.4, y - 3.4, 8.8, 6.8, 0xFF14101A);
                rect(x - 3.9, y - 2.9, 7.8, 5.8, c);
                rect(x - 3.4, y - 2.9, 1.1, 5.8, 0xFFD4A832);
                rect(x + 2.3, y - 2.9, 1.1, 5.8, 0xFFD4A832);
                rect(x - 3.9, y - 0.9, 7.8, 0.7, 0xFF14101A);
                rect(x - 0.8, y - 0.8, 1.6, 2, 0xFFD4A832);
            }
            case "plantilla" -> {
                rect(x - 3.2, y - 3.8, 6.4, 7.6, d);
                rect(x - 2.7, y - 3.3, 5.4, 6.6, c);
                rect(x - 1.8, y - 2.4, 3.6, 1.2, l);
                rect(x - 1.8, y - 0.4, 1.4, 1.2, l);
                rect(x + 0.4, y - 0.4, 1.4, 1.2, l);
                rect(x - 1.8, y + 1.6, 3.6, 1, l);
                rect(x - 3.2, y - 0.8, 0.8, 1.6, 0xFF14101A);
            }
            case "restos" -> {
                rect(x - 3.2, y - 2.8, 6.4, 5.6, d);
                rect(x - 2.7, y - 2.3, 5.4, 4.6, c);
                rect(x - 1.2, y - 2.3, 0.8, 3, 0xFFE0A050);
                rect(x + 1, y - 1.2, 0.8, 3.2, 0xFFE0A050);
                rect(x - 2.4, y + 0.8, 1, 1, 0xFF3A2A28);
            }
            case "fragmento" -> {
                disco(x, y, 4.6, alfa(c, 0.12 + 0.14 * pulso));
                rect(x - 1, y - 4.2, 2, 2.4, l);
                rect(x - 2.2, y - 1.8, 4.4, 3, c);
                rect(x - 1.4, y + 1.2, 2.8, 1.4, d);
                rect(x - 0.5, y + 2.6, 1, 1.2, d);
                rect(x - 0.4, y - 3.4, 0.5, 4, 0xFFFFFFFF);
            }
            case "shulker" -> {
                rect(x - 3.6, y - 3.4, 7.2, 3.4, c);
                rect(x - 3.6, y + 0.4, 7.2, 3, d);
                rect(x - 3.6, y - 0.4, 7.2, 0.8, 0xFFCDA0CD);
                rect(x - 2.6, y - 2.6, 1.6, 1, l);
                rect(x - 0.6, y - 0.8, 1.2, 1.6, 0xFF3A1A4A);
            }
            case "frasco" -> {
                disco(x, y + 1.6, 4.6, alfa(c, 0.12 + 0.12 * pulso));
                disco(x, y + 1.6, 3.2, d);
                disco(x, y + 1.6, 2.8, c);
                rect(x - 1, y - 3.4, 2, 2.4, 0xFFC8B8D0);
                rect(x - 1.1, y - 4.2, 2.2, 1, 0xFFA0672E);
                rect(x - 1.8, y + 0.6, 1, 1, l);
                rect(x + 0.4, y + 2.2, 0.8, 0.8, l);
            }
            case "cabeza" -> {
                rect(x - 3.8, y - 2.4, 7.6, 5, d);
                rect(x - 3.3, y - 1.9, 6.6, 4, c);
                rect(x - 4.8, y + 0.2, 1.8, 2.6, c);
                rect(x + 3, y + 0.2, 1.8, 2.6, c);
                rect(x - 3.4, y - 4.8, 1, 2.8, 0xFFB8B8B8);
                rect(x + 2.4, y - 4.8, 1, 2.8, 0xFFB8B8B8);
                rect(x - 2, y - 0.8, 1.2, 1, 0xFFE070D0);
                rect(x + 0.8, y - 0.8, 1.2, 1, 0xFFE070D0);
                rect(x - 2.4, y + 1.8, 4.8, 0.7, 0xFFE8E8E8);
            }
            case "elitros" -> {
                for (int r = 0; r < 7; r++) {
                    double wl = r < 3 ? 1.4 + r * 1.1 : 4.6 - (r - 3) * 0.8;
                    rect(x - 0.4 - wl, y - 3.6 + r, wl, 1, c);
                    rect(x + 0.4, y - 3.6 + r, wl, 1, c);
                }
                linea(x - 0.6, y - 3, x - 4.2, y + 1, 0.5, d);
                linea(x + 0.6, y - 3, x + 4.2, y + 1, 0.5, d);
                linea(x - 0.6, y - 1.5, x - 3, y + 2, 0.5, d);
                linea(x + 0.6, y - 1.5, x + 3, y + 2, 0.5, d);
            }
            case "trofeo" -> {
                disco(x, y - 0.5, 5.4, alfa(c, 0.1 + 0.12 * pulso));
                rect(x - 3.2, y - 3.6, 6.4, 1.4, c);
                rect(x - 2.6, y - 2.2, 5.2, 1.2, c);
                rect(x - 1.6, y - 1, 3.2, 1.2, c);
                rect(x - 4.4, y - 3, 1, 2.4, d);
                rect(x + 3.4, y - 3, 1, 2.4, d);
                rect(x - 0.6, y + 0.2, 1.2, 2, c);
                rect(x - 2.4, y + 2.2, 4.8, 1.2, d);
                rect(x - 0.5, y - 2.8, 1, 1, 0xFFFFFFFF);
            }
            default -> {
                rect(x - 3, y - 3, 6, 6, d);
                rect(x - 2.5, y - 2.5, 5, 5, c);
            }
        }
    }

    private void peligro(Peligro h, double cam, double t) {
        double y = h.y - cam;
        switch (h.k) {
            case BUB -> {
                if (y < -10 || y > H + 10) return;
                boolean a = h.met && h.life > 2.4 && Math.sin(t * 30) > 0;
                int col = h.met ? (a ? 0xFFFFFFFF : 0xC0BEFF78) : 0xB0E1F5FF;
                double r = h.met && h.life > 3 ? 9 : h.r;
                disco(h.x, y, r, alfa(col, 0.28));
                for (int i = 0; i < 14; i++) {
                    double an = i / 14.0 * 6.283;
                    rect(h.x + Math.cos(an) * r - 0.4, y + Math.sin(an) * r - 0.4, 0.8, 0.8, col);
                }
                rect(h.x - r * 0.4, y - r * 0.4, 1, 1, 0xFFFFFFFF);
            }
            case DRIFT -> {
                double yy = h.yy - cam;
                if (yy < -24 || yy > H + 24) return;
                int col = 0xFF000000 | h.def.color;
                switch (h.def) {
                    case MEDUSA, MEDUSA_U -> {
                        disco(h.x, yy - 1, h.r, alfa(col, 0.85));
                        for (int i = -2; i <= 2; i++) rect(h.x + i * 1.6 - 0.3 + Math.sin(t * 3 + i + h.ph) * 0.6, yy - 0.5, 0.7, h.r + 1.5, alfa(col, 0.8));
                    }
                    case TEMPANO -> {
                        disco(h.x, yy, h.r, 0xFFEAF6FF);
                        rect(h.x - h.r * 0.5, yy - h.r * 0.5, h.r * 0.8, 1, 0xFFFFFFFF);
                        rect(h.x - 1, yy, 3, 1, 0xFFB8D8F0);
                    }
                    case TRONCO -> {
                        rect(h.x - h.r * 1.5, yy - h.r * 0.6, h.r * 3, h.r * 1.2, 0xFF7A5530);
                        rect(h.x - h.r * 1.5, yy - h.r * 0.6, 1.5, h.r * 1.2, 0xFFB08850);
                        rect(h.x - h.r, yy - 0.3, h.r * 2, 0.6, 0xFF5A3D20);
                    }
                    case MAGMA -> {
                        rect(h.x - h.r, yy - h.r, 2 * h.r, 2 * h.r, 0xFF5A1A10);
                        rect(h.x - h.r + 1, yy - h.r + 1, 2 * h.r - 2, 2 * h.r - 2, 0xFFB8381A);
                        rect(h.x - 2.5, yy - 1.5, 1.5, 1.5, 0xFFFFD060);
                        rect(h.x + 1, yy - 1.5, 1.5, 1.5, 0xFFFFD060);
                    }
                    case CHISPA -> {
                        disco(h.x, yy, h.r + 1.5, alfa(0xFFD060, 0.3));
                        disco(h.x, yy, h.r, Math.sin(t * 20 + h.ph) > 0 ? 0xFFFFFFFF : 0xFFFFD060);
                    }
                    case ALMA -> {
                        disco(h.x, yy, h.r + 2, alfa(0x5FE0F0, 0.25));
                        disco(h.x, yy + 0.5, h.r, 0xFF5FE0F0);
                        rect(h.x - 1, yy - h.r - 2 + Math.sin(t * 8 + h.ph), 2, 3, 0xFFB0F8FF);
                    }
                    case ISLA -> {
                        rect(h.x - h.r * 1.4, yy - h.r * 0.6, h.r * 2.8, h.r * 0.7, 0xFFD4D69A);
                        rect(h.x - h.r, yy, h.r * 2, h.r * 0.5, 0xFFA8AA78);
                        rect(h.x - h.r * 0.5, yy + h.r * 0.4, h.r, h.r * 0.5, 0xFF8A8C60);
                    }
                    default -> { // nube de aliento de dragón
                        for (int i = 0; i < 5; i++) disco(h.x + Math.cos(i * 1.3 + t * 0.7) * h.r * 0.35, yy + Math.sin(i * 2.1 + t * 0.5) * h.r * 0.3, h.r * 0.7, 0x38D060E0);
                        for (int i = 0; i < 16; i++) {
                            double an = i / 16.0 * 6.283;
                            rect(h.x + Math.cos(an) * h.r - 0.5, yy + Math.sin(an) * h.r - 0.5, 1, 1, 0xC0F0A0FF);
                        }
                    }
                }
            }
            case NET -> {
                double yy = h.yy - cam;
                if (yy < -10 || yy > H + 10) return;
                for (int i = -2; i <= 2; i++) {
                    rect(h.x + i * 2.5 - 0.3, yy - 6, 0.6, 12, 0xD0D8E0C0);
                    rect(h.x - 6, yy + i * 2.5 - 0.3, 12, 0.6, 0xD0D8E0C0);
                }
            }
            case SEEK -> {
                if (h.st == Partida.WAIT) {
                    if (!h.bul && y > -6 && y < H + 6) disco(h.x, y, h.r, 0xFF2E2224);
                } else if (h.st == Partida.WARN) { // aviso de la bala de shulker en el borde
                    if (Math.sin(t * 25) > -0.3) {
                        double ax = h.side > 0 ? W - 6 : 2, ay = p.hookY - cam + 30;
                        rect(ax, ay - 4, 4, 8, 0xFFF0A0FF);
                        rect(ax + 1.5, ay - 3, 1, 4, 0xFF3A1A4A);
                        rect(ax + 1.5, ay + 2, 1, 1, 0xFF3A1A4A);
                    }
                } else if (h.bul) {
                    disco(h.x, y, h.r + 1.5, 0x50F0A0FF);
                    rect(h.x - h.r, y - h.r, 2 * h.r, 2 * h.r, 0xFFF4E8FF);
                } else {
                    for (int i = 0; i < 4; i++) disco(h.x + Math.sin(t * 8 + i) * 0.8, y - i * 1.6, h.r * (1 - i * 0.15), 0xFF2E2224);
                    rect(h.x - 0.5, y + 1, 1, 1, 0xFFB03030);
                }
            }
            case CROSS -> {
                if (y < -12 || y > H + 12) return;
                int col = CRUZA[bioma.ordinal()];
                if (h.st == Partida.WARN) {
                    if (Math.sin(t * 25) > -0.3) {
                        double ax = h.dir > 0 ? 2 : W - 6;
                        rect(ax, y - 4, 4, 8, 0xFFFF5040);
                        rect(ax + 1.5, y - 3, 1, 4, 0xFFFFFFFF);
                        rect(ax + 1.5, y + 2, 1, 1, 0xFFFFFFFF);
                    }
                } else if (h.st == Partida.GO) {
                    if (h.ghast) {
                        disco(h.x - h.dir * 4, y, 3, 0x80FF9030);
                        disco(h.x, y, 3.2, 0xFF3A2A2A);
                        disco(h.x, y, 2, 0xFFFF9030);
                    } else {
                        rect(h.x - 7, y - 2, 14, 4, col);
                        rect(h.x - 4, y - 3, 8, 6, col);
                        rect(h.x - h.dir * 10, y - 3, 3, 6, col);
                        rect(h.x + h.dir * 7, y - 0.5, 4, 1, col);
                        rect(h.x + h.dir * 4, y - 1.5, 1, 1, 0xFF101010);
                    }
                }
            }
            case GEY -> {
                if (y > -40 && y < H + 40) {
                    int base = h.lava ? 0xFF5A1A10 : 0xFF3A4048;
                    rect(h.x - (h.side < 0 ? 0 : 3), y - 3, 3, 6, base);
                    if (h.st == Partida.WARN && Math.sin(t * 25) > -0.3) rect(h.x - (h.side < 0 ? -3 : 5), y - 1.5, 2, 3, h.lava || h.hot ? 0xFFFF9040 : 0xFFFFFFFF);
                }
                int jc = h.lava ? 0xFFFF8A30 : h.hot ? 0xE0FFD8B0 : 0xD0E6F6FF;
                for (double[] j : h.jets) disco(j[0], j[1] - cam, j[4], jc);
            }
            case COL -> {
                if (y < -50 || y > H + 50) return;
                if (h.st == Partida.WARN) {
                    if (Math.sin(t * 25) > -0.3) rect(h.x - 0.4, y - h.hh, 0.8, 2 * h.hh, h.chim ? 0xA0FF9040 : 0xA0FFF7A0);
                } else if (h.st == Partida.ON) {
                    rect(h.x - h.w, y - h.hh, 2 * h.w, 2 * h.hh, h.chim ? 0xC0FF7020 : 0xC0FFF7A0);
                    rect(h.x - h.w * 0.4, y - h.hh, h.w * 0.8, 2 * h.hh, 0xFFFFFFFF);
                }
                if (h.chim) rect(h.x - 4, y + h.hh - 2, 8, 4, 0xFF2A2020);
            }
            case FALL -> {
                if (h.st == Partida.WARN) {
                    if (Math.sin(t * 25) > -0.3) {
                        rect(h.x - 3, 1, 6, 1.5, 0xFFEAF6FF);
                        rect(h.x - 1.5, 2.5, 3, 2, 0xFFEAF6FF);
                    }
                } else if (h.st == Partida.GO) {
                    double yy = h.yy - cam;
                    rect(h.x - 2, yy - 6, 4, 3, 0xFFEAF6FF);
                    rect(h.x - 1.2, yy - 3, 2.4, 3, 0xFFD0ECFF);
                    rect(h.x - 0.5, yy, 1, 2.5, 0xFFFFFFFF);
                }
            }
            case LION -> {
                if (y > -30 && y < H + 30) {
                    for (int i = 0; i < 8; i++) {
                        double a = i / 8.0 * 6.283 + 0.39;
                        linea(h.x, y, h.x + Math.cos(a) * 5.5, y + Math.sin(a) * 5.5, 0.6, h.tm < 0.5 && Math.sin(t * 30) > 0 ? 0xFFFFFFFF : 0xFFF6EEC0);
                    }
                    disco(h.x, y, h.r, 0xFFD04A3A);
                    rect(h.x - h.r, y - 0.5, 2 * h.r, 1, 0xFFFFFFFF);
                }
                for (double[] q : h.jets) rect(q[0] - 0.75, q[1] - cam - 0.75, 1.5, 1.5, 0xFFF6EEC0);
            }
            case LUNGE -> {
                if (y < -10 || y > H + 10) return;
                rect(h.wx - 2, y - 3, 4, 6, 0xFF101418);
                if (h.st == Partida.WARN && Math.sin(t * 25) > -0.3) {
                    rect(h.wx + h.dir * 2 - 0.5, y - 1.5, 1, 1, 0xFFFFF096);
                    rect(h.wx + h.dir * 2 - 0.5, y + 0.5, 1, 1, 0xFFFFF096);
                }
                if (h.ext > 0.5) {
                    double a = Math.min(h.wx, h.x), b = Math.max(h.wx, h.x);
                    if (h.stay) {
                        rect(a, y - 1.6, b - a, 3.2, 0xFF4A3520);
                        rect(a, y - 0.4, b - a, 0.8, 0xFF6A4A2A);
                    } else {
                        rect(a, y - 1.8, b - a, 3.6, 0xFF3A4A3A);
                        rect(h.x - (h.dir > 0 ? 4 : 0), y - 2.6, 4, 5.2, 0xFF4A5E4A);
                        rect(h.x - (h.dir > 0 ? 1.5 : -0.5), y - 1.8, 1, 1, 0xFFFFE060);
                    }
                }
            }
        }
    }

    // ---------- Textos ----------

    private Component nombre(Objeto o) {
        if (o.pez() != null) {
            ItemStack stack = ModItems.pezStack(o.pez().id());
            return stack.isEmpty() ? Component.literal(o.pez().name()) : stack.getHoverName();
        }
        if (o.tesoro().chest()) return Component.translatable("minijuego.toodeepseas.cofre");
        ItemStack stack = ModItems.tesoroStack(o.tesoro().id());
        return stack.isEmpty() ? Component.literal(o.tesoro().name()) : stack.getHoverName();
    }

    private Component nombreZona(int z) {
        String b = bioma.name().toLowerCase(Locale.ROOT);
        if (bioma == Bioma.VACIO && z > 0) return Component.translatable("minijuego.toodeepseas.zona.vacio.n", z * 100);
        return Component.translatable("minijuego.toodeepseas.zona." + b + "." + z);
    }

    private void centrado(Component texto, int y, int color) {
        g.drawCenteredString(font, texto, W / 2, y, color);
    }

    private void hud() {
        double d = p.profundidad();
        g.drawString(font, String.format(Locale.ROOT, "%.1f m", d), 3, 3, 0xFFFFFF, true);
        String max = Double.isInfinite(p.aj.maxD()) ? "∞" : String.valueOf((int) Math.round(p.aj.maxD()));
        g.drawString(font, Component.translatable("minijuego.toodeepseas.max", max), 3, 13, 0xA9BCCD, true);
        g.drawString(font, Component.translatable("minijuego.toodeepseas.botin", p.botin.size()), 3, H - 11, 0xFFE9A8, true);
        // barra de profundidad
        int bx = W - 5, by0 = 18, by1 = H - 18;
        g.fill(bx - 1, by0, bx + 2, by1, 0x73000000);
        double f = Double.isInfinite(p.aj.maxD()) ? (d % 100) / 100 : Math.min(1, d / p.aj.maxD());
        g.fill(bx - 1, by0, bx + 2, by0 + (int) Math.round((by1 - by0) * f), 0xFF7FD0FF);
        if (p.estado == Estado.CAST || p.estado == Estado.PLAY) {
            if (cartelT > 0) centrado(Component.literal("— ").append(nombreZona(zonaVista)).append(" —"), 26, 0xE8F4FF);
            if (p.invT > 0) centrado(Component.translatable("minijuego.toodeepseas.invertido", String.format(Locale.ROOT, "%.1f", p.invT)), 50, 0xF0A0FF);
            else if (p.slowT > 0) centrado(Component.translatable("minijuego.toodeepseas.enredado"), 50, 0xD8E0C0);
            if (p.atFloor && Math.sin(p.t * 5) > -0.6) centrado(Component.translatable("minijuego.toodeepseas.fondo"), H - 24, 0xFFF0C8);
            if (p.runT < 5 && p.estado == Estado.PLAY) centrado(Component.translatable("minijuego.toodeepseas.ayuda"), H - 24, 0xCFE6FF);
        }
        if (p.estado == Estado.REEL) centrado(Component.translatable("minijuego.toodeepseas.recogiendo"), H / 2 - 20, 0xBFE8FF);
        // última captura
        double desde = (p.pasos - p.pasoCaptura) * Partida.DT;
        if (p.pasoCaptura >= 0 && desde < 1.4 && p.estado == Estado.PLAY) {
            Objeto o = p.objs.get(p.ultimaCaptura);
            centrado(Component.literal("+").append(nombre(o)), (int) (H / 2 - 20 - desde * 10), RAREZA[o.rarity()] & 0xFFFFFF);
        }
        if (p.estado == Estado.MUERTO && finT <= 1.2) centrado(Component.translatable("minijuego.toodeepseas.muerte." + p.causa.clave()), H / 2 - 30, 0xFF6A5A);
        if (terminada()) resultado();
    }

    private void resultado() {
        g.fill(8, 14, W - 8, H - 10, 0xD0101820);
        g.fill(8, 14, W - 8, 15, 0xFF4FB3FF);
        boolean murio = p.estado == Estado.MUERTO;
        centrado(Component.translatable(murio ? "minijuego.toodeepseas.has_muerto" : "minijuego.toodeepseas.has_salido"), 20, murio ? 0xFF7070 : 0x9FF09A);
        int y = 32;
        if (murio) {
            centrado(Component.translatable("minijuego.toodeepseas.muerte." + p.causa.clave()), y, 0xFFC0B0);
            y += 10;
        }
        centrado(Component.translatable("minijuego.toodeepseas.profundidad", String.format(Locale.ROOT, "%.1f", p.profFinal)), y, 0xCFE6FF);
        y += 12;
        Map<String, int[]> grupos = new LinkedHashMap<>();
        for (int id : p.botin) {
            Objeto o = p.objs.get(id);
            grupos.computeIfAbsent(nombre(o).getString(), k -> new int[]{0, o.rarity()})[0]++;
        }
        if (grupos.isEmpty()) centrado(Component.translatable("minijuego.toodeepseas.nada"), y, 0x93A4B5);
        int n = 0;
        for (Map.Entry<String, int[]> e : grupos.entrySet()) {
            if (n == 8 && grupos.size() > 9) {
                centrado(Component.translatable("minijuego.toodeepseas.y_mas", grupos.size() - n), y, 0x93A4B5);
                break;
            }
            String linea = (e.getValue()[0] > 1 ? e.getValue()[0] + "× " : "") + e.getKey();
            int color = murio ? 0x707070 : RAREZA[e.getValue()[1]] & 0xFFFFFF;
            g.drawString(font, linea, (W - font.width(linea)) / 2, y, color, true);
            if (murio) g.fill((W - font.width(linea)) / 2 - 1, y + 4, (W + font.width(linea)) / 2 + 1, y + 5, 0xFFB04040); // perdido
            y += 10;
            n++;
        }
        if (Math.sin(Util.getMillis() / 200.0) > -0.5) centrado(Component.translatable("minijuego.toodeepseas.continuar"), H - 22, 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long ahora = Util.getMillis();
        if (ultimo < 0) ultimo = ahora;
        acc += Math.min(0.1, (ahora - ultimo) / 1000.0);
        ultimo = ahora;
        while (acc >= Partida.DT) {
            acc -= Partida.DT;
            paso();
        }
        this.g = graphics;
        renderBackground(graphics);
        int left = (width - W) / 2, top = (height - H) / 2;
        // marco como el de un inventario
        graphics.fill(left - 8, top - 18, left + W + 8, top + H + 8, 0xFF000000);
        graphics.fill(left - 7, top - 17, left + W + 7, top + H + 7, 0xFFC6C6C6);
        graphics.fill(left - 7, top - 17, left + W + 6, top - 16, 0xFFFFFFFF);
        graphics.fill(left - 7, top - 17, left - 6, top + H + 6, 0xFFFFFFFF);
        graphics.fill(left - 6, top + H + 6, left + W + 7, top + H + 7, 0xFF555555);
        graphics.fill(left + W + 6, top - 16, left + W + 7, top + H + 7, 0xFF555555);
        graphics.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF373737);
        graphics.drawString(font, title, left - 2, top - 12, 0x404040, false);
        graphics.enableScissor(left, top, left + W, top + H);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0);
        pose.pushPose();
        pose.scale(1F / Q, 1F / Q, 1F);
        graphics.drawManaged(this::mundo); // un solo lote: miles de rectángulos
        pose.popPose();
        hud();
        pose.popPose();
        graphics.disableScissor();
    }
}
