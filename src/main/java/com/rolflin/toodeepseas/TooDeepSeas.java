package com.rolflin.toodeepseas;

import com.rolflin.toodeepseas.block.ModBlocks;
import com.rolflin.toodeepseas.item.AccessoryItem;
import com.rolflin.toodeepseas.item.BaitItem;
import com.rolflin.toodeepseas.item.MaterialItem;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.loot.AddItemModifier;
import com.rolflin.toodeepseas.item.TieredRodItem;
import com.rolflin.toodeepseas.menu.ModMenus;
import com.rolflin.toodeepseas.net.Network;
import com.rolflin.toodeepseas.recipe.ModRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(TooDeepSeas.MODID)
public class TooDeepSeas {
    public static final String MODID = "toodeepseas";

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
            })
            .build());

    public TooDeepSeas() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModMenus.MENUS.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        ModRecipes.TYPES.register(bus);
        AddItemModifier.SERIALIZERS.register(bus);
        TABS.register(bus);
        Network.register();
    }
}
