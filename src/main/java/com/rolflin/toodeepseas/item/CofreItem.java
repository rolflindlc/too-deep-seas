package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.core.Cofres.Banda;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Calidad;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Cofre del tesoro pescado (como las cajas de pesca de Terraria). Hay un objeto por bioma; la calidad,
 * la banda de profundidad y el clima van en el NBT ({@code Calidad}, {@code Banda}, {@code Clima}; sin etiqueta = 0).
 * Clic derecho lo abre: se gasta y da el botín de sus tablas ({@link Cofres#tiradas}).
 */
public class CofreItem extends Item {
    public static final String CALIDAD = "Calidad";
    public static final String BANDA = "Banda";
    public static final String CLIMA = "Clima";

    private final Bioma bioma;

    public CofreItem(Bioma bioma, Properties properties) {
        super(properties.stacksTo(16));
        this.bioma = bioma;
    }

    public Bioma bioma() {
        return bioma;
    }

    private static int read(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(key);
    }

    public static Calidad calidad(ItemStack stack) {
        return Calidad.byIndex(read(stack, CALIDAD));
    }

    public static Banda banda(ItemStack stack) {
        return Banda.byIndex(read(stack, BANDA));
    }

    public Clima clima(ItemStack stack) {
        return Cofres.climaEfectivo(bioma, Clima.byIndex(read(stack, CLIMA)));
    }

    /** Los valores por defecto no se guardan, para que dos cofres iguales se apilen siempre. */
    public ItemStack create(Calidad calidad, Banda banda, Clima clima) {
        ItemStack stack = new ItemStack(this);
        clima = Cofres.climaEfectivo(bioma, clima);
        if (calidad.ordinal() > 0) stack.getOrCreateTag().putInt(CALIDAD, calidad.ordinal());
        if (banda.ordinal() > 0) stack.getOrCreateTag().putInt(BANDA, banda.ordinal());
        if (clima.ordinal() > 0) stack.getOrCreateTag().putInt(CLIMA, clima.ordinal());
        return stack;
    }

    /** Botín de un cofre: tira de cada tabla las veces que toca y junta lo que sea igual. */
    public List<ItemStack> roll(ServerLevel level, ItemStack stack, Vec3 origin, @Nullable Player player) {
        LootParams.Builder builder = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin);
        if (player != null) builder.withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck());
        LootParams params = builder.create(LootContextParamSets.CHEST);
        List<ItemStack> out = new ArrayList<>();
        for (Cofres.Tirada tirada : Cofres.tiradas(bioma, calidad(stack), banda(stack), clima(stack))) {
            LootTable table = level.getServer().getLootData().getLootTable(new ResourceLocation(TooDeepSeas.MODID, tirada.table()));
            for (int i = 0; i < tirada.rolls(); i++) {
                for (ItemStack loot : table.getRandomItems(params)) merge(out, loot);
            }
        }
        return out;
    }

    private static void merge(List<ItemStack> out, ItemStack loot) {
        if (loot.isEmpty()) return;
        for (ItemStack have : out) {
            if (have.getCount() < have.getMaxStackSize() && ItemStack.isSameItemSameTags(have, loot)) {
                int moved = Math.min(loot.getCount(), have.getMaxStackSize() - have.getCount());
                have.grow(moved);
                loot.shrink(moved);
                if (loot.isEmpty()) return;
            }
        }
        out.add(loot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            List<ItemStack> loot = roll(server, stack, player.position(), player);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            for (ItemStack item : loot) {
                // Lo que no quepa en el inventario cae a los pies, a nombre del jugador
                if (!player.getInventory().add(item) && !item.isEmpty()) {
                    ItemEntity dropped = player.drop(item, false);
                    if (dropped != null) {
                        dropped.setNoPickUpDelay();
                        dropped.setTarget(player.getUUID());
                    }
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.6F, 1.1F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 1.4F);
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player instanceof ServerPlayer sp) sp.containerMenu.broadcastChanges();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return switch (calidad(stack)) {
            case SENCILLO -> Rarity.COMMON;
            case REFORZADO -> Rarity.UNCOMMON;
            case ORNAMENTADO -> Rarity.RARE;
            case ANCESTRAL -> Rarity.EPIC;
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.toodeepseas.cofre.nombre",
                Component.translatable("cofre.toodeepseas.calidad." + calidad(stack).id()),
                Component.translatable("cofre.toodeepseas.bioma." + bioma.id()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.toodeepseas.cofre.profundidad",
                Component.translatable("cofre.toodeepseas.banda." + banda(stack).id())).withStyle(ChatFormatting.BLUE));
        if (bioma.hasWeather) {
            tooltip.add(Component.translatable("tooltip.toodeepseas.cofre.clima",
                    Component.translatable("cofre.toodeepseas.clima." + clima(stack).id())).withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("tooltip.toodeepseas.cofre.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.toodeepseas.cofre.abrir").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
