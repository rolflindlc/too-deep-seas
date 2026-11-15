package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.core.juego.Catalogo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Tesoro que se lleva en la cabeza (las coronas): clic derecho para ponérselo; sus efectos los da {@link CapturasEventos}. */
public class CoronaItem extends TesoroItem implements Equipable {
    public CoronaItem(Catalogo.Tesoro def) {
        super(def);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
}
