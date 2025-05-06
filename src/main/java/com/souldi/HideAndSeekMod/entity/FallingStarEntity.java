package com.souldi.HideAndSeekMod.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class FallingStarEntity extends Entity {
    private static final Logger LOGGER = LogManager.getLogger();

    private int lifetime = 0;
    private int maxLifetime = 400; // Увеличенное время жизни (20 секунд)
    private Vec3 targetPos; // Позиция цели
    private double targetY; // Высота цели (игрока)
    private boolean hasImpacted = false;
    private double fallSpeed = 0.7D; // Увеличенная скорость падения

    public FallingStarEntity(EntityType<? extends FallingStarEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // Отключаем физику столкновений
    }

    public FallingStarEntity(Level level, double x, double y, double z, Vec3 targetPos) {
        this(ModEntities.FALLING_STAR.get(), level);
        this.setPos(x, y, z);
        this.targetPos = targetPos;
        this.targetY = targetPos.y; // Запоминаем высоту цели

        // Рассчитываем вектор направления к цели
        Vec3 direction = targetPos.subtract(x, y, z).normalize();

        // Устанавливаем скорость движения, преимущественно вниз
        this.setDeltaMovement(
                direction.x * fallSpeed * 0.5, // Снижаем горизонтальную скорость
                -fallSpeed, // Увеличиваем вертикальную скорость (вниз)
                direction.z * fallSpeed * 0.5  // Снижаем горизонтальную скорость
        );

        LOGGER.info("Created FallingStarEntity at " + x + ", " + y + ", " + z + " targeting " + targetPos);
    }

    @Override
    protected void defineSynchedData() {
        // Нет синхронизируемых данных
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TargetX") && tag.contains("TargetY") && tag.contains("TargetZ")) {
            double tx = tag.getDouble("TargetX");
            double ty = tag.getDouble("TargetY");
            double tz = tag.getDouble("TargetZ");
            this.targetPos = new Vec3(tx, ty, tz);
            this.targetY = ty;
        }

        this.lifetime = tag.getInt("Lifetime");
        this.maxLifetime = tag.getInt("MaxLifetime");
        this.hasImpacted = tag.getBoolean("HasImpacted");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.targetPos != null) {
            tag.putDouble("TargetX", this.targetPos.x);
            tag.putDouble("TargetY", this.targetPos.y);
            tag.putDouble("TargetZ", this.targetPos.z);
        }

        tag.putInt("Lifetime", this.lifetime);
        tag.putInt("MaxLifetime", this.maxLifetime);
        tag.putBoolean("HasImpacted", this.hasImpacted);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void tick() {
        super.tick();

        // Увеличиваем время жизни
        this.lifetime++;

        // Удаляем сущность если время жизни превышено
        if (this.lifetime > this.maxLifetime) {
            this.discard();
            return;
        }

        // Обрабатываем движение звезды только если она не столкнулась
        if (!this.hasImpacted) {
            // Запоминаем предыдущую позицию для частиц
            double prevX = this.getX();
            double prevY = this.getY();
            double prevZ = this.getZ();

            // Перемещаем сущность
            this.move(MoverType.SELF, this.getDeltaMovement());

            // Добавляем частицы следа
            for (int i = 0; i < 5; i++) {
                double trailX = prevX + (this.getX() - prevX) * (i / 5.0);
                double trailY = prevY + (this.getY() - prevY) * (i / 5.0);
                double trailZ = prevZ + (this.getZ() - prevZ) * (i / 5.0);

                this.level.addParticle(
                        ParticleTypes.FLAME,
                        trailX, trailY, trailZ,
                        0.0, 0.0, 0.0
                );
            }

            // Проверяем достижение высоты игрока
            if (this.getY() <= this.targetY + 1.0) {
                LOGGER.info("Star reached target height: " + this.getY() + " <= " + (this.targetY + 1.0));
                this.impact();
                return;
            }

            // Проверяем столкновение с блоками
            BlockPos blockPos = new BlockPos(this.getX(), this.getY(), this.getZ());
            BlockState blockState = this.level.getBlockState(blockPos);

            if (!blockState.isAir()) {
                LOGGER.info("Star hit a block: " + blockState.getBlock().getName().getString());
                // Разрушаем блок при столкновении
                destroyBlocks();
                // Если это не последний блок до цели, просто двигаемся дальше
                if (this.getY() > this.targetY + 3.0) {
                    return;
                }
                this.impact();
            }
        }
    }

    /**
     * Разрушает блоки в форме звезды вокруг падающей звезды
     */
    private void destroyBlocks() {
        int radius = 2; // Радиус разрушения
        List<BlockPos> blocksToDestroy = new ArrayList<>();

        BlockPos center = new BlockPos(this.getX(), this.getY(), this.getZ());

        // Собираем блоки в форме крестообразной звезды
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Крест (горизонтальные линии)
                if (x == 0 || z == 0) {
                    blocksToDestroy.add(center.offset(x, 0, z));

                    // Дополнительные блоки для более "звездной" формы
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                        blocksToDestroy.add(center.offset(x, 0, z));
                    }
                }
            }
        }

        // Разрушаем блоки
        for (BlockPos pos : blocksToDestroy) {
            BlockState blockState = this.level.getBlockState(pos);
            if (!blockState.isAir() && blockState.getDestroySpeed(level, pos) >= 0
                    && level.getBlockEntity(pos) == null) {
                level.destroyBlock(pos, false);

                // Добавляем эффект частиц
                level.addParticle(
                        ParticleTypes.EXPLOSION,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        0.0, 0.0, 0.0
                );
            }
        }
    }

    private void impact() {
        if (!level.isClientSide && !this.hasImpacted) {
            this.hasImpacted = true;

            // Эффект взрыва (без урона)
            level.explode(
                    this,
                    this.getX(), this.getY(), this.getZ(),
                    3.0F, // Больший радиус взрыва для эффекта
                    false, // Без огня
                    Explosion.BlockInteraction.NONE // Не разрушаем блоки взрывом (уже разрушили выше)
            );

            // Добавляем больше частиц для эффекта
            for (int i = 0; i < 30; i++) {
                double offsetX = this.random.nextGaussian() * 0.5;
                double offsetY = this.random.nextGaussian() * 0.5;
                double offsetZ = this.random.nextGaussian() * 0.5;

                level.addParticle(
                        ParticleTypes.EXPLOSION,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        0.0, 0.0, 0.0
                );
            }

            // Удаляем сущность
            this.discard();
        }
    }

    // Переопределяем bounding box для лучшего рендеринга
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(2.0);
    }
}