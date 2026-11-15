package com.rolflin.toodeepseas.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: golpe externo; recoge el sedal ya (sales con lo que llevabas). */
public record RecogerPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static RecogerPacket decode(FriendlyByteBuf buf) {
        return new RecogerPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rolflin.toodeepseas.client.MinijuegoCliente.recoger()));
        ctx.get().setPacketHandled(true);
    }
}
