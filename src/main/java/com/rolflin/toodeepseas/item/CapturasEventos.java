package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.juego.Catalogo;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Efectos de los tesoros que se llevan puestos y compras del aldeano pescador. */
@Mod.EventBusSubscriber(modid = TooDeepSeas.MODID)
public final class CapturasEventos {
    private CapturasEventos() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || event.player.tickCount % 40 != 0) return;
        aplicarReliquias(event.player);
    }

    /** Corona en la cabeza o trofeo en la mano secundaria: renueva sus efectos (duran algo más que el intervalo). */
    public static void aplicarReliquias(LivingEntity entity) {
        aplicar(entity, entity.getItemBySlot(EquipmentSlot.HEAD), "cabeza");
        aplicar(entity, entity.getItemBySlot(EquipmentSlot.OFFHAND), "mano");
    }

    private static void aplicar(LivingEntity entity, ItemStack stack, String kind) {
        if (!(stack.getItem() instanceof TesoroItem tesoro) || !tesoro.def().kind().equals(kind)) return;
        for (Catalogo.Efecto e : tesoro.def().effects()) {
            MobEffect effect = Capturas.efecto(e.id());
            if (effect != null) entity.addEffect(new MobEffectInstance(effect, 300, e.amplifier(), true, false, true));
        }
    }

    /** El pescador compra peces del mod (más esmeraldas cuanto más raros) y monedas de oro. */
    @SubscribeEvent
    public static void onTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.FISHERMAN) return;
        var trades = event.getTrades();
        trades.get(1).add(compra(0, 8, 1, 2));
        trades.get(2).add(compra(1, 5, 1, 5));
        trades.get(2).add((trader, random) -> new MerchantOffer(new ItemStack(ModItems.TESOROS.get("moneda_de_oro").get()),
                new ItemStack(Items.EMERALD), 12, 5, 0.05F));
        trades.get(3).add(compra(2, 1, 2, 10));
        trades.get(4).add(compra(3, 1, 6, 15));
        trades.get(5).add(compra(4, 1, 24, 30));
    }

    /** Compra {@code count} peces (uno al azar de esa rareza) por {@code emeralds} esmeraldas. */
    public static VillagerTrades.ItemListing compra(int rarity, int count, int emeralds, int xp) {
        return (trader, random) -> {
            List<PezItem> options = new ArrayList<>();
            for (RegistryObject<PezItem> pez : ModItems.PECES.values()) if (pez.get().def().rarity() == rarity) options.add(pez.get());
            if (options.isEmpty()) return null;
            PezItem pez = options.get(random.nextInt(options.size()));
            return new MerchantOffer(new ItemStack(pez, count), new ItemStack(Items.EMERALD, emeralds), 12, xp, 0.05F);
        };
    }
}
