package com.souldi.HideAndSeekMod.entity;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class DrillSeatEntity extends Boat {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HideAndSeekMod.MOD_ID);

    public static final RegistryObject<EntityType<DrillSeatEntity>> DRILL_SEAT =
            ENTITY_TYPES.register("drill_seat",
                    () -> EntityType.Builder.<DrillSeatEntity>of(DrillSeatEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(HideAndSeekMod.MOD_ID + ":drill_seat"));

    // Поля для плавного движения
    private Vec3 targetPos = null;
    private Vec3 moveDirection = Vec3.ZERO;
    private Vec3 lastPosition = null;
    private Vec3 velocity = Vec3.ZERO;
    private static final double MOVE_SPEED = 0.5;
    private static final double ACCELERATION = 0.1;
    private static final double DECELERATION = 0.2;
    private static final double MIN_DISTANCE_TO_TARGET = 2.0;
    private static final double MAX_SPEED = 1.2;

    private double currentSpeed = 0.0;
    private boolean isAccelerating = true;
    private int ticksInMotion = 0;

    private UUID targetPlayerUUID = null;
    private ServerPlayer cachedTargetPlayer = null;
    private int targetPlayerUpdateTicks = 0;

    private float originalYHeadRot = 0.0f;
    private float originalXRot = 0.0f;

    public DrillSeatEntity(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = false;
    }

    public DrillSeatEntity(Level level, double x, double y, double z) {
        this(DRILL_SEAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.lastPosition = new Vec3(x, y, z);
    }

    @Override
    public void tick() {
        try {
            if (this.getPassengers().isEmpty()) {
                if (!this.level.isClientSide()) {
                    this.discard();
                }
                return;
            }

            ticksInMotion++;

            targetPlayerUpdateTicks++;

            Entity passenger = this.getPassengers().get(0);
            if (passenger instanceof Player player) {
                ItemStack drillItem = null;

                if (player.getMainHandItem().getItem() instanceof SeekerDrillItem) {
                    drillItem = player.getMainHandItem();
                } else if (player.getOffhandItem().getItem() instanceof SeekerDrillItem) {
                    drillItem = player.getOffhandItem();
                }

                if (drillItem != null) {
                    CompoundTag tag = drillItem.getOrCreateTag();
                    if (tag.getBoolean("IsActive")) {

                        if (ticksInMotion == 1) {
                            originalYHeadRot = player.getYHeadRot();
                            originalXRot = player.getXRot();
                        }


                        if (lastPosition == null) {
                            lastPosition = this.position();
                        }


                        if (tag.contains("LastPosX") && tag.contains("LastPosY") && tag.contains("LastPosZ")) {
                            double lastPosX = tag.getDouble("LastPosX");
                            double lastPosY = tag.getDouble("LastPosY");
                            double lastPosZ = tag.getDouble("LastPosZ");


                            if (tag.hasUUID("TargetPlayerUUID")) {
                                UUID targetUUID = tag.getUUID("TargetPlayerUUID");


                                if (targetPlayerUUID == null || !targetPlayerUUID.equals(targetUUID)) {
                                    targetPlayerUUID = targetUUID;
                                    cachedTargetPlayer = null;
                                }

                                ServerPlayer targetPlayer = null;

                                if (cachedTargetPlayer == null || targetPlayerUpdateTicks >= 2) {
                                    if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                                        for (ServerPlayer serverPlayer : serverLevel.getServer().getPlayerList().getPlayers()) {
                                            if (serverPlayer.getUUID().equals(targetUUID)) {
                                                targetPlayer = serverPlayer;
                                                cachedTargetPlayer = serverPlayer;
                                                targetPlayerUpdateTicks = 0;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    targetPlayer = cachedTargetPlayer;
                                }

                                if (targetPlayer != null) {
                                    Vec3 currentPos = this.position();
                                    Vec3 playerTargetPos = targetPlayer.position();
                                    targetPos = playerTargetPos;

                                    Vec3 directionToTarget = playerTargetPos.subtract(currentPos).normalize();
                                    moveDirection = directionToTarget;

                                    double distanceToTarget = currentPos.distanceTo(playerTargetPos);

                                    tag.putDouble("LastPosX", playerTargetPos.x);
                                    tag.putDouble("LastPosY", playerTargetPos.y);
                                    tag.putDouble("LastPosZ", playerTargetPos.z);

                                    updatePlayerBodyOrientation(player, directionToTarget);

                                    if (distanceToTarget < MIN_DISTANCE_TO_TARGET) {
                                        currentSpeed = Math.max(0, currentSpeed - DECELERATION);

                                        if (currentSpeed < 0.05 && distanceToTarget < 1.5) {
                                            tag.putBoolean("IsActive", false);
                                            currentSpeed = 0;
                                            return;
                                        }
                                    }
                                    else {
                                        if (ticksInMotion < 20) {
                                            currentSpeed = Math.min(MAX_SPEED, currentSpeed + ACCELERATION);
                                        }
                                        else if (distanceToTarget > 15) {
                                            currentSpeed = MAX_SPEED;
                                        }
                                        else if (distanceToTarget < 10) {
                                            double slowdownFactor = distanceToTarget / 10;
                                            currentSpeed = Math.max(MOVE_SPEED, MAX_SPEED * slowdownFactor);
                                        }
                                    }

                                    Vec3 movement = moveDirection.scale(currentSpeed);
                                    this.setDeltaMovement(movement);
                                    this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

                                    lastPosition = this.position();

                                    return;
                                }
                            }
                        }
                    } else {
                        currentSpeed = 0;
                        ticksInMotion = 0;
                        targetPlayerUUID = null;
                        cachedTargetPlayer = null;
                    }
                }
            }

            targetPos = null;
            moveDirection = Vec3.ZERO;
            currentSpeed = 0;
            ticksInMotion = 0;
            targetPlayerUUID = null;
            cachedTargetPlayer = null;

            super.tick();
        } catch (Exception e) {
            LOGGER.error("Ошибка в DrillSeatEntity.tick(): " + e.getMessage(), e);

            targetPos = null;
            moveDirection = Vec3.ZERO;
            currentSpeed = 0;
            ticksInMotion = 0;
            targetPlayerUUID = null;
            cachedTargetPlayer = null;

            super.tick();
        }
    }

    private void updatePlayerBodyOrientation(Player player, Vec3 directionToTarget) {
        try {
            float targetYaw = (float) Math.toDegrees(Math.atan2(-directionToTarget.x, directionToTarget.z));

            this.setYRot(targetYaw);

            player.setYRot(targetYaw);

            player.setXRot(90.0F);

            player.setYBodyRot(targetYaw);

            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                CompoundTag playerData = serverPlayer.getPersistentData();
                CompoundTag drillData = new CompoundTag();

                drillData.putBoolean("IsDrilling", true);
                drillData.putFloat("BodyYaw", targetYaw);
                drillData.putBoolean("AllowHeadMovement", true);

                playerData.put("DrillData", drillData);
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка в DrillSeatEntity.updatePlayerBodyOrientation(): " + e.getMessage(), e);
        }
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    public Vec3 getMoveDirection() {
        return moveDirection;
    }

    public UUID getTargetPlayerUUID() {
        return targetPlayerUUID;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {

        if (!this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            boolean holdingDrill = mainHand.getItem() instanceof SeekerDrillItem ||
                    offHand.getItem() instanceof SeekerDrillItem;

            if (holdingDrill) {

                CompoundTag tag = mainHand.getItem() instanceof SeekerDrillItem ?
                        mainHand.getOrCreateTag() : offHand.getOrCreateTag();

                if (tag.getBoolean("IsActive")) {

                    return true;
                }
            }
        }


        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 1;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}