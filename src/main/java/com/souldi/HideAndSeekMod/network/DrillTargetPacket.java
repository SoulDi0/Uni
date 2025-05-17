package com.souldi.HideAndSeekMod.network;

import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Пакет для синхронизации прокрутки бура между клиентом и сервером
 */
public class DrillTargetPacket {
    private final UUID dummyUUID;
    private final int scrollDirection;

    public DrillTargetPacket(UUID dummyUUID, int scrollDirection) {
        this.dummyUUID = dummyUUID;
        this.scrollDirection = scrollDirection;
    }

    public static void encode(DrillTargetPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.dummyUUID);
        buffer.writeInt(packet.scrollDirection);
    }

    public static DrillTargetPacket decode(FriendlyByteBuf buffer) {
        return new DrillTargetPacket(buffer.readUUID(), buffer.readInt());
    }

    public static void handle(DrillTargetPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            ItemStack mainHandItem = player.getMainHandItem();

            if (mainHandItem.getItem() instanceof SeekerDrillItem drillItem) {
                drillItem.onScroll(mainHandItem, player, packet.scrollDirection);
            }
        });

        context.setPacketHandled(true);
    }
}