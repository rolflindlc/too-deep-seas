package com.rolflin.toodeepseas.data;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.AccessoryDef;
import com.rolflin.toodeepseas.core.BaitDef;
import com.rolflin.toodeepseas.core.juego.PezDef;
import com.rolflin.toodeepseas.net.Network;
import com.rolflin.toodeepseas.net.SyncDefsPacket;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Accesorios, cebos y peces cargados del datapack, y su envío a los clientes. */
@Mod.EventBusSubscriber(modid = TooDeepSeas.MODID)
public final class Defs {
    public static final DefRegistry<AccessoryDef> ACCESSORIES = new DefRegistry<>("accessories", AccessoryDef::parse);
    public static final DefRegistry<BaitDef> BAITS = new DefRegistry<>("baits", BaitDef::parse);
    /** Peces del minijuego ({@code data/<ns>/fish/*.json}). */
    public static final DefRegistry<PezDef> PECES = new DefRegistry<>("fish", PezDef::parse);

    private Defs() {
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(ACCESSORIES);
        event.addListener(BAITS);
        event.addListener(PECES);
    }

    /** Al entrar un jugador y después de cada /reload. */
    @SubscribeEvent
    public static void onSync(OnDatapackSyncEvent event) {
        SyncDefsPacket packet = new SyncDefsPacket(ACCESSORIES.raw(), BAITS.raw(), PECES.raw());
        if (event.getPlayer() != null) Network.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), packet);
        else Network.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
