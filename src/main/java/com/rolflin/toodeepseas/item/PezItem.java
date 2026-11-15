package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.core.juego.Catalogo;
import com.rolflin.toodeepseas.core.juego.PezDef;
import com.rolflin.toodeepseas.data.Defs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Un pez del minijuego como objeto: se come crudo, se cocina y, según el pez, sirve para pociones, platos o materiales. */
public class PezItem extends Item {
    private final Catalogo.Pez def;

    public PezItem(Catalogo.Pez def) {
        super(properties(def));
        this.def = def;
    }

    private static Properties properties(Catalogo.Pez def) {
        Properties p = new Properties().rarity(Capturas.rareza(def.rarity()))
                .food(Capturas.comida(def.nutrition(), def.saturation(), def.effects(), def.always()));
        return def.family().equals("igneo") ? p.fireResistant() : p;
    }

    public Catalogo.Pez def() {
        return def;
    }

    @Override
    public String getDescriptionId() {
        return Capturas.clave(def.id());
    }

    /** Los peces de lava arden en el horno. */
    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return def.fuel() > 0 ? def.fuel() : -1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.toodeepseas.captura.rareza." + def.rarity()).append(" · ")
                .append(Component.translatable("tooltip.toodeepseas.captura.familia." + def.family())).withStyle(ChatFormatting.BLUE));
        PezDef pez = Defs.PECES.get(def.id());
        if (pez != null) {
            MutableComponent donde = Component.empty();
            boolean first = true;
            for (Cofres.Bioma b : pez.biomes()) {
                if (!first) donde.append(", ");
                donde.append(Component.translatable("tooltip.toodeepseas.captura.bioma." + b.id()));
                first = false;
            }
            tooltip.add(Component.translatable("tooltip.toodeepseas.captura.donde", donde, pez.minDepth()).withStyle(ChatFormatting.DARK_GRAY));
        }
        Capturas.usos(tooltip, def.id(), def.lines());
        tooltip.add(Component.literal("• ").append(Component.translatable("tooltip.toodeepseas.captura.vende")).withStyle(ChatFormatting.GRAY));
    }
}
