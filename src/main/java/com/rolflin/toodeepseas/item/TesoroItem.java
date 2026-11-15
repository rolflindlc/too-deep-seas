package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.juego.Catalogo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tesoro de pesca propio del mod. Según su tipo: material (recetas y pociones), «abrir» (da botín de
 * {@code loot_tables/tesoros/<id>}), «caliz» (quita los efectos dañinos, con espera) y «cabeza»/«mano»
 * (efectos mientras se lleva; ver {@link CapturasEventos} y {@link CoronaItem}).
 */
public class TesoroItem extends Item {
    public static final int ESPERA_CALIZ = 6000;
    protected final Catalogo.Tesoro def;

    public TesoroItem(Catalogo.Tesoro def) {
        super(properties(def));
        this.def = def;
    }

    private static Properties properties(Catalogo.Tesoro def) {
        Properties p = new Properties().rarity(Capturas.rareza(def.rarity())).stacksTo(def.stack());
        return def.biomes().contains(com.rolflin.toodeepseas.core.Cofres.Bioma.LAVA) && def.biomes().size() == 1 ? p.fireResistant() : p;
    }

    public Catalogo.Tesoro def() {
        return def;
    }

    @Override
    public String getDescriptionId() {
        return Capturas.clave(def.id());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return def.rarity() >= 4 || super.isFoil(stack);
    }

    /** Lo que da al abrirlo (solo los de tipo «abrir»). */
    public List<ItemStack> abrir(ServerLevel level, @Nullable Player player) {
        LootTable table = level.getServer().getLootData().getLootTable(new ResourceLocation(TooDeepSeas.MODID, "tesoros/" + def.id()));
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player == null ? net.minecraft.world.phys.Vec3.ZERO : player.position());
        if (player != null) builder.withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck());
        List<ItemStack> out = new ArrayList<>(table.getRandomItems(builder.create(LootContextParamSets.CHEST)));
        if (def.leftover() != null) {
            Item rest = ForgeRegistries.ITEMS.getValue(new ResourceLocation(def.leftover()));
            if (rest != null) out.add(new ItemStack(rest));
        }
        return out;
    }

    /** Quita los efectos dañinos; devuelve cuántos ha quitado. */
    public static int purificar(LivingEntity entity) {
        List<MobEffect> malos = new ArrayList<>();
        for (MobEffectInstance e : entity.getActiveEffects()) {
            if (e.getEffect().getCategory() == MobEffectCategory.HARMFUL) malos.add(e.getEffect());
        }
        for (MobEffect e : malos) entity.removeEffect(e);
        return malos.size();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (def.kind().equals("abrir")) {
            if (level instanceof ServerLevel server) {
                List<ItemStack> loot = abrir(server, player);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                for (ItemStack item : loot) {
                    if (!player.getInventory().add(item) && !item.isEmpty()) player.drop(item, false);
                }
                if (def.xp() > 0) ExperienceOrb.award(server, player.position(), def.xp());
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5F, 1.3F);
                player.awardStat(Stats.ITEM_USED.get(this));
                if (player instanceof ServerPlayer sp) sp.containerMenu.broadcastChanges();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (def.kind().equals("caliz")) {
            if (!level.isClientSide()) {
                purificar(player);
                player.getCooldowns().addCooldown(this, ESPERA_CALIZ);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8F, 1.0F);
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.toodeepseas.captura.rareza." + def.rarity()).append(" · ")
                .append(Component.translatable("tooltip.toodeepseas.captura.tesoro")).withStyle(ChatFormatting.BLUE));
        Capturas.usos(tooltip, def.id(), def.lines());
    }
}
