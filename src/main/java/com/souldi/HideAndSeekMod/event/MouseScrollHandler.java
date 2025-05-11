package com.souldi.HideAndSeekMod.event;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Обработчик прокрутки колесика мыши для SeekerDrillItem
 */
@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID, value = Dist.CLIENT)
public class MouseScrollHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        // Проверяем, держит ли игрок бур в руке
        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.getItem() instanceof SeekerDrillItem drillItem) {
            // Получаем направление прокрутки (положительное - вверх, отрицательное - вниз)
            double scrollDelta = event.getScrollDelta();

            // Передаем событие прокрутки в бур
            boolean handled = drillItem.onScroll(
                    mainHandItem, player, (int) Math.signum(scrollDelta));

            // Если обработано, отменяем базовое событие
            if (handled) {
                event.setCanceled(true);
            }
        }
    }
}