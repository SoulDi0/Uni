package com.souldi.HideAndSeekMod.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.ClipContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Vector3f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaserPointerItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    // Кэш последних целевых блоков для игроков (для подсветки)
    private static final Map<UUID, BlockPos> PLAYER_TARGET_BLOCKS = new HashMap<>();

    // Тиковая система для создания визуальных эффектов
    private static int tickCounter = 0;

    // Цвета для названия предмета
    private static final int COLOR_BLUE = 0x0066FF;   // Синий цвет
    private static final int COLOR_ORANGE = 0xFF8800; // Оранжевый цвет

    public LaserPointerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Получаем точку, куда смотрит игрок с предельной дальностью
            HitResult hitResult = rayTraceExtended(level, player, ClipContext.Fluid.NONE, 10000.0);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockPos targetPos = blockHit.getBlockPos();
                BlockState targetBlock = level.getBlockState(targetPos);

                // Проверяем, можно ли телепортироваться на этот блок (не должен быть воздухом)
                if (!targetBlock.isAir()) {
                    // Находим безопасную позицию для телепортации (на блок выше цели)
                    BlockPos safePos = targetPos.above();
                    BlockState aboveBlock = level.getBlockState(safePos);
                    BlockState aboveAboveBlock = level.getBlockState(safePos.above());

                    // Проверяем, достаточно ли места для игрока (два блока пустого пространства)
                    if (aboveBlock.isAir() && aboveAboveBlock.isAir()) {
                        // Телепортируем игрока
                        if (player instanceof ServerPlayer) {
                            ServerPlayer serverPlayer = (ServerPlayer) player;

                            // Сохраняем исходные координаты
                            Vec3 startPos = player.position();

                            // Телепортируем
                            serverPlayer.teleportTo(
                                    safePos.getX() + 0.5,
                                    safePos.getY(),
                                    safePos.getZ() + 0.5
                            );

                            // Создаем эффекты частиц и звуковых эффектов
                            if (level instanceof ServerLevel serverLevel) {
                                // Эффект в начальной позиции
                                serverLevel.sendParticles(
                                        ParticleTypes.PORTAL,
                                        startPos.x, startPos.y + 1, startPos.z,
                                        30, 0.5, 1.0, 0.5, 0.1
                                );

                                // Эффект в конечной позиции
                                serverLevel.sendParticles(
                                        ParticleTypes.PORTAL,
                                        safePos.getX() + 0.5, safePos.getY() + 1, safePos.getZ() + 0.5,
                                        30, 0.5, 1.0, 0.5, 0.1
                                );

                                // Звук
                                level.playSound(
                                        null,
                                        safePos,
                                        SoundEvents.ENDERMAN_TELEPORT,
                                        SoundSource.PLAYERS,
                                        1.0F, 1.0F
                                );
                            }

                            // Логируем для отладки
                            LOGGER.info("Телепортация игрока " + player.getName().getString() +
                                    " на координаты: " + safePos.getX() + ", " +
                                    safePos.getY() + ", " + safePos.getZ() +
                                    ", расстояние: " + startPos.distanceTo(new Vec3(safePos.getX(), safePos.getY(), safePos.getZ())));

                            // Не уменьшаем прочность предмета

                            return InteractionResultHolder.success(itemStack);
                        }
                    } else {
                        // Сообщение о недостатке места
                        player.displayClientMessage(
                                Component.literal("Недостаточно места для телепортации!"),
                                true
                        );
                    }
                }
            }
        }

        return InteractionResultHolder.fail(itemStack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Создаем визуальный эффект подсветки только когда предмет выбран
        if (level.isClientSide || !isSelected || !(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;

        // Инкрементируем счетчик тиков
        tickCounter++;

        // Обновляем эффекты каждые 2 тика для более плавной анимации
        if (tickCounter % 2 == 0) {
            // Получаем точку, куда смотрит игрок
            HitResult hitResult = rayTraceExtended(level, player, ClipContext.Fluid.NONE, 1000.0);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockPos targetPos = blockHit.getBlockPos();

                // Проверяем, подходит ли блок для телепортации
                BlockState targetBlock = level.getBlockState(targetPos);
                BlockPos safePos = targetPos.above();
                BlockState aboveBlock = level.getBlockState(safePos);
                BlockState aboveAboveBlock = level.getBlockState(safePos.above());

                // Определяем цвет подсветки в зависимости от возможности телепортации
                Vector3f highlightColor;
                if (!targetBlock.isAir() && aboveBlock.isAir() && aboveAboveBlock.isAir()) {
                    // Зеленый, если можно телепортироваться
                    highlightColor = new Vector3f(0.3F, 1.0F, 0.3F);
                } else {
                    // Красный, если нельзя телепортироваться
                    highlightColor = new Vector3f(1.0F, 0.3F, 0.3F);
                }

                if (level instanceof ServerLevel serverLevel) {
                    // Подсвечиваем целевой блок с эффектом свечения, видимым через блоки
                    createCompleteOutlineEffect(serverLevel, targetPos, highlightColor);
                }
            }
        }
    }

    /**
     * Создает полный эффект подсветки куба, видимый через блоки
     */
    private void createCompleteOutlineEffect(ServerLevel level, BlockPos pos, Vector3f color) {
        // Размер частиц (маленькие)
        float size = 0.5f;

        // Создаем частицу для контура
        DustParticleOptions outlineParticle = new DustParticleOptions(color, size);

        // Количество частиц на ребро
        int particlesPerEdge = 10;

        // Количество частиц на грань (для заполнения)
        int particlesPerFace = 5;

        // Анимация пульсации для точек
        float pulse = (float)(0.5 + 0.5 * Math.sin(tickCounter * 0.1));

        // Смещение для красивого визуального эффекта
        float offset = 0.05f + pulse * 0.02f;

        // 1. Создаем контур ребер куба
        for (int i = 0; i < particlesPerEdge; i++) {
            float t = (float)i / (particlesPerEdge - 1); // От 0 до 1

            // Все 12 ребер куба
            // Нижний прямоугольник
            sendGlowingParticle(level, pos.getX() + t, pos.getY() - offset, pos.getZ() - offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() + t, pos.getY() - offset, pos.getZ() + 1 + offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() - offset, pos.getY() - offset, pos.getZ() + t, outlineParticle);
            sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() - offset, pos.getZ() + t, outlineParticle);

            // Верхний прямоугольник
            sendGlowingParticle(level, pos.getX() + t, pos.getY() + 1 + offset, pos.getZ() - offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() + t, pos.getY() + 1 + offset, pos.getZ() + 1 + offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() - offset, pos.getY() + 1 + offset, pos.getZ() + t, outlineParticle);
            sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() + 1 + offset, pos.getZ() + t, outlineParticle);

            // Вертикальные ребра
            sendGlowingParticle(level, pos.getX() - offset, pos.getY() + t, pos.getZ() - offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() - offset, pos.getY() + t, pos.getZ() + 1 + offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() + t, pos.getZ() - offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() + t, pos.getZ() + 1 + offset, outlineParticle);
        }

        // 2. Добавляем внутренние частицы для лучшей видимости через блоки
        // Немного меньший размер для внутренних частиц
        DustParticleOptions innerParticle = new DustParticleOptions(color, size * 0.8f);

        // Добавляем частицы внутри граней для лучшей видимости сквозь блоки
        for (int ix = 1; ix < particlesPerFace; ix++) {
            for (int iz = 1; iz < particlesPerFace; iz++) {
                float tx = (float)ix / particlesPerFace;
                float tz = (float)iz / particlesPerFace;

                // Нижняя грань
                sendGlowingParticle(level, pos.getX() + tx, pos.getY() - offset, pos.getZ() + tz, innerParticle);

                // Верхняя грань
                sendGlowingParticle(level, pos.getX() + tx, pos.getY() + 1 + offset, pos.getZ() + tz, innerParticle);
            }
        }

        // Боковые грани
        for (int ix = 1; ix < particlesPerFace; ix++) {
            for (int iy = 1; iy < particlesPerFace; iy++) {
                float tx = (float)ix / particlesPerFace;
                float ty = (float)iy / particlesPerFace;

                // Грань Z-
                sendGlowingParticle(level, pos.getX() + tx, pos.getY() + ty, pos.getZ() - offset, innerParticle);

                // Грань Z+
                sendGlowingParticle(level, pos.getX() + tx, pos.getY() + ty, pos.getZ() + 1 + offset, innerParticle);
            }
        }

        // Боковые грани X
        for (int iz = 1; iz < particlesPerFace; iz++) {
            for (int iy = 1; iy < particlesPerFace; iy++) {
                float tz = (float)iz / particlesPerFace;
                float ty = (float)iy / particlesPerFace;

                // Грань X-
                sendGlowingParticle(level, pos.getX() - offset, pos.getY() + ty, pos.getZ() + tz, innerParticle);

                // Грань X+
                sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() + ty, pos.getZ() + tz, innerParticle);
            }
        }

        // 3. Небольшое свечение в центре куба для усиления эффекта
        DustParticleOptions centerParticle = new DustParticleOptions(color, size * 0.5f);
        sendGlowingParticle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, centerParticle);
    }

    /**
     * Отправляет частицу с эффектом свечения всем игрокам в радиусе
     * с особыми параметрами для видимости через блоки
     */
    private void sendGlowingParticle(ServerLevel level, double x, double y, double z, DustParticleOptions particle) {
        // Создаем специальный пакет для частиц с параметрами для видимости через блоки
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                particle,
                true,  // longDistance = true - важно для видимости на дальних расстояниях
                x, y, z,
                0, 0, 0,  // скорость
                0,        // одиночная частица
                0         // дополнительные данные
        );

        // Отправляем всем игрокам в мире
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            // Проверяем, что игрок в том же измерении
            if (player.level.dimension() == level.dimension()) {
                // Отправляем пакет игроку без проверки расстояния
                player.connection.send(packet);
            }
        }
    }

    /**
     * Расширенная версия функции определения, куда смотрит игрок,
     * с возможностью указать максимальную дистанцию
     */
    private HitResult rayTraceExtended(Level level, Player player, ClipContext.Fluid fluidMode, double maxDistance) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVector.x * maxDistance, viewVector.y * maxDistance, viewVector.z * maxDistance);

        ClipContext context = new ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                fluidMode,
                player
        );

        return level.clip(context);
    }

    // Предмет неразрушимый - отключаем урон
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    // Переопределяем getName для создания цветного текста
    @Override
    public Component getName(ItemStack stack) {
        // Создаем цветной текст "Портативный телепорт" с заданными цветами
        MutableComponent coloredText = Component.empty()
                .append(Component.literal("Портативный").withStyle(Style.EMPTY.withColor(COLOR_BLUE)))
                .append(Component.literal(" телепорт").withStyle(Style.EMPTY.withColor(COLOR_ORANGE)));

        return coloredText;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        // Удаляем первую подсказку, так как она дублирует название
        // tooltip.add(Component.translatable("item.hide_and_seek.laser_pointer.tooltip.1"));

        tooltip.add(Component.translatable("item.hide_and_seek.laser_pointer.tooltip.2"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}