package com.souldi.HideAndSeekMod.event;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.item.TNTCannonItem;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent; // Исправленный импорт
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID)
public class TNTCannonEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    // Счетчик тиков для периодической очистки
    private static int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 6000; // 5 минут (20 тиков * 60 сек * 5)

    /**
     * Обработчик урона, который предотвращает урон игроку от его собственного ТНТ из пушки
     * Высокий приоритет (HIGHEST), чтобы обработать событие раньше других обработчиков
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        // Проверяем, что получатель урона - игрок
        if (event.getEntity() instanceof Player player) {
            // Проверяем, что источник урона - взрыв
            if (event.getSource().isExplosion()) {
                // Проверяем, что источник взрыва - ТНТ
                if (event.getSource().getDirectEntity() instanceof PrimedTnt tnt) {
                    // Проверяем, принадлежит ли это ТНТ игроку
                    if (TNTCannonItem.isTntOwnedByPlayer(tnt, player.getUUID())) {
                        // Отменяем урон, так как это ТНТ выпущено из пушки игрока
                        event.setCanceled(true);
                        LOGGER.info("Отменен урон от ТНТ из пушки для игрока " + player.getName().getString());
                    }
                } else if (event.getSource().getDirectEntity() == null && event.getSource().getEntity() instanceof PrimedTnt tnt) {
                    // Дополнительная проверка на случай, если DirectEntity равен null
                    if (TNTCannonItem.isTntOwnedByPlayer(tnt, player.getUUID())) {
                        event.setCanceled(true);
                        LOGGER.info("Отменен урон от ТНТ (вариант 2) из пушки для игрока " + player.getName().getString());
                    }
                }
            }
        }
    }

    /**
     * Дополнительная регистрация для ТНТ из пушки
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {  // Исправленное имя метода
        if (event.getEntity() instanceof PrimedTnt) {
            // Здесь можно добавить дополнительную логику при создании ТНТ
            LOGGER.debug("ТНТ добавлено в мир: " + event.getEntity().getUUID());
        }
    }

    /**
     * Периодически очищаем старые записи о ТНТ для предотвращения утечек памяти
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            cleanupCounter++;

            if (cleanupCounter >= CLEANUP_INTERVAL) {
                cleanupCounter = 0;
                TNTCannonItem.cleanupOldTntEntries();
            }
        }
    }
}