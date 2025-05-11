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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FallingStarEntity extends Entity {
    private static final Logger LOGGER = LogManager.getLogger();

    private int lifetime = 0;
    private int maxLifetime = 400; // 20 секунд при 20 тиках в секунду
    private Vec3 targetPos; // Позиция цели
    private double targetY; // Высота цели (игрока)
    private boolean hasImpacted = false;
    private double fallSpeed = 0.7D; // Скорость падения
    private UUID targetPlayerUUID; // UUID игрока-цели

    // Смещение по Y для остановки звезды выше уровня игрока
    private static final int STOP_HEIGHT_ABOVE_PLAYER = 3;

    // Переменные для отслеживания пути движения
    private List<BlockPos> destroyedBlocks = new ArrayList<>();
    private int blockDestroyTick = 0;
    private static final int DESTROY_FREQUENCY = 1; // Проверка блоков каждый тик

    // Параметры звезды
    private static final int STAR_POINTS = 5; // 5-конечная звезда
    private static final int MAIN_RADIUS = 8; // Большой радиус (лучи звезды)
    private static final int INNER_RADIUS = 3; // Внутренний радиус (между лучами)
    private static final int VERTICAL_RANGE = 2; // Вертикальный диапазон звезды

    public FallingStarEntity(EntityType<? extends FallingStarEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // Отключаем физику столкновений

        // Установим стандартное движение вниз (на случай, если targetPos не инициализирован)
        this.setDeltaMovement(0, -fallSpeed, 0);
    }

    public FallingStarEntity(Level level, double x, double y, double z, Vec3 targetPos, UUID targetPlayerUUID) {
        this(ModEntities.FALLING_STAR.get(), level);

        // Точно в позицию игрока
        this.setPos(x, y, z);
        this.targetPos = targetPos;

        // Устанавливаем высоту цели на STOP_HEIGHT_ABOVE_PLAYER блоков выше игрока
        this.targetY = targetPos.y + STOP_HEIGHT_ABOVE_PLAYER;
        this.targetPlayerUUID = targetPlayerUUID; // Запоминаем UUID игрока-цели

        // Рассчитываем вектор направления точно к цели
        Vec3 direction = targetPos.subtract(x, y, z).normalize();

        // Устанавливаем скорость движения
        this.setDeltaMovement(
                direction.x * fallSpeed,
                direction.y * fallSpeed,
                direction.z * fallSpeed
        );

        LOGGER.info("Created FallingStarEntity at " + x + ", " + y + ", " + z + " targeting " + targetPos + " for player " + targetPlayerUUID);
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
        } else {
            // Если информация о цели отсутствует, устанавливаем движение вниз
            this.targetPos = null;
            this.targetY = this.getY() - 10.0; // Просто 10 блоков ниже текущей позиции
            this.setDeltaMovement(0, -fallSpeed, 0);
        }

        this.lifetime = tag.getInt("Lifetime");
        this.maxLifetime = tag.getInt("MaxLifetime");
        this.hasImpacted = tag.getBoolean("HasImpacted");

        if (tag.hasUUID("TargetPlayerUUID")) {
            this.targetPlayerUUID = tag.getUUID("TargetPlayerUUID");
        }
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

        if (this.targetPlayerUUID != null) {
            tag.putUUID("TargetPlayerUUID", this.targetPlayerUUID);
        }
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
            // Получаем текущую позицию перед движением
            BlockPos currentBlockPos = new BlockPos(this.getX(), this.getY(), this.getZ());

            // Перемещаем сущность
            this.move(MoverType.SELF, this.getDeltaMovement());

            // Проверяем достижение высоты цели
            if (this.getY() <= this.targetY) {
                LOGGER.info("Star reached target height: " + this.getY() + " <= " + this.targetY);
                impact();
                return;
            }

            // Разрушаем блоки по пути каждые DESTROY_FREQUENCY тиков
            blockDestroyTick++;
            if (blockDestroyTick >= DESTROY_FREQUENCY) {
                blockDestroyTick = 0;

                // Разрушаем блоки в форме 5-конечной звезды по пути движения
                destroyBlocksInStarPattern(currentBlockPos);

                // Добавляем частицы для визуального эффекта
                if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            this.getX(), this.getY(), this.getZ(),
                            10, 0.5, 0.5, 0.5, 0.01);
                }
            }

            // Проверяем столкновение с блоками
            BlockPos blockPos = new BlockPos(this.getX(), this.getY(), this.getZ());
            BlockState blockState = this.level.getBlockState(blockPos);

            if (!blockState.isAir()) {
                LOGGER.info("Star hit a block: " + blockState.getBlock().getName().getString());

                // Получаем высоту игрока
                Player targetPlayer = getTargetPlayer();
                if (targetPlayer != null) {
                    double playerY = targetPlayer.getY() + STOP_HEIGHT_ABOVE_PLAYER;

                    // Если звезда опустилась ниже заданной высоты над игроком, останавливаем её
                    if (this.getY() <= playerY) {
                        LOGGER.info("Star stopped above player: " + this.getY() + " <= " + playerY);
                        impact();
                        return;
                    }
                }

                // Разрушаем блок при столкновении
                destroyBlocksInStarPattern(blockPos);

                // Проверяем, находимся ли мы еще далеко от цели
                double distanceToTarget = (targetPos != null)
                        ? new Vec3(this.getX(), this.getY(), this.getZ()).distanceTo(targetPos)
                        : 0; // Если targetPos == null, считаем что мы близко к цели

                if (targetPos != null && distanceToTarget > 3.0) {
                    // Если далеко от цели, пересчитываем вектор движения для обхода препятствия
                    Vec3 newDirection = targetPos.subtract(this.getX(), this.getY(), this.getZ()).normalize();
                    this.setDeltaMovement(
                            newDirection.x * fallSpeed,
                            newDirection.y * fallSpeed,
                            newDirection.z * fallSpeed
                    );
                    return;
                }
                impact();
            }
        }
    }

    /**
     * Находит игрока-цель по UUID
     */
    private Player getTargetPlayer() {
        if (targetPlayerUUID != null) {
            for (Player player : level.players()) {
                if (player.getUUID().equals(targetPlayerUUID)) {
                    return player;
                }
            }
        }
        return null;
    }

    /**
     * Разрушает блоки в форме пятиконечной звезды вокруг указанной позиции
     */
    private void destroyBlocksInStarPattern(BlockPos center) {
        // Создаем список позиций для разрушения
        List<BlockPos> blocksToDestroy = new ArrayList<>();

        // Находим и добавляем блоки в форме звезды
        for (int y = -VERTICAL_RANGE; y <= VERTICAL_RANGE; y++) {
            for (int x = -MAIN_RADIUS; x <= MAIN_RADIUS; x++) {
                for (int z = -MAIN_RADIUS; z <= MAIN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    // Пропускаем центр (добавляем отдельно)
                    if (x == 0 && z == 0 && y == 0) {
                        blocksToDestroy.add(pos);
                        continue;
                    }

                    // Вычисляем радиальное расстояние от центра
                    double distance = Math.sqrt(x * x + z * z);

                    // Если точка за пределами максимального радиуса, пропускаем
                    if (distance > MAIN_RADIUS) continue;

                    // Вычисляем угол в полярных координатах
                    double angle = Math.atan2(z, x);
                    // Нормализуем угол в положительный диапазон [0, 2π]
                    if (angle < 0) angle += 2 * Math.PI;

                    // Вычисляем нормализованный угол в секторе одного луча звезды
                    double sectorAngle = 2 * Math.PI / STAR_POINTS;
                    double normalizedAngle = angle % sectorAngle;

                    // Вычисляем пороговое расстояние для данного угла
                    double threshold;
                    if (normalizedAngle < sectorAngle / 2) {
                        // Первая половина сектора - интерполируем от внешнего к внутреннему
                        threshold = MAIN_RADIUS - (MAIN_RADIUS - INNER_RADIUS) * (normalizedAngle * 2 / sectorAngle);
                    } else {
                        // Вторая половина сектора - интерполируем от внутреннего к внешнему
                        threshold = INNER_RADIUS + (MAIN_RADIUS - INNER_RADIUS) * ((normalizedAngle - sectorAngle/2) * 2 / sectorAngle);
                    }

                    // Добавляем все блоки внутри звезды
                    if (distance <= threshold) {
                        blocksToDestroy.add(pos);
                    }
                }
            }
        }

        // Разрушаем блоки
        for (BlockPos pos : blocksToDestroy) {
            // Пропускаем уже разрушенные блоки
            if (destroyedBlocks.contains(pos)) {
                continue;
            }

            breakBlockSafely(pos);
        }
    }

    private void impact() {
        if (!level.isClientSide && !this.hasImpacted) {
            this.hasImpacted = true;

            // Используем тот же метод разрушения для финального эффекта
            destroyBlocksInStarPattern(new BlockPos(this.getX(), this.getY(), this.getZ()));

            // Никакого взрыва, просто небольшой визуальный эффект
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0);

                // Добавляем больше эффектных частиц при финальном столкновении
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        this.getX(), this.getY(), this.getZ(),
                        30, 2.0, 0.5, 2.0, 0.1);

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        50, 2.0, 0.5, 2.0, 0.05);
            }

            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F, 0.5F);

            // Удаляем сущность
            this.discard();
        }
    }

    /**
     * Безопасно разрушает блок с проверками
     */
    private void breakBlockSafely(BlockPos pos) {
        // Предотвращаем двойное разрушение
        if (destroyedBlocks.contains(pos)) {
            return;
        }

        // Проверяем, можно ли разрушить блок
        BlockState blockState = this.level.getBlockState(pos);
        if (!blockState.isAir() &&
                blockState.getDestroySpeed(level, pos) >= 0 &&
                level.getBlockEntity(pos) == null) {
            // Удалена проверка !blockState.is(net.minecraftforge.common.Tags.Blocks.ORES)

            // Отмечаем как разрушенный
            destroyedBlocks.add(pos);

            // Разрушаем без дропа
            level.destroyBlock(pos, false);

            // Добавляем случайные частицы для большего эффекта
            if (!level.isClientSide && level instanceof ServerLevel serverLevel && random.nextFloat() < 0.1) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    // Переопределяем boundingbox для лучшего рендеринга
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(2.0);
    }
}