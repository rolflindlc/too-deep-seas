package com.rolflin.toodeepseas.net;

import com.rolflin.toodeepseas.data.Defs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

/** Servidor → cliente: los JSON de accesorios y cebos, para que el cliente calcule lo mismo que el servidor. */
public record SyncDefsPacket(Map<String, String> accessories, Map<String, String> baits) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(accessories, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
        buf.writeMap(baits, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
    }

    public static SyncDefsPacket decode(FriendlyByteBuf buf) {
        return new SyncDefsPacket(buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf), buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Defs.ACCESSORIES.setFromRaw(accessories);
            Defs.BAITS.setFromRaw(baits);
        });
        ctx.get().setPacketHandled(true);
    }
}
