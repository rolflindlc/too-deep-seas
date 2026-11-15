package com.rolflin.toodeepseas.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.rolflin.toodeepseas.item.AccessoryItem;
import com.rolflin.toodeepseas.item.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Receta del taller de la mesa del pescador: un objeto base más 1 o 2 materiales.
 * Sirve para subir de nivel un accesorio (se mira el id y el nivel, nunca los usos: gastado también vale,
 * y el resultado sale con todos sus usos) y para mejorar una caña conservando su equipo.
 *
 * <p>El inventario que recibe tiene la base en la casilla 0 y los materiales en la 1 y la 2, en cualquier orden.</p>
 */
public class TallerRecipe implements Recipe<Container> {
    public record Material(Ingredient ingredient, int count) {
    }

    private final ResourceLocation id;
    private final Ingredient base;
    /** Nivel que debe tener el accesorio base; 0 = da igual (cañas). */
    private final int baseLevel;
    private final List<Material> materials;
    private final ItemStack result;
    /** Nivel del accesorio resultante; 0 = sin nivel. */
    private final int resultLevel;

    public TallerRecipe(ResourceLocation id, Ingredient base, int baseLevel, List<Material> materials, ItemStack result, int resultLevel) {
        this.id = id;
        this.base = base;
        this.baseLevel = baseLevel;
        this.materials = List.copyOf(materials);
        this.result = result;
        this.resultLevel = resultLevel;
    }

    public List<Material> materials() {
        return materials;
    }

    public int resultLevel() {
        return resultLevel;
    }

    public ItemStack result() {
        return result;
    }

    /** ¿Es este el objeto base de la receta (con su nivel, si lo pide)? */
    public boolean matchesBase(ItemStack stack) {
        if (stack.isEmpty() || !base.test(stack)) return false;
        return baseLevel <= 0 || (stack.getItem() instanceof AccessoryItem && AccessoryItem.level(stack) == baseLevel);
    }

    /**
     * Reparte los materiales de la receta entre las casillas 1 y 2.
     *
     * @return casilla de cada material, o null si no encajan (falta alguno, no llega la cantidad o sobra algo)
     */
    @Nullable
    private int[] assign(Container inv) {
        if (inv.getContainerSize() < 3 || !matchesBase(inv.getItem(0))) return null;
        if (materials.size() == 1) {
            for (int slot = 1; slot <= 2; slot++) {
                if (fits(materials.get(0), inv.getItem(slot)) && inv.getItem(3 - slot).isEmpty()) return new int[]{slot};
            }
            return null;
        }
        for (int slot = 1; slot <= 2; slot++) {
            if (fits(materials.get(0), inv.getItem(slot)) && fits(materials.get(1), inv.getItem(3 - slot))) return new int[]{slot, 3 - slot};
        }
        return null;
    }

    private static boolean fits(Material material, ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() >= material.count() && material.ingredient().test(stack);
    }

    @Override
    public boolean matches(Container inv, Level level) {
        return assign(inv) != null;
    }

    @Override
    public ItemStack assemble(Container inv, RegistryAccess access) {
        ItemStack in = inv.getItem(0);
        ItemStack out = result.copy();
        if (out.getItem() instanceof AccessoryItem acc) {
            out = acc.create(Math.max(1, resultLevel), -1);
        } else if (ModItems.tierOf(in) != null && in.hasTag()) {
            // Caña: conserva accesorios (con sus usos), cebo y encantamientos, y sale entera
            out.setTag(in.getTag().copy());
            out.setDamageValue(0);
        }
        return out;
    }

    /** Gasta la base y los materiales. Solo se llama cuando {@link #matches} es cierto. */
    public void consume(Container inv) {
        int[] slots = assign(inv);
        if (slots == null) return;
        for (int i = 0; i < slots.length; i++) inv.removeItem(slots[i], materials.get(i).count());
        inv.removeItem(0, 1);
    }

    /** «2× Lingote de hierro + 2× Cuerda». */
    public Component materialsText() {
        MutableComponent text = Component.empty();
        for (int i = 0; i < materials.size(); i++) {
            Material m = materials.get(i);
            ItemStack[] options = m.ingredient().getItems();
            if (i > 0) text.append(" + ");
            text.append(m.count() + "× ");
            text.append(options.length == 0 ? Component.literal("?") : options[0].getHoverName());
        }
        return text;
    }

    /** Recetas de taller cuyo objeto base es este (cliente y servidor). */
    public static List<TallerRecipe> forBase(@Nullable Level level, ItemStack base) {
        List<TallerRecipe> out = new ArrayList<>();
        if (level == null || base.isEmpty()) return out;
        for (TallerRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.TALLER_TYPE.get())) {
            if (recipe.matchesBase(base)) out.add(recipe);
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    /** No aparece en el libro de recetas (no es una receta de mesa de trabajo). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TALLER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.TALLER_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<TallerRecipe> {
        @Override
        public TallerRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient base = Ingredient.fromJson(json.get("base"));
            List<Material> materials = new ArrayList<>();
            JsonArray array = GsonHelper.getAsJsonArray(json, "materials");
            for (JsonElement el : array) {
                JsonObject o = GsonHelper.convertToJsonObject(el, "material");
                materials.add(new Material(Ingredient.fromJson(o.get("ingredient")), GsonHelper.getAsInt(o, "count", 1)));
            }
            if (materials.isEmpty() || materials.size() > 2) throw new JsonParseException("materials debe tener 1 o 2 entradas");
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new TallerRecipe(id, base, GsonHelper.getAsInt(json, "base_level", 0), materials, result, GsonHelper.getAsInt(json, "result_level", 0));
        }

        @Nullable
        @Override
        public TallerRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient base = Ingredient.fromNetwork(buf);
            int baseLevel = buf.readVarInt();
            int n = buf.readVarInt();
            List<Material> materials = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Ingredient ingredient = Ingredient.fromNetwork(buf);
                materials.add(new Material(ingredient, buf.readVarInt()));
            }
            ItemStack result = buf.readItem();
            return new TallerRecipe(id, base, baseLevel, materials, result, buf.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TallerRecipe recipe) {
            recipe.base.toNetwork(buf);
            buf.writeVarInt(recipe.baseLevel);
            buf.writeVarInt(recipe.materials.size());
            for (Material m : recipe.materials) {
                m.ingredient().toNetwork(buf);
                buf.writeVarInt(m.count());
            }
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.resultLevel);
        }
    }
}
