package com.rolflin.toodeepseas.effect;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.juego.Catalogo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Pociones nuevas (con sus versiones larga y fuerte) y las mezclas del soporte: poción rara + pez o tesoro.
 * Los ingredientes de cada poción están en el catálogo ({@link Catalogo#MEZCLAS}); también dan pociones vanilla.
 */
public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, TooDeepSeas.MODID);

    /** Poción base → [base, larga o null, fuerte o null]. */
    public static final Map<String, Familia> FAMILIAS = new LinkedHashMap<>();

    public record Familia(RegistryObject<Potion> base, RegistryObject<Potion> larga, RegistryObject<Potion> fuerte) {
    }

    static {
        //        id               efecto                              base  larga  fuerte (0 = no hay)  nivel base
        familia("prisa", () -> MobEffects.DIG_SPEED, 3600, 9600, 1800, 0);
        familia("gracia_delfin", () -> MobEffects.DOLPHINS_GRACE, 1800, 4800, 0, 0);
        familia("conducto", () -> MobEffects.CONDUIT_POWER, 3600, 9600, 0, 0);
        familia("resistencia", () -> MobEffects.DAMAGE_RESISTANCE, 3600, 9600, 1800, 0);
        familia("absorcion", () -> MobEffects.ABSORPTION, 2400, 4800, 1200, 1);
        familia("brillo", () -> MobEffects.GLOWING, 3600, 9600, 0, 0);
        familia("levitacion", () -> MobEffects.LEVITATION, 400, 800, 0, 0);
        familia("descomposicion", () -> MobEffects.WITHER, 400, 800, 200, 0);
        familia("aletas", ModEffects.ALETAS, 3600, 9600, 1800, 0);
        familia("coraza", ModEffects.CORAZA, 3600, 9600, 1800, 0);
    }

    private ModPotions() {
    }

    private static void familia(String id, Supplier<MobEffect> effect, int base, int larga, int fuerte, int nivel) {
        // Las tres comparten nombre («Poción de prisa»), como las de Minecraft
        RegistryObject<Potion> b = POTIONS.register(id, () -> new Potion(id, new MobEffectInstance(effect.get(), base, nivel)));
        RegistryObject<Potion> l = larga == 0 ? null : POTIONS.register("long_" + id, () -> new Potion(id, new MobEffectInstance(effect.get(), larga, nivel)));
        RegistryObject<Potion> f = fuerte == 0 ? null : POTIONS.register("strong_" + id, () -> new Potion(id, new MobEffectInstance(effect.get(), fuerte, nivel * 2 + 1)));
        FAMILIAS.put(id, new Familia(b, l, f));
    }

    /** Mezcla del soporte: una poción (normal, arrojadiza o persistente) de un tipo + un ingrediente = la misma botella con otra poción. */
    public record Mezcla(Potion from, Ingredient with, Potion to) implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            Item item = input.getItem();
            return (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) && PotionUtils.getPotion(input) == from;
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return with.test(ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) return ItemStack.EMPTY;
            return PotionUtils.setPotion(new ItemStack(input.getItem()), to);
        }
    }

    /** Las mezclas del catálogo y las de alargar (redstone) y reforzar (piedra luminosa). Se llama una vez, al arrancar. */
    public static List<Mezcla> mezclas() {
        List<Mezcla> out = new ArrayList<>();
        for (Catalogo.Mezcla m : Catalogo.MEZCLAS) {
            Potion to = ForgeRegistries.POTIONS.getValue(new ResourceLocation(m.to()));
            if (to == null || to == Potions.EMPTY) throw new IllegalStateException("poción desconocida: " + m.to());
            List<ItemStack> items = new ArrayList<>();
            for (String id : m.with()) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                if (item == null || item == Items.AIR) throw new IllegalStateException("ingrediente desconocido: " + id);
                items.add(new ItemStack(item));
            }
            out.add(new Mezcla(Potions.AWKWARD, Ingredient.of(items.stream()), to));
        }
        for (Familia f : FAMILIAS.values()) {
            if (f.larga() != null) out.add(new Mezcla(f.base().get(), Ingredient.of(Items.REDSTONE), f.larga().get()));
            if (f.fuerte() != null) out.add(new Mezcla(f.base().get(), Ingredient.of(Items.GLOWSTONE_DUST), f.fuerte().get()));
        }
        return out;
    }

    public static void registrarMezclas() {
        for (Mezcla m : mezclas()) BrewingRecipeRegistry.addRecipe(m);
    }
}
