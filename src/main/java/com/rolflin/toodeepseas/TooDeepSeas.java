package com.rolflin.toodeepseas;

import com.rolflin.toodeepseas.block.ModBlocks;
import com.rolflin.toodeepseas.item.AccessoryItem;
import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.item.BaitItem;
import com.rolflin.toodeepseas.item.CofreClimaItem;
import com.rolflin.toodeepseas.item.CofreItem;
import com.rolflin.toodeepseas.item.MaterialItem;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.effect.ModEffects;
import com.rolflin.toodeepseas.effect.ModPotions;
import com.rolflin.toodeepseas.item.ComidaItem;
import com.rolflin.toodeepseas.item.PezItem;
import com.rolflin.toodeepseas.item.TesoroItem;
import com.rolflin.toodeepseas.loot.AddItemModifier;
import com.rolflin.toodeepseas.loot.CapturaPescaModifier;
import net.minecraft.world.item.alchemy.PotionUtils;
import com.rolflin.toodeepseas.loot.CofrePescaModifier;
import com.rolflin.toodeepseas.item.TieredRodItem;
import com.rolflin.toodeepseas.menu.ModMenus;
import com.rolflin.toodeepseas.net.Network;
import com.rolflin.toodeepseas.recipe.ModRecipes;
import com.rolflin.toodeepseas.world.Aldeas;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(TooDeepSeas.MODID)
public class TooDeepSeas {
    public static final String MODID = "toodeepseas";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Pestaña propia del creativo: cañas, mesa, accesorios en todos sus niveles, cebos y materiales. */
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.toodeepseas"))
            .icon(() -> new ItemStack(ModItems.CANA_PRISMARINA.get()))
            .displayItems((parameters, output) -> {
                output.accept(Items.FISHING_ROD);
                for (RegistryObject<TieredRodItem> rod : ModItems.RODS) output.accept(rod.get());
                output.accept(ModItems.MESA_PESCADOR.get());
                for (RegistryObject<AccessoryItem> entry : ModItems.ACCESSORIES.values()) {
                    AccessoryItem item = entry.get();
                    for (int level = 1; level <= item.maxLevel(); level++) output.accept(item.create(level, -1));
                }
                for (RegistryObject<BaitItem> bait : ModItems.BAITS.values()) output.accept(bait.get());
                for (RegistryObject<MaterialItem> material : ModItems.MATERIALS) output.accept(material.get());
                // Cofres: las 4 calidades de cada bioma y, de muestra, el mejor posible (abisal y con tormenta)
                for (RegistryObject<CofreItem> entry : ModItems.COFRES.values()) {
                    CofreItem cofre = entry.get();
                    for (Cofres.Calidad calidad : Cofres.Calidad.values()) {
                        output.accept(cofre.create(calidad, Cofres.Banda.SUPERFICIE, Cofres.Clima.DESPEJADO));
                    }
                    output.accept(cofre.create(Cofres.Calidad.ANCESTRAL, Cofres.Banda.ABISAL, Cofres.Clima.TORMENTA));
                }
                // Cofres exclusivos de clima: las 4 calidades y el abisal
                for (RegistryObject<CofreClimaItem> entry : ModItems.COFRES_CLIMA.values()) {
                    for (Cofres.Calidad calidad : Cofres.Calidad.values()) output.accept(entry.get().create(calidad, Cofres.Banda.SUPERFICIE));
                    output.accept(entry.get().create(Cofres.Calidad.ANCESTRAL, Cofres.Banda.ABISAL));
                }
            })
            .build());

    /** Segunda pestaña: los peces, los tesoros, la comida y las pociones nuevas. */
    public static final RegistryObject<CreativeModeTab> TAB_CAPTURAS = TABS.register("capturas", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.toodeepseas.capturas"))
            .withTabsBefore(TAB.getKey())
            .icon(() -> new ItemStack(ModItems.PECES.get("pez_espada").get()))
            .displayItems((parameters, output) -> {
                for (RegistryObject<PezItem> pez : ModItems.PECES.values()) output.accept(pez.get());
                for (RegistryObject<TesoroItem> tesoro : ModItems.TESOROS.values()) output.accept(tesoro.get());
                for (RegistryObject<ComidaItem> comida : ModItems.COMIDAS.values()) output.accept(comida.get());
                for (ModPotions.Familia familia : ModPotions.FAMILIAS.values()) {
                    output.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), familia.base().get()));
                    if (familia.larga() != null) output.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), familia.larga().get()));
                    if (familia.fuerte() != null) output.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), familia.fuerte().get()));
                }
            })
            .build());

    public TooDeepSeas() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModPotions.POTIONS.register(bus);
        ModMenus.MENUS.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        ModRecipes.TYPES.register(bus);
        // Cargar la clase registra su serializador en el mismo registro que AddItemModifier
        CofrePescaModifier.COFRE_PESCA.getId();
        CapturaPescaModifier.CAPTURA_PESCA.getId();
        AddItemModifier.SERIALIZERS.register(bus);
        TABS.register(bus);
        Network.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // La mesa del pescador como puesto de trabajo del aldeano pescador
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> Aldeas.aplicarPuesto(Config.BARRIL_LEGADO.get())));
        // Pociones: poción rara + pez o tesoro
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(ModPotions::registrarMezclas));
    }
}
