package com.rolflin.toodeepseas.menu;

import com.rolflin.toodeepseas.TooDeepSeas;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TooDeepSeas.MODID);

    public static final RegistryObject<MenuType<MesaPescadorMenu>> MESA_PESCADOR = MENUS.register("mesa_pescador",
            () -> new MenuType<>(MesaPescadorMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }
}
