package com.souldi.HideAndSeekMod.entity;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
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
    private static final double MOVE_SPEED = 0.5; // Базовая скорость движения
    private static final double ACCELERATION = 0.1; // Ускорение при старте
    private static final double DECELERATION = 0.2; // Замедление при приближении к цели
    private static final double MIN_DISTANCE_TO_TARGET = 2.0; // Минимальное расстояние до цели
    private static final double MAX_SPEED = 1.2; // Максимальная скорость движения

    // Для сглаживания движения
    private double currentSpeed = 0.0;
    private boolean isAccelerating = true;
    private int ticksInMotion = 0;

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
        if (this.getPassengers().isEmpty()) {
            if (!this.level.isClientSide()) {
                this.discard();
            }
            return;
        }

        // Увеличиваем счетчик тиков в движении
        ticksInMotion++;

        // Проверяем, активно ли бурение
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
                    // Сохраняем текущую позицию для расчета скорости
                    if (lastPosition == null) {
                        lastPosition = this.position();
                    }

                    // Если бурение активно, получаем целевую позицию из NBT
                    if (tag.contains("LastPosX") && tag.contains("LastPosY") && tag.contains("LastPosZ")) {
                        double initialX = tag.getDouble("LastPosX");
                        double initialY = tag.getDouble("LastPosY");
                        double initialZ = tag.getDouble("LastPosZ");

                        // Получаем целевую позицию - UUID целевого игрока
                        if (tag.hasUUID("TargetPlayerUUID")) {
                            UUID targetUUID = tag.getUUID("TargetPlayerUUID");
                            ServerPlayer targetPlayer = null;

                            // Находим игрока по UUID
                            if (!level.isClientSide) {
                                for (ServerPlayer serverPlayer : ((ServerLevel)level).getServer().getPlayerList().getPlayers()) {
                                    if (serverPlayer.getUUID().equals(targetUUID)) {
                                        targetPlayer = serverPlayer;
                                        break;
                                    }
                                }
                            }

                            // Если нашли целевого игрока, рассчитываем плавное движение к нему
                            if (targetPlayer != null) {
                                Vec3 currentPos = this.position();
                                Vec3 playerTargetPos = targetPlayer.position();
                                targetPos = playerTargetPos;

                                // Рассчитываем направление к игроку
                                Vec3 directionToTarget = playerTargetPos.subtract(currentPos).normalize();
                                moveDirection = directionToTarget;

                                // Рассчитываем расстояние до цели
                                double distanceToTarget = currentPos.distanceTo(playerTargetPos);

                                // Обновляем тег с последней позицией цели для следующего тика
                                tag.putDouble("LastPosX", playerTargetPos.x);
                                tag.putDouble("LastPosY", playerTargetPos.y);
                                tag.putDouble("LastPosZ", playerTargetPos.z);

                                // Плавная остановка при приближении к цели
                                if (distanceToTarget < MIN_DISTANCE_TO_TARGET) {
                                    // Замедляемся и останавливаемся
                                    currentSpeed = Math.max(0, currentSpeed - DECELERATION);

                                    // Если почти остановились и близко к цели, останавливаем бурение
                                    if (currentSpeed < 0.05 && distanceToTarget < 1.5) {
                                        tag.putBoolean("IsActive", false);
                                        currentSpeed = 0;
                                        return;
                                    }
                                }
                                // Плавное ускорение или поддержание скорости
                                else {
                                    // В начале движения - плавное ускорение
                                    if (ticksInMotion < 20) {
                                        currentSpeed = Math.min(MAX_SPEED, currentSpeed + ACCELERATION);
                                    }
                                    // На дальних расстояниях - поддерживаем максимальную скорость
                                    else if (distanceToTarget > 15) {
                                        currentSpeed = MAX_SPEED;
                                    }
                                    // При приближении к цели - начинаем замедляться
                                    else if (distanceToTarget < 10) {
                                        double slowdownFactor = distanceToTarget / 10; // 1.0 на расстоянии 10, 0.5 на расстоянии 5
                                        currentSpeed = Math.max(MOVE_SPEED, MAX_SPEED * slowdownFactor);
                                    }
                                }

                                // Применяем движение с текущей скоростью
                                Vec3 movement = moveDirection.scale(currentSpeed);
                                this.setDeltaMovement(movement);
                                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

                                // Сохраняем позицию для следующего расчета
                                lastPosition = this.position();

                                // Не выполняем обычную физику для лодки в режиме бурения
                                return;
                            }
                        }
                    }
                } else {
                    // Если бурение не активно, сбрасываем скорость и счетчики
                    currentSpeed = 0;
                    ticksInMotion = 0;
                }
            }
        }

        // Сбрасываем параметры движения, если не в режиме бурения
        targetPos = null;
        moveDirection = Vec3.ZERO;
        currentSpeed = 0;
        ticksInMotion = 0;

        // Вызываем стандартную обработку родителя для коллизий и гравитации, если не в режиме бурения
        super.tick();
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
        // Проверяем, активно ли бурение у игрока на сиденье
        if (!this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            boolean holdingDrill = mainHand.getItem() instanceof SeekerDrillItem ||
                    offHand.getItem() instanceof SeekerDrillItem;

            if (holdingDrill) {
                // Проверяем, активно ли бурение
                CompoundTag tag = mainHand.getItem() instanceof SeekerDrillItem ?
                        mainHand.getOrCreateTag() : offHand.getOrCreateTag();

                if (tag.getBoolean("IsActive")) {
                    // Если бурение активно, отключаем гравитацию
                    return true;
                }
            }
        }

        // По умолчанию гравитация включена
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