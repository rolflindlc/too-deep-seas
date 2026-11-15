package com.rolflin.toodeepseas.client;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.item.TieredRodItem;
import com.rolflin.toodeepseas.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = TooDeepSeas.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.MESA_PESCADOR.get(), MesaPescadorScreen::new);
            // Igual que la caña vanilla: el modelo cambia a «lanzada» mientras el anzuelo está fuera
            for (RegistryObject<TieredRodItem> rod : ModItems.RODS) {
                ItemProperties.register(rod.get(), new ResourceLocation("cast"), (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0F;
                    boolean main = entity.getMainHandItem() == stack;
                    boolean off = entity.getOffhandItem() == stack;
                    if (entity.getMainHandItem().getItem() instanceof FishingRodItem) off = false;
                    return (main || off) && entity instanceof Player player && player.fishing != null ? 1.0F : 0.0F;
                });
            }
        });
    }
}
