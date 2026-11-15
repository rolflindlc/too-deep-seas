package com.rolflin.pesca.item;

import com.rolflin.pesca.core.RodTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/** Caña con tier. Hereda de la caña vanilla, así que se lanza y se encanta igual. */
public class TieredRodItem extends FishingRodItem {
    private final RodTier tier;

    public TieredRodItem(RodTier tier, Properties properties) {
        super(properties.durability(tier.durability));
        this.tier = tier;
    }

    public RodTier getTier() {
        return tier;
    }

    @Override
    public int getEnchantmentValue() {
        return tier.enchantability;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String t = tier.tier == Math.floor(tier.tier) ? String.valueOf((int) tier.tier) : String.valueOf(tier.tier).replace('.', ',');
        tooltip.add(Component.translatable("tooltip.pesca.tier", t).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.pesca.ranuras", tier.accessorySlots, "I".repeat(tier.maxAccessoryLevel())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(tier.canFishVoid() ? "tooltip.pesca.profundidad_vacio" : "tooltip.pesca.profundidad", tier.lineDepth).withStyle(ChatFormatting.GRAY));
        if (tier.baitSlots > 1) {
            tooltip.add(Component.translatable("tooltip.pesca.cebos", tier.baitSlots).withStyle(ChatFormatting.GRAY));
        }
        switch (tier) {
            case ORO, PRISMARINA, NETHERITA -> tooltip.add(Component.translatable("tooltip.pesca.rasgo." + tier.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.AQUA));
            case VACIO -> tooltip.add(Component.translatable("tooltip.pesca.rasgo.vacio").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            default -> {
            }
        }
    }
}
