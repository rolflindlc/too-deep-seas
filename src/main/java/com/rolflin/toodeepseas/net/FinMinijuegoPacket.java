package com.rolflin.toodeepseas.net;

import com.rolflin.toodeepseas.juego.Sesiones;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente → servidor: la partida ha terminado; estas son las teclas, paso a paso, para que el servidor la repita. */
public record FinMinijuegoPacket(byte[] grabacion) {
    /** Tope de la grabación (cada cambio de teclas ocupa 3 bytes). */
    public static final int MAX_BYTES = 30000;

    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(grabacion);
    }

    public static FinMinijuegoPacket decode(FriendlyByteBuf buf) {
        return new FinMinijuegoPacket(buf.readByteArray(MAX_BYTES));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) Sesiones.alTerminar(player, grabacion);
        });
        ctx.get().setPacketHandled(true);
    }
}
