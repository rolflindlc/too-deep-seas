package com.rolflin.toodeepseas.client;

import com.rolflin.toodeepseas.core.juego.Partida;
import net.minecraft.client.Minecraft;

/** Lo que el cliente hace con los paquetes del minijuego. Solo se carga en el cliente. */
public final class MinijuegoCliente {
    private MinijuegoCliente() {
    }

    public static void abrir(Partida.Ajustes ajustes) {
        Minecraft.getInstance().setScreen(new MinijuegoScreen(ajustes));
    }

    public static void recoger() {
        if (Minecraft.getInstance().screen instanceof MinijuegoScreen screen) screen.recoger();
    }
}
