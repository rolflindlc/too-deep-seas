package com.rolflin.toodeepseas.core.juego;

/**
 * Azar determinista del minijuego, idéntico bit a bit al del prototipo web ({@code mulberry32}, {@code hash2},
 * {@code vnoise}): con la misma semilla, cliente y servidor generan exactamente la misma partida.
 * Solo usa aritmética de enteros de 32 bits, así que no depende de la plataforma.
 */
public final class Azar {
    private static final double DOS_32 = 4294967296.0;
    private int a;

    public Azar(int semilla) {
        this.a = semilla;
    }

    /** Siguiente número en [0, 1). */
    public double next() {
        a += 0x6D2B79F5;
        int t = (a ^ (a >>> 15)) * (1 | a);
        t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
        return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / DOS_32;
    }

    /** Mezcla dos enteros en uno (sin signo en el prototipo; aquí, los mismos 32 bits). */
    public static int hash2(int a, int b) {
        int h = ((a ^ 0x9e3779b9) * 0x85ebca6b) ^ ((b + 0x7f4a7c15) * 0xc2b2ae35);
        h ^= h >>> 13;
        h *= 0x27d4eb2d;
        h ^= h >>> 16;
        return h;
    }

    private static double unit(int h) {
        return (h & 0xFFFFFFFFL) / DOS_32;
    }

    /** Ruido suave de una dimensión en [0, 1). */
    public static double vnoise(int s, double x) {
        double fl = Math.floor(x);
        int i = (int) (long) fl;
        double f = x - fl, a = unit(hash2(s, i)), b = unit(hash2(s, i + 1)), u = f * f * (3 - 2 * f);
        return a + (b - a) * u;
    }

    /** Semilla a partir de un texto: los números se usan tal cual y el resto se resume (FNV-1a), como en el prototipo. */
    public static int semillaDe(String texto) {
        if (!texto.isEmpty() && texto.chars().allMatch(c -> c >= '0' && c <= '9') && texto.length() <= 18) {
            return (int) Long.parseLong(texto);
        }
        int h = (int) 2166136261L;
        for (int i = 0; i < texto.length(); i++) {
            h ^= texto.charAt(i);
            h *= 16777619;
        }
        return h;
    }
}
