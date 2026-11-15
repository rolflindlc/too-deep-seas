package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.RodTier;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TooDeepSeas.MODID);

    // La caña de madera (tier 0) es la caña vanilla
    public static final RegistryObject<TieredRodItem> CANA_COBRE = rod("cana_cobre", RodTier.COBRE, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_HIERRO = rod("cana_hierro", RodTier.HIERRO, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_ORO = rod("cana_oro", RodTier.ORO, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_PRISMARINA = rod("cana_prismarina", RodTier.PRISMARINA, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_DIAMANTE = rod("cana_diamante", RodTier.DIAMANTE, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_NETHERITA = rod("cana_netherita", RodTier.NETHERITA, new Item.Properties().fireResistant());
    public static final RegistryObject<TieredRodItem> CANA_VACIO = rod("cana_vacio", RodTier.VACIO, new Item.Properties().fireResistant().rarity(Rarity.EPIC));

    public static final List<RegistryObject<TieredRodItem>> RODS = List.of(
            CANA_COBRE, CANA_HIERRO, CANA_ORO, CANA_PRISMARINA, CANA_DIAMANTE, CANA_NETHERITA, CANA_VACIO);

    private ModItems() {
    }

    private static RegistryObject<TieredRodItem> rod(String name, RodTier tier, Item.Properties properties) {
        return ITEMS.register(name, () -> new TieredRodItem(tier, properties));
    }

    /** Tier de cualquier caña: las del mod, el suyo; la vanilla (u otra caña de otro mod), madera. */
    public static RodTier tierOf(ItemStack stack) {
        if (stack.getItem() instanceof TieredRodItem rod) return rod.getTier();
        return stack.getItem() instanceof FishingRodItem ? RodTier.MADERA : null;
    }
}
