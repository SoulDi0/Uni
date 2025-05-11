package com.souldi.HideAndSeekMod.event;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Обработчик событий для предотвращения урона игроку, использующему бур
 */
@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID)
public class DrillDamageHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Обработчик атаки, который отменяет любой урон для игрока, активно использующего бур
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        // Проверяем, что цель - игрок
        if (event.getEntity() instanceof Player player) {
            // Проверяем, находится ли игрок в списке активно бурящих
            if (SeekerDrillItem.isPlayerActiveDrilling(player.getUUID())) {
                // Отменяем любой урон для игрока
                event.setCanceled(true);
                LOGGER.debug("[DrillDamageHandler] Отменен урон для игрока " + player.getName().getString() + ", активно использующего бур");
            }
        }
    }
}