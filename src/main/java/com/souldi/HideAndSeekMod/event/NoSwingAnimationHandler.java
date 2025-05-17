package com.souldi.HideAndSeekMod.event;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID, value = Dist.CLIENT)
public class NoSwingAnimationHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && isHoldingDrill(player)) {
                player.swinging = false;
                player.swingTime = 0;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @OnlyIn(Dist.CLIENT)
    public static void onMouseInput(InputEvent.MouseButton event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && isHoldingDrill(player) && event.getButton() == 0 && event.getAction() == 1) {
            player.swinging = false;
            player.swingTime = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (isHoldingDrill(player)) {
            player.swinging = false;
            player.swingTime = 0;

            if (player.level.isClientSide && Minecraft.getInstance().screen == null) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (isHoldingDrill(player)) {
            player.swinging = false;
            player.swingTime = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    @OnlyIn(Dist.CLIENT)
    public static void onPrePlayerSwing(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && isHoldingDrill(player)) {
                player.swinging = false;

                if (Minecraft.getInstance().screen == null) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && isHoldingDrill(player)) {
                // Make sure the animation doesn't render
                player.swinging = false;
                player.swingTime = 0;
            }
        }
    }

    private static boolean isHoldingDrill(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.getItem() instanceof SeekerDrillItem || offHand.getItem() instanceof SeekerDrillItem;
    }
}