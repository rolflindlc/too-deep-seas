package com.rolflin.toodeepseas.net;

import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import com.rolflin.toodeepseas.core.juego.Partida;
import com.rolflin.toodeepseas.core.juego.Perfil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.function.Supplier;

/** Servidor → cliente: el anzuelo ha tocado el agua; abre el minijuego con lo que ha medido el servidor. */
public record AbrirMinijuegoPacket(Partida.Ajustes ajustes) {
    public void encode(FriendlyByteBuf buf) {
        Partida.Ajustes a = ajustes;
        buf.writeInt(a.seed());
        buf.writeEnum(a.bioma());
        buf.writeEnum(a.sub());
        buf.writeEnum(a.clima());
        buf.writeDouble(a.maxD());
        buf.writeDouble(a.base());
        buf.writeDouble(a.k());
        buf.writeDouble(a.vh());
        buf.writeDouble(a.dive());
        buf.writeDouble(a.hookR());
        buf.writeVarInt(a.luck());
        buf.writeVarInt(a.lure());
        buf.writeDouble(a.attractR());
        buf.writeDouble(a.magnetR());
        buf.writeCollection(a.cebos(), FriendlyByteBuf::writeUtf);
    }

    public static AbrirMinijuegoPacket decode(FriendlyByteBuf buf) {
        return new AbrirMinijuegoPacket(new Partida.Ajustes(buf.readInt(), buf.readEnum(Bioma.class), buf.readEnum(Perfil.SubBioma.class), buf.readEnum(Clima.class),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readCollection(HashSet::new, FriendlyByteBuf::readUtf)));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.rolflin.toodeepseas.client.MinijuegoCliente.abrir(ajustes)));
        ctx.get().setPacketHandled(true);
    }
}
