package com.souldi.HideAndSeekMod.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TNTCannonItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    // Настройки пушки
    private static final float SHOOT_POWER = 3.0f;      // Увеличенная сила выстрела
    private static final float TNT_FUSE = 80.0f;        // Время до взрыва ТНТ в тиках (4 секунды)

    // Цвета для названия предмета
    private static final int COLOR_RED = 0xFF0000;   // Красный цвет
    private static final int COLOR_WHITE = 0xFFFFFF; // Белый цвет

    // Кэш для отслеживания ТНТ, выпущенных из пушки (для защиты владельца)
    private static final Map<UUID, UUID> OWNER_TNT_MAP = new ConcurrentHashMap<>();

    public TNTCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Создаем ТНТ на позиции игрока
            shootTNT(level, player);

            // Удалено сообщение над хотбаром

            // Возвращаем успешный результат
            return InteractionResultHolder.success(itemStack);
        }

        return InteractionResultHolder.pass(itemStack);
    }

    /**
     * Создает и запускает ТНТ в направлении взгляда игрока
     */
    private void shootTNT(Level level, Player player) {
        // Создаем ТНТ на позиции игрока (немного выше глаз)
        Vec3 playerPos = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0f);

        // Смещаем ТНТ вперед, чтобы оно не взрывалось рядом с игроком
        Vec3 spawnPos = playerPos.add(viewVector.multiply(1.2, 1.2, 1.2));

        // Создаем ТНТ
        PrimedTnt tnt = new PrimedTnt(level, spawnPos.x, spawnPos.y, spawnPos.z, player);

        // Устанавливаем время до взрыва
        tnt.setFuse((int) TNT_FUSE);

        // Помечаем ТНТ как выпущенное из пушки (для защиты владельца)
        CompoundTag data = tnt.getPersistentData();
        data.putUUID("CannonOwnerUUID", player.getUUID());

        // Также сохраняем информацию в карте
        // (для случаев, когда NBT не срабатывает - двойная защита)
        OWNER_TNT_MAP.put(tnt.getUUID(), player.getUUID());

        // Вычисляем вектор скорости ТНТ (направление взгляда игрока + небольшое отклонение вверх)
        Vec3 velocity = viewVector.multiply(SHOOT_POWER, SHOOT_POWER, SHOOT_POWER);

        // Добавляем небольшое случайное отклонение для реалистичности
        double spreadFactor = 0.1;
        velocity = velocity.add(
                (Math.random() - 0.5) * spreadFactor,
                (Math.random()) * spreadFactor, // Слегка вверх
                (Math.random() - 0.5) * spreadFactor
        );

        // Устанавливаем скорость ТНТ
        tnt.setDeltaMovement(velocity);

        // Добавляем ТНТ в мир
        level.addFreshEntity(tnt);

        // Воспроизводим звук выстрела
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TNT_PRIMED,
                SoundSource.PLAYERS,
                1.0F, 1.0F
        );

        // Логируем для отладки
        LOGGER.info("Игрок " + player.getName().getString() + " выстрелил из ТНТ пушки");
    }

    /**
     * Метод для проверки, является ли ТНТ выпущенным из пушки игрока
     * Может использоваться в обработчике событий для предотвращения урона владельцу
     */
    public static boolean isTntOwnedByPlayer(PrimedTnt tnt, UUID playerUUID) {
        // Проверяем сначала в NBT данных (надежный метод)
        CompoundTag data = tnt.getPersistentData();
        if (data.hasUUID("CannonOwnerUUID")) {
            UUID ownerUUID = data.getUUID("CannonOwnerUUID");
            return ownerUUID.equals(playerUUID);
        }

        // Если нет в NBT, проверяем в мапе (запасной метод)
        return OWNER_TNT_MAP.containsKey(tnt.getUUID()) &&
                OWNER_TNT_MAP.get(tnt.getUUID()).equals(playerUUID);
    }

    /**
     * Очищает старые записи о ТНТ
     */
    public static void cleanupOldTntEntries() {
        // Эта функция может быть вызвана периодически для очистки мапы от старых записей
        OWNER_TNT_MAP.clear();
    }

    // Предмет неразрушимый - отключаем урон
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    // Переопределяем getName для создания цветного текста
    @Override
    public Component getName(ItemStack stack) {
        // Создаем цветной текст "ТНТ пушка" с заданными цветами
        MutableComponent coloredText = Component.empty()
                .append(Component.literal("Т").withStyle(Style.EMPTY.withColor(COLOR_RED)))
                .append(Component.literal("Н").withStyle(Style.EMPTY.withColor(COLOR_WHITE)))
                .append(Component.literal("Т ").withStyle(Style.EMPTY.withColor(COLOR_RED)))
                .append(Component.literal("пу").withStyle(Style.EMPTY.withColor(COLOR_RED)))
                .append(Component.literal("ш").withStyle(Style.EMPTY.withColor(COLOR_WHITE)))
                .append(Component.literal("ка").withStyle(Style.EMPTY.withColor(COLOR_RED)));

        return coloredText;
    }

    // Переопределяем appendHoverText для цветного описания
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        // Создаем цветное описание
        MutableComponent coloredDesc = Component.empty()
                .append(Component.literal("Пушка с бесконечным запасом ").withStyle(Style.EMPTY.withColor(COLOR_WHITE)))
                .append(Component.literal("динамитов").withStyle(Style.EMPTY.withColor(COLOR_RED)));

        tooltip.add(coloredDesc);
        // Удалена строка "Не бойся взрывов - собственные ТНТ не нанесут тебе урон!"
        super.appendHoverText(stack, level, tooltip, flag);
    }
}