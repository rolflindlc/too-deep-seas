package com.rolflin.toodeepseas;

import net.minecraftforge.common.ForgeConfigSpec;

/** Opciones del mod ({@code config/toodeepseas-common.toml}). Se leen al arrancar: cambiarlas pide reiniciar el juego. */
public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue BARRIL_LEGADO;

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
        SPEC = builder.build();
    }

    private Config() {
    }
}
