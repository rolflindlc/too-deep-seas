package com.rolflin.toodeepseas.client;

import net.minecraft.client.Minecraft;

import java.util.Locale;

/** Números para la interfaz: sin ceros de más y con coma decimal en español. */
public final class Fmt {
    private Fmt() {
    }

    public static String num(double value, int decimals) {
        String s = String.format(Locale.ROOT, "%." + decimals + "f", value);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        if (s.equals("-0")) s = "0";
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("es") ? s.replace('.', ',') : s;
    }

    /** Fracción como porcentaje con signo: 0,2 → «+20 %». */
    public static String pct(double fraction) {
        long p = Math.round(fraction * 100);
        return (p > 0 ? "+" : p < 0 ? "-" : "") + Math.abs(p) + " %";
    }
}
