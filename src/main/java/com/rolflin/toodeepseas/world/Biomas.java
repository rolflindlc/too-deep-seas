package com.rolflin.toodeepseas.world;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import com.rolflin.toodeepseas.core.juego.Perfil.SubBioma;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Traduce el sitio donde ha caído el anzuelo a los biomas y climas de pesca del mod.
 * Qué bioma de Minecraft cuenta como cada uno se decide con etiquetas de bioma
 * ({@code data/toodeepseas/tags/worldgen/biome/pesca/*.json}), así que otros mods o datapacks pueden añadir los suyos.
 */
public final class Biomas {
    public static final TagKey<Biome> PANTANO = tag("pantano");
    public static final TagKey<Biome> HELADO = tag("helado");
    public static final TagKey<Biome> CALIDO = tag("calido");
    public static final TagKey<Biome> OCEANO = tag("oceano");

    private Biomas() {
    }

    private static TagKey<Biome> tag(String name) {
        return TagKey.create(Registries.BIOME, new ResourceLocation(TooDeepSeas.MODID, "pesca/" + name));
    }

    /** Nether = lava, End = Vacío; en el resto manda la etiqueta del bioma y, si no tiene ninguna, es agua dulce (río). */
    public static Bioma bioma(Level level, BlockPos pos) {
        if (level.dimension() == Level.NETHER) return Bioma.LAVA;
        if (level.dimension() == Level.END) return Bioma.VACIO;
        return bioma(level.getBiome(pos));
    }

    public static Bioma bioma(Holder<Biome> biome) {
        if (biome.is(PANTANO)) return Bioma.PANTANO;
        if (biome.is(HELADO)) return Bioma.HELADO;
        if (biome.is(CALIDO)) return Bioma.CALIDO;
        if (biome.is(OCEANO)) return Bioma.OCEANO;
        return Bioma.RIO;
    }

    /** Sub-bioma del Nether en ese punto (en la lava hace el papel del clima); fuera del Nether, {@code null}. */
    @javax.annotation.Nullable
    public static SubBioma subBioma(Level level, BlockPos pos) {
        return level.dimension() == Level.NETHER ? subBioma(level.getBiome(pos)) : null;
    }

    /** Lo que no sea ninguno de los otros cuatro biomas del Nether (también los de otros mods) cuenta como yermos. */
    public static SubBioma subBioma(Holder<Biome> biome) {
        if (biome.is(Biomes.BASALT_DELTAS)) return SubBioma.DELTAS;
        if (biome.is(Biomes.SOUL_SAND_VALLEY)) return SubBioma.ALMAS;
        if (biome.is(Biomes.CRIMSON_FOREST)) return SubBioma.CARMESI;
        if (biome.is(Biomes.WARPED_FOREST)) return SubBioma.DISTORSIONADO;
        return SubBioma.YERMOS;
    }

    /** Clima en ese punto: en los biomas donde nunca llueve (desierto, sabana) siempre está despejado. */
    public static Clima clima(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight() || !level.isRaining()) return Clima.DESPEJADO;
        if (level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.NONE) return Clima.DESPEJADO;
        return level.isThundering() ? Clima.TORMENTA : Clima.LLUVIA;
    }
}
