package com.souldi.HideAndSeekMod.network;

import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * Пакет для передачи сигнала о закрытии меню по ЛКМ с клиента на сервер
 */
public class DrillMenuClosePacket {
    private static final Logger LOGGER = LogManager.getLogger();

    public DrillMenuClosePacket() {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static DrillMenuClosePacket decode(FriendlyByteBuf buf) {
        return new DrillMenuClosePacket();
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack mainHandItem = player.getMainHandItem();

                if (mainHandItem.getItem() instanceof SeekerDrillItem) {
                    // Сброс всех тегов у бура при закрытии меню по ЛКМ
                    SeekerDrillItem.resetDrillState(mainHandItem);
                    LOGGER.info("[DrillMenuClosePacket] Сброшено состояние бура на сервере при закрытии меню по ЛКМ");
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}