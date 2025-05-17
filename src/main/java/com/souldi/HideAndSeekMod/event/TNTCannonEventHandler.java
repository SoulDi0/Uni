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

    private static int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 6000; // 5 минут (20 тиков * 60 сек * 5)

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getSource().isExplosion()) {
                if (event.getSource().getDirectEntity() instanceof PrimedTnt tnt) {
                    if (TNTCannonItem.isTntOwnedByPlayer(tnt, player.getUUID())) {
                        event.setCanceled(true);
                        LOGGER.info("Отменен урон от ТНТ из пушки для игрока " + player.getName().getString());
                    }
                } else if (event.getSource().getDirectEntity() == null && event.getSource().getEntity() instanceof PrimedTnt tnt) {
                    if (TNTCannonItem.isTntOwnedByPlayer(tnt, player.getUUID())) {
                        event.setCanceled(true);
                        LOGGER.info("Отменен урон от ТНТ (вариант 2) из пушки для игрока " + player.getName().getString());
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof PrimedTnt) {
            LOGGER.debug("ТНТ добавлено в мир: " + event.getEntity().getUUID());
        }
    }


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