package com.rolflin.toodeepseas.recipe;

import com.rolflin.toodeepseas.TooDeepSeas;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TooDeepSeas.MODID);
    public static final RegistryObject<RecipeSerializer<RodUpgradeRecipe>> ROD_UPGRADE = SERIALIZERS.register("rod_upgrade", RodUpgradeRecipe.Serializer::new);

    private ModRecipes() {
    }
}
