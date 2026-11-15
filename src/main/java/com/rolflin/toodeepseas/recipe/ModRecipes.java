package com.rolflin.toodeepseas.recipe;

import com.rolflin.toodeepseas.TooDeepSeas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TooDeepSeas.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, TooDeepSeas.MODID);

    public static final RegistryObject<RecipeSerializer<RodUpgradeRecipe>> ROD_UPGRADE = SERIALIZERS.register("rod_upgrade", RodUpgradeRecipe.Serializer::new);
    /** Taller de la mesa del pescador: mejora de nivel de accesorios y de cañas. */
    public static final RegistryObject<RecipeSerializer<TallerRecipe>> TALLER = SERIALIZERS.register("taller", TallerRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<TallerRecipe>> TALLER_TYPE = TYPES.register("taller",
            () -> RecipeType.simple(new ResourceLocation(TooDeepSeas.MODID, "taller")));

    private ModRecipes() {
    }
}
