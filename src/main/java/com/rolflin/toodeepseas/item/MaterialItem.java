package com.rolflin.toodeepseas.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Material de fabricación propio del mod, con una línea que dice para qué sirve o de dónde sale. */
public class MaterialItem extends Item {
    private final String id;

    public MaterialItem(String id, Properties properties) {
        super(properties);
        this.id = id;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.toodeepseas.material." + id).withStyle(ChatFormatting.GRAY));
    }
}
