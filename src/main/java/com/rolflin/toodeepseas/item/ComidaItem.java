package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.core.juego.Catalogo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Pescado cocinado y platos. Los que van en cuenco (o en palo) lo devuelven al comerlos. */
public class ComidaItem extends Item {
    private final Catalogo.Comida def;

    public ComidaItem(Catalogo.Comida def) {
        super(new Properties().stacksTo(def.container() == null ? 64 : 16)
                .food(Capturas.comida(def.nutrition(), def.saturation(), def.effects(), !def.effects().isEmpty())));
        this.def = def;
    }

    public Catalogo.Comida def() {
        return def;
    }

    @Override
    public String getDescriptionId() {
        return Capturas.clave(def.id());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (def.container() == null || (entity instanceof Player p && p.getAbilities().instabuild)) return rest;
        Item container = ForgeRegistries.ITEMS.getValue(new ResourceLocation(def.container()));
        if (container == null) return rest;
        ItemStack empty = new ItemStack(container);
        if (rest.isEmpty()) return empty;
        if (entity instanceof Player player && !player.getInventory().add(empty)) player.drop(empty, false);
        return rest;
    }
}
