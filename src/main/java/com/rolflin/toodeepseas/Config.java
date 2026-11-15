package com.rolflin.toodeepseas;

import net.minecraftforge.common.ForgeConfigSpec;

/** Opciones del mod ({@code config/toodeepseas-common.toml}). Se leen al arrancar: cambiarlas pide reiniciar el juego. */
public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue BARRIL_LEGADO;
    public static final ForgeConfigSpec.BooleanValue MINIJUEGO;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("aldeas");
        BARRIL_LEGADO = builder
                .comment("La mesa del pescador es el puesto de trabajo del aldeano pescador.",
                        "false (por defecto): el barril deja de serlo y las casas de pescador nuevas se generan con la mesa.",
                        "true: el barril sigue valiendo además de la mesa y las aldeas se generan como siempre.",
                        "Hay que reiniciar el juego para que el cambio tenga efecto.")
                .define("barrilLegado", false);
        builder.pop();
        builder.push("minijuego");
        MINIJUEGO = builder
                .comment("true (por defecto): al lanzar la caña se juega el minijuego de pesca.",
                        "false: se pesca como en Minecraft (los peces del mod siguen saliendo).")
                .define("activo", true);
        builder.pop();
        SPEC = builder.build();
    }

    private Config() {
    }
}
