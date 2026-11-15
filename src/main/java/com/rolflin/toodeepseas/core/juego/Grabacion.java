package com.rolflin.toodeepseas.core.juego;

import java.io.ByteArrayOutputStream;

/**
 * Las teclas de una partida, paso a paso, comprimidas en tramos (teclas, cuántos pasos seguidos). El cliente la envía
 * al terminar y el servidor repite la partida con ella para saber qué se ha pescado de verdad.
 */
public final class Grabacion {
    /** Tope de pasos de una partida: 30 minutos. */
    public static final int MAX_PASOS = 60 * 60 * 30;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private int actual = -1, cuenta, pasos;

    public void add(int teclas) {
        teclas &= 15;
        if (teclas == actual && cuenta < 0xFFFF) cuenta++;
        else {
            flush();
            actual = teclas;
            cuenta = 1;
        }
        pasos++;
    }

    private void flush() {
        if (cuenta == 0) return;
        out.write(actual);
        out.write(cuenta >> 8);
        out.write(cuenta & 0xFF);
        cuenta = 0;
    }

    public int pasos() {
        return pasos;
    }

    public byte[] bytes() {
        flush();
        actual = -1;
        return out.toByteArray();
    }

    /** Repite la partida con esas teclas hasta que termina, se recoge o se acaban las teclas (cuenta como recoger). */
    public static void repetir(Partida p, byte[] datos) {
        int total = 0;
        for (int i = 0; i + 2 < datos.length && total < MAX_PASOS; i += 3) {
            int teclas = datos[i] & 15, n = ((datos[i + 1] & 0xFF) << 8) | (datos[i + 2] & 0xFF);
            for (int k = 0; k < n && total < MAX_PASOS; k++, total++) {
                if (p.estado != Partida.Estado.CAST && p.estado != Partida.Estado.PLAY) return;
                p.paso(teclas);
            }
        }
        // sin la orden de recoger (desconexión, pantalla cerrada a la fuerza): se recoge en el punto en que estaba
        for (int k = 0; k < 600 && p.estado == Partida.Estado.CAST; k++) p.paso(0);
        if (p.estado == Partida.Estado.PLAY) p.paso(Partida.RECOGER);
    }
}
