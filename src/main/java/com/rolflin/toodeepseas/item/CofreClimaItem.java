package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.core.Cofres.Banda;
import com.rolflin.toodeepseas.core.Cofres.Calidad;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Cofre exclusivo de clima: el cofre de lluvia solo sale mientras llueve y el de tormenta solo durante una tormenta,
 * en cualquier bioma de agua. Tiene su propio botín ({@link Cofres#tiradasClima}), que no depende del bioma.
 * Como los demás cofres, guarda la calidad y la banda de profundidad en el NBT.
 */
public class CofreClimaItem extends CofreItem {
    private final Clima tipo;

    public CofreClimaItem(Clima tipo, Properties properties) {
        super(null, properties);
        this.tipo = tipo;
    }

    public Clima tipo() {
        return tipo;
    }

    @Override
    public Clima clima(ItemStack stack) {
        return tipo;
    }

    public ItemStack create(Calidad calidad, Banda banda) {
        ItemStack stack = new ItemStack(this);
        if (calidad.ordinal() > 0) stack.getOrCreateTag().putInt(CALIDAD, calidad.ordinal());
        if (banda.ordinal() > 0) stack.getOrCreateTag().putInt(BANDA, banda.ordinal());
        return stack;
    }

    @Override
    public ItemStack create(Calidad calidad, Banda banda, Clima clima) {
        return create(calidad, banda);
    }

    @Override
    protected List<Cofres.Tirada> tiradas(ItemStack stack) {
        return Cofres.tiradasClima(tipo, calidad(stack), banda(stack));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.toodeepseas.cofre.nombre",
                Component.translatable("cofre.toodeepseas.calidad." + calidad(stack).id()),
                Component.translatable("cofre.toodeepseas.exclusivo." + tipo.id()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.toodeepseas.cofre.profundidad",
                Component.translatable("cofre.toodeepseas.banda." + banda(stack).id())).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.toodeepseas.cofre.exclusivo." + tipo.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.toodeepseas.cofre.abrir").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
