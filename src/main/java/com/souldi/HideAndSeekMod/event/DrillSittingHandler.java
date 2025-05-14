package com.souldi.HideAndSeekMod.event;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.entity.DrillSeatEntity;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID)
public class DrillSittingHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<UUID, DrillSeatEntity> PLAYER_SEATS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide()) {
            Player player = event.player;
            UUID playerUUID = player.getUUID();

            boolean isHoldingDrill = player.getMainHandItem().getItem() instanceof SeekerDrillItem ||
                    player.getOffhandItem().getItem() instanceof SeekerDrillItem;
            boolean hasSeat = PLAYER_SEATS.containsKey(playerUUID);
            boolean isRidingSeat = hasSeat && player.isPassenger() && player.getVehicle() == PLAYER_SEATS.get(playerUUID);

            if (isHoldingDrill && !isRidingSeat && player instanceof ServerPlayer) {
                // Если игрок сидит на чем-то другом, не трогаем его
                if (player.isPassenger() && !hasSeat) {
                    return;
                }

                // Если у игрока уже есть сиденье, но он не на нем, удаляем старое
                if (hasSeat) {
                    DrillSeatEntity oldSeat = PLAYER_SEATS.get(playerUUID);
                    if (oldSeat != null && !oldSeat.isRemoved()) {
                        oldSeat.discard();
                    }
                    PLAYER_SEATS.remove(playerUUID);
                }

                // Создаем новое сиденье там, где стоит игрок
                ServerLevel serverLevel = (ServerLevel) player.level;
                DrillSeatEntity seatEntity = new DrillSeatEntity(serverLevel, player.getX(), player.getY(), player.getZ());

                serverLevel.addFreshEntity(seatEntity);
                player.startRiding(seatEntity, true);
                PLAYER_SEATS.put(playerUUID, seatEntity);
                LOGGER.info("Player " + player.getName().getString() + " started riding drill seat");
            } else if (!isHoldingDrill && hasSeat) {
                DrillSeatEntity seat = PLAYER_SEATS.get(playerUUID);

                if (seat != null && !seat.isRemoved()) {
                    player.stopRiding();
                    seat.discard();
                    LOGGER.info("Player " + player.getName().getString() + " stopped riding drill seat (no longer holding drill)");
                }

                PLAYER_SEATS.remove(playerUUID);
            }

            // Проверяем наличие сиденья и исправность его состояния
            if (hasSeat) {
                DrillSeatEntity seat = PLAYER_SEATS.get(playerUUID);
                if (seat == null || seat.isRemoved()) {
                    PLAYER_SEATS.remove(playerUUID);
                    LOGGER.info("Removed invalid drill seat for player " + player.getName().getString());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();

        if (PLAYER_SEATS.containsKey(playerUUID)) {
            DrillSeatEntity seat = PLAYER_SEATS.get(playerUUID);

            if (seat != null && !seat.isRemoved()) {
                seat.discard();
                LOGGER.info("Removed drill seat for logged out player " + player.getName().getString());
            }

            PLAYER_SEATS.remove(playerUUID);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();

        if (PLAYER_SEATS.containsKey(playerUUID)) {
            DrillSeatEntity seat = PLAYER_SEATS.get(playerUUID);

            if (seat != null && !seat.isRemoved()) {
                seat.discard();
                LOGGER.info("Removed drill seat for dimension-changing player " + player.getName().getString());
            }

            PLAYER_SEATS.remove(playerUUID);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();

        if (PLAYER_SEATS.containsKey(playerUUID)) {
            DrillSeatEntity seat = PLAYER_SEATS.get(playerUUID);

            if (seat != null && !seat.isRemoved()) {
                seat.discard();
                LOGGER.info("Removed drill seat for respawning player " + player.getName().getString());
            }

            PLAYER_SEATS.remove(playerUUID);
        }
    }
}