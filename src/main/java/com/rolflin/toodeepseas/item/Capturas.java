package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.core.juego.Catalogo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/** Lo común a los objetos de las capturas (peces, tesoros y platos): comida, rareza y descripción. */
public final class Capturas {
    private Capturas() {
    }

    /** Rareza del catálogo (0 común … 4 legendario) como color del nombre. */
    public static Rarity rareza(int rarity) {
        return rarity >= 4 ? Rarity.EPIC : rarity == 3 ? Rarity.RARE : rarity == 2 ? Rarity.UNCOMMON : Rarity.COMMON;
    }

    @Nullable
    public static MobEffect efecto(String id) {
        return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
    }

    public static FoodProperties comida(int nutrition, float saturation, List<Catalogo.Efecto> effects, boolean always) {
        FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(nutrition).saturationMod(saturation);
        if (always) builder.alwaysEat();
        for (Catalogo.Efecto e : effects) {
            // El efecto se busca al comer: los del mod aún no están registrados cuando se crea el objeto
            builder.effect(() -> {
                MobEffect effect = efecto(e.id());
                return new MobEffectInstance(effect == null ? net.minecraft.world.effect.MobEffects.LUCK : effect, e.seconds() * 20, e.amplifier());
            }, e.chance());
        }
        return builder.build();
    }

    public static String clave(String id) {
        return "captura.toodeepseas." + id;
    }

    /** Las líneas de «para qué sirve», que escribe el generador en los archivos de idioma. */
    public static void usos(List<Component> tooltip, String id, int lines) {
        for (int i = 1; i <= lines; i++) {
            tooltip.add(Component.literal("• ").append(Component.translatable(clave(id) + ".uso." + i)).withStyle(ChatFormatting.GRAY));
        }
    }
}
