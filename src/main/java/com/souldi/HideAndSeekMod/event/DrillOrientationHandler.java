package com.souldi.HideAndSeekMod.event;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.entity.DrillSeatEntity;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID)
public class DrillOrientationHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String IS_ACTIVE_KEY = "IsActive";

    private static final int UPDATE_FREQUENCY = 1; // Каждый тик
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        try {
            tickCounter++;

            if (tickCounter % UPDATE_FREQUENCY != 0) {
                return;
            }

            if (event.phase == TickEvent.Phase.END) {
                Player player = event.player;

                if (isPlayerDrilling(player)) {
                    Entity vehicle = player.getVehicle();
                    if (vehicle instanceof DrillSeatEntity drillSeat) {
                        float currentYHeadRot = player.getYHeadRot();
                        float currentXRot = player.getXRot();

                        UUID targetUUID = drillSeat.getTargetPlayerUUID();
                        if (targetUUID != null) {
                            Player targetPlayer = null;
                            if (!player.level.isClientSide && player.level instanceof ServerLevel serverLevel) {
                                for (ServerPlayer serverPlayer : serverLevel.getServer().getPlayerList().getPlayers()) {
                                    if (serverPlayer.getUUID().equals(targetUUID)) {
                                        targetPlayer = serverPlayer;
                                        break;
                                    }
                                }
                            }

                            if (targetPlayer != null) {
                                Vec3 currentPos = player.position();
                                Vec3 targetPos = targetPlayer.position();
                                Vec3 direction = targetPos.subtract(currentPos);

                                Vec3 horizontalDir = new Vec3(direction.x, 0, direction.z).normalize();

                                float targetYaw = (float) Math.toDegrees(Math.atan2(-horizontalDir.x, horizontalDir.z));

                                player.setYRot(targetYaw);
                                player.setYBodyRot(targetYaw);

                                player.setXRot(90.0F);

                                if (!player.level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                                    CompoundTag playerData = serverPlayer.getPersistentData();
                                    CompoundTag drillData = new CompoundTag();
                                    drillData.putBoolean("IsDrilling", true);
                                    drillData.putFloat("BodyYaw", targetYaw);
                                    drillData.putFloat("HeadYaw", currentYHeadRot);
                                    drillData.putFloat("HeadPitch", currentXRot);
                                    drillData.putBoolean("AllowHeadMovement", true);
                                    playerData.put("DrillData", drillData);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка в DrillOrientationHandler.onPlayerTick: " + e.getMessage(), e);
        }
    }

    private static boolean isPlayerDrilling(Player player) {
        if (player.isPassenger() && player.getVehicle() instanceof DrillSeatEntity) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            if (mainHand.getItem() instanceof SeekerDrillItem) {
                CompoundTag tag = mainHand.getTag();
                if (tag != null && tag.getBoolean(IS_ACTIVE_KEY)) {
                    return true;
                }
            }

            if (offHand.getItem() instanceof SeekerDrillItem) {
                CompoundTag tag = offHand.getTag();
                if (tag != null && tag.getBoolean(IS_ACTIVE_KEY)) {
                    return true;
                }
            }
        }

        return false;
    }
}