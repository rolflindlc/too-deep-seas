package com.rolflin.toodeepseas.client;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.AccessoryDef;
import com.rolflin.toodeepseas.core.FishingMedium;
import com.rolflin.toodeepseas.core.RodStats;
import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.data.Defs;
import com.rolflin.toodeepseas.data.RodData;
import com.rolflin.toodeepseas.item.AccessoryItem;
import com.rolflin.toodeepseas.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Descripción de cualquier caña (también la vanilla): tier, ranuras, equipo y, con Mayús, estadísticas finales. */
@Mod.EventBusSubscriber(modid = TooDeepSeas.MODID, value = Dist.CLIENT)
public final class RodTooltip {
    private RodTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        RodTier tier = ModItems.tierOf(stack);
        if (tier == null) return;

        List<Component> lines = new ArrayList<>();
        List<RodData.Slot> slots = RodData.slots(stack);
        lines.add(Component.translatable("tooltip.toodeepseas.tier", Fmt.num(tier.tier, 1)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.toodeepseas.ranuras", slots.size() + "/" + tier.accessorySlots, "I".repeat(tier.maxAccessoryLevel())).withStyle(ChatFormatting.GRAY));
        for (RodData.Slot slot : slots) {
            AccessoryDef def = Defs.ACCESSORIES.get(slot.id());
            int max = def == null ? slot.uses() : def.usesAt(slot.level());
            String level = def != null && def.maxLevel() > 1 ? " " + "I".repeat(Math.max(1, Math.min(3, slot.level()))) : "";
            lines.add(Component.literal(" • ").append(Component.translatable("accessory.toodeepseas." + slot.id()))
                    .append(level + " (" + slot.uses() + "/" + max + ")").withStyle(AccessoryItem.usesColor(slot.uses(), max)));
        }
        List<RodData.Bait> baits = RodData.baits(stack);
        if (tier.baitSlots > 1) lines.add(Component.translatable("tooltip.toodeepseas.cebos", tier.baitSlots).withStyle(ChatFormatting.GRAY));
        for (RodData.Bait bait : baits) {
            lines.add(Component.literal(" • ").append(Component.translatable("bait.toodeepseas." + bait.id()))
                    .append(" ×" + bait.count()).withStyle(ChatFormatting.YELLOW));
        }
        switch (tier) {
            case ORO, PRISMARINA, NETHERITA -> lines.add(Component.translatable("tooltip.toodeepseas.rasgo." + tier.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.AQUA));
            case VACIO -> lines.add(Component.translatable("tooltip.toodeepseas.rasgo.vacio").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            default -> {
            }
        }
        if (Screen.hasShiftDown()) {
            for (Component line : statLines(stack)) lines.add(Component.literal(" ").append(line).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            lines.add(Component.translatable("tooltip.toodeepseas.mayus").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    /**
     * Estadísticas finales de la caña con su equipo y encantamientos: siempre sedal, bajada, velocidad lateral y radio;
     * el resto, solo si la caña lo tiene.
     */
    public static List<Component> statLines(ItemStack rod) {
        List<Component> out = new ArrayList<>();
        RodStats s = RodData.stats(rod, FishingMedium.WATER, Double.MAX_VALUE);
        if (s == null) return out;
        out.add(Component.translatable("stat.toodeepseas.sedal", Fmt.num(s.maxDepth, 0)));
        out.add(Component.translatable("stat.toodeepseas.bajada", Fmt.num(s.base, 2), Fmt.pct(s.base / RodStats.BASE_DESCENT - 1)));
        out.add(Component.translatable("stat.toodeepseas.lateral", Fmt.pct(s.vh / RodStats.BASE_HSPEED - 1)));
        out.add(Component.translatable("stat.toodeepseas.radio", Fmt.num(s.hookR, 1)));
        if (s.luck > 0) out.add(Component.translatable("stat.toodeepseas.suerte", s.luck));
        if (s.attractR > 0) out.add(Component.translatable("stat.toodeepseas.atraccion", Fmt.num(s.attractR, 0)));
        if (s.dive != RodStats.BASE_DIVE) out.add(Component.translatable("stat.toodeepseas.buceo", Fmt.num(s.dive, 1)));
        if (s.magnetR > 0) out.add(Component.translatable("stat.toodeepseas.iman", Fmt.num(s.magnetR, 0)));
        if (s.dupChance > 0) out.add(Component.translatable("stat.toodeepseas.doble", Math.round(s.dupChance * 100)));
        if (s.costMul != 1) out.add(Component.translatable("stat.toodeepseas.coste", Fmt.pct(s.costMul - 1)));
        return out;
    }
}
