package com.rolflin.pesca;

import com.rolflin.pesca.item.ModItems;
import com.rolflin.pesca.item.TieredRodItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;

@Mod(PescaMod.MODID)
public class PescaMod {
    public static final String MODID = "pesca";

    public PescaMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (RegistryObject<TieredRodItem> rod : ModItems.RODS) event.accept(rod);
        }
    }
}
