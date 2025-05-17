package com.souldi.HideAndSeekMod.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.entity.DrillSeatEntity;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Улучшенный обработчик для рендеринга игрока во время бурения
 * Игрок будет лежать горизонтально, ногами к целевому игроку, с полной свободой камеры
 */
@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID, value = Dist.CLIENT)
public class DrillPlayerRenderHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String IS_ACTIVE_KEY = "IsActive";

    private static final Map<UUID, Float> LAST_TARGET_YAW = new HashMap<>();

    // Константы для сглаживания поворотов
    private static final float SMOOTHING_FACTOR = 0.2F;
    private static final float MAX_DELTA_PER_TICK = 5.0F;

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        try {
            Player player = event.getEntity();

            if (isPlayerDrilling(player)) {
                Vec3 directionToTarget = getDirectionToTarget(player);

                float rawTargetYaw = (float) Math.toDegrees(Math.atan2(-directionToTarget.x, directionToTarget.z));

                float smoothedYaw = getSmoothRotation(player.getUUID(), rawTargetYaw);

                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();

                float playerHeight = player.getBbHeight();
                poseStack.translate(0, playerHeight / 2, 0);

                poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));

                poseStack.mulPose(Vector3f.ZP.rotationDegrees(smoothedYaw + 180.0F));

                poseStack.translate(0, -playerHeight / 2, 0);

                poseStack.translate(0, -0.2, 0);
            } else {
                LAST_TARGET_YAW.remove(player.getUUID());
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка в DrillPlayerRenderHandler.onRenderPlayer: " + e.getMessage(), e);
        }
    }

    private static float getSmoothRotation(UUID playerUUID, float targetYaw) {
        targetYaw = (targetYaw % 360 + 360) % 360;

        if (!LAST_TARGET_YAW.containsKey(playerUUID)) {
            LAST_TARGET_YAW.put(playerUUID, targetYaw);
            return targetYaw;
        }

        float lastYaw = LAST_TARGET_YAW.get(playerUUID);

        float deltaYaw = targetYaw - lastYaw;
        if (deltaYaw > 180) {
            deltaYaw -= 360;
        } else if (deltaYaw < -180) {
            deltaYaw += 360;
        }

        if (deltaYaw > MAX_DELTA_PER_TICK) {
            deltaYaw = MAX_DELTA_PER_TICK;
        } else if (deltaYaw < -MAX_DELTA_PER_TICK) {
            deltaYaw = -MAX_DELTA_PER_TICK;
        }

        float smoothedYaw = lastYaw + deltaYaw * SMOOTHING_FACTOR;

        smoothedYaw = (smoothedYaw % 360 + 360) % 360;

        LAST_TARGET_YAW.put(playerUUID, smoothedYaw);

        return smoothedYaw;
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        try {
            if (isPlayerDrilling(event.getEntity())) {
                event.getPoseStack().popPose();
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка в DrillPlayerRenderHandler.onRenderPlayerPost: " + e.getMessage(), e);
        }
    }

    private static Vec3 getDirectionToTarget(Player player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof DrillSeatEntity drillSeat)) {
            float yaw = player.getYRot();
            return new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        }

        UUID targetUUID = drillSeat.getTargetPlayerUUID();
        if (targetUUID == null) {
            Vec3 moveDir = drillSeat.getMoveDirection();
            if (moveDir != null && moveDir.length() > 0.001) {
                return new Vec3(moveDir.x, 0, moveDir.z).normalize();
            } else {
                float yaw = player.getYRot();
                return new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
            }
        }

        Player targetPlayer = Minecraft.getInstance().level.getPlayerByUUID(targetUUID);
        if (targetPlayer == null) {
            Vec3 moveDir = drillSeat.getMoveDirection();
            if (moveDir != null && moveDir.length() > 0.001) {
                return new Vec3(moveDir.x, 0, moveDir.z).normalize();
            } else {
                float yaw = player.getYRot();
                return new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
            }
        }

        Vec3 playerPos = player.position();
        Vec3 targetPos = targetPlayer.position();
        Vec3 direction = targetPos.subtract(playerPos);

        return new Vec3(direction.x, 0, direction.z).normalize();
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