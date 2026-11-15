package com.rolflin.toodeepseas.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Cebo como objeto. El id del objeto es el de su definición en el datapack. Se apila hasta 64. */
public class BaitItem extends Item {
    private final String id;

    public BaitItem(String id, Properties properties) {
        super(properties);
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String getDescriptionId() {
        return "bait.toodeepseas." + id;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.toodeepseas.cebo").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.toodeepseas.como_equipar").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
