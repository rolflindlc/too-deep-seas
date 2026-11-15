package com.rolflin.toodeepseas.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nullable;

/** Receta con forma que copia el NBT de la caña anterior: conserva accesorios (con sus usos), cebo y encantamientos. */
public class RodUpgradeRecipe extends ShapedRecipe {
    public RodUpgradeRecipe(ShapedRecipe r) {
        super(r.getId(), r.getGroup(), r.category(), r.getWidth(), r.getHeight(), r.getIngredients(), r.getResultItem(RegistryAccess.EMPTY), r.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        ItemStack out = super.assemble(inv, access);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack in = inv.getItem(i);
            if (in.getItem() instanceof FishingRodItem && in.hasTag()) {
                out.setTag(in.getTag().copy());
                out.setDamageValue(0); // la caña nueva sale entera
                break;
            }
        }
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ROD_UPGRADE.get();
    }

    public static class Serializer implements RecipeSerializer<RodUpgradeRecipe> {
        @Override
        public RodUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new RodUpgradeRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Nullable
        @Override
        public RodUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ShapedRecipe r = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            return r == null ? null : new RodUpgradeRecipe(r);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RodUpgradeRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
