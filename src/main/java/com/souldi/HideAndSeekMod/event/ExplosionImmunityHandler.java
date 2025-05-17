package com.souldi.HideAndSeekMod.event;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.item.TNTCannonItem;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID)
public class ExplosionImmunityHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getSource().isExplosion()) {
                boolean hasTntCannon = playerHasTntCannon(player);

                if (hasTntCannon) {
                    if (isTntExplosion(event.getSource())) {
                        event.setCanceled(true);
                        LOGGER.info("[Extra] Отменен урон от ТНТ для игрока " + player.getName().getString() + " (у игрока есть ТНТ пушка)");
                    }
                }
            }
        }
    }

    /**
     * Проверяет, есть ли у игрока ТНТ пушка в инвентаре
     */
    private static boolean playerHasTntCannon(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof TNTCannonItem) {
                return true;
            }
        }

        for (ItemStack stack : player.getHandSlots()) {
            if (stack.getItem() instanceof TNTCannonItem) {
                return true;
            }
        }

        return false;
    }

    /**
     * Проверяет, является ли источник урона взрывом ТНТ
     */
    private static boolean isTntExplosion(DamageSource source) {
        return (source.getDirectEntity() instanceof PrimedTnt) ||
                (source.getEntity() instanceof PrimedTnt) ||
                (source.isExplosion() &&
                        (source.getMsgId().equals("explosion") ||
                                source.getMsgId().equals("explosion.player")));
    }
}