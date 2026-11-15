package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.core.AccessoryDef;
import com.rolflin.toodeepseas.data.Defs;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Accesorio de caña como objeto. El id del objeto es el de su definición en el datapack;
 * el nivel y los usos que le quedan van en el NBT ({@code Lvl}, {@code Uses}; sin {@code Uses} = entero).
 */
public class AccessoryItem extends Item {
    public static final String LVL = "Lvl";
    public static final String USES = "Uses";

    private final String id;
    private final int maxLevel;

    public AccessoryItem(String id, int maxLevel, Properties properties) {
        super(properties.stacksTo(1));
        this.id = id;
        this.maxLevel = maxLevel;
    }

    public String id() {
        return id;
    }

    /** Niveles que tiene este accesorio en el datapack incluido en el mod (para el modo creativo). */
    public int maxLevel() {
        return maxLevel;
    }

    public static int level(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(LVL) ? 1 : Mth.clamp(tag.getInt(LVL), 1, 3);
    }

    public int maxUses(ItemStack stack) {
        AccessoryDef def = Defs.ACCESSORIES.get(id);
        return def == null ? 0 : def.usesAt(level(stack));
    }

    public int uses(ItemStack stack) {
        int max = maxUses(stack);
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(USES) ? Mth.clamp(tag.getInt(USES), 0, max) : max;
    }

    /** @param uses usos que le quedan; negativo = todos */
    public ItemStack create(int level, int uses) {
        ItemStack stack = new ItemStack(this);
        if (level > 1) stack.getOrCreateTag().putInt(LVL, Math.min(3, level));
        if (uses >= 0 && uses < maxUses(stack)) stack.getOrCreateTag().putInt(USES, uses);
        return stack;
    }

    @Override
    public String getDescriptionId() {
        return "accessory.toodeepseas." + id;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent name = Component.translatable(getDescriptionId());
        return maxLevel > 1 || level(stack) > 1 ? name.append(" " + "I".repeat(level(stack))) : name;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return uses(stack) < maxUses(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = maxUses(stack);
        return max <= 0 ? 0 : Math.round(13.0F * uses(stack) / max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int max = maxUses(stack);
        float f = max <= 0 ? 0 : (float) uses(stack) / max;
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        AccessoryDef def = Defs.ACCESSORIES.get(id);
        if (def != null) {
            tooltip.add(Component.translatable("tooltip.toodeepseas.accesorio",
                    Component.translatable("category.toodeepseas." + def.category().name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (def != null) tooltip.add(usesLine(uses(stack), maxUses(stack)));
        tooltip.add(Component.translatable("tooltip.toodeepseas.como_equipar").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** «Usos: 35/40»: verde, naranja por debajo del 10 % y «Gastado» en rojo al llegar a 0. */
    public static Component usesLine(int uses, int max) {
        if (uses <= 0) return Component.translatable("tooltip.toodeepseas.gastado").withStyle(ChatFormatting.RED);
        return Component.translatable("tooltip.toodeepseas.usos", uses, max).withStyle(usesColor(uses, max));
    }

    public static ChatFormatting usesColor(int uses, int max) {
        if (uses <= 0) return ChatFormatting.DARK_GRAY;
        return uses * 10 < max ? ChatFormatting.GOLD : ChatFormatting.GREEN;
    }
}
