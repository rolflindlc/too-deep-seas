package com.rolflin.toodeepseas.net;

import com.rolflin.toodeepseas.TooDeepSeas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class Network {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TooDeepSeas.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Network() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, SyncDefsPacket.class, SyncDefsPacket::encode, SyncDefsPacket::decode, SyncDefsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, AbrirMinijuegoPacket.class, AbrirMinijuegoPacket::encode, AbrirMinijuegoPacket::decode, AbrirMinijuegoPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, RecogerPacket.class, RecogerPacket::encode, RecogerPacket::decode, RecogerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, FinMinijuegoPacket.class, FinMinijuegoPacket::encode, FinMinijuegoPacket::decode, FinMinijuegoPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
