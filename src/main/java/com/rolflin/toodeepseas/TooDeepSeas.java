package com.rolflin.toodeepseas;

import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.item.TieredRodItem;
import com.rolflin.toodeepseas.net.Network;
import com.rolflin.toodeepseas.recipe.ModRecipes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;

@Mod(TooDeepSeas.MODID)
public class TooDeepSeas {
    public static final String MODID = "toodeepseas";

    public TooDeepSeas() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        Network.register();
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (RegistryObject<TieredRodItem> rod : ModItems.RODS) event.accept(rod);
        }
    }
}
