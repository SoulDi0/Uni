package com.souldi.HideAndSeekMod.network;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HideAndSeekMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, DrillTargetPacket.class,
                DrillTargetPacket::encode,
                DrillTargetPacket::decode,
                DrillTargetPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        INSTANCE.registerMessage(id++, DrillMenuClosePacket.class,
                DrillMenuClosePacket::encode,
                DrillMenuClosePacket::decode,
                DrillMenuClosePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}