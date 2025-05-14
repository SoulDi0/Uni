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
                // Completely reset animation
                player.swinging = false;
                player.swingTime = 0;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // Changed to LOWEST so it runs after menu handling
    @OnlyIn(Dist.CLIENT)
    public static void onMouseInput(InputEvent.MouseButton event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && isHoldingDrill(player) && event.getButton() == 0 && event.getAction() == 1) {
            // Cancel the left click swing animation entirely
            player.swinging = false;
            player.swingTime = 0;
            // Don't cancel the event completely, as this prevents menu interactions
            // event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL) // Changed from HIGHEST to NORMAL
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (isHoldingDrill(player)) {
            // Cancel the block interaction animation
            player.swinging = false;
            player.swingTime = 0;

            // Only cancel on client side and only if we're not interacting with a GUI
            if (player.level.isClientSide && Minecraft.getInstance().screen == null) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (isHoldingDrill(player)) {
            // Cancel the air swing animation
            player.swinging = false;
            player.swingTime = 0;
        }
    }

    // Modify the attack prevention handler to allow menu interaction
    @SubscribeEvent(priority = EventPriority.NORMAL) // Changed from HIGHEST to NORMAL
    @OnlyIn(Dist.CLIENT)
    public static void onPrePlayerSwing(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && isHoldingDrill(player)) {
                // Just prevent swing animation without canceling the entire event
                player.swinging = false;

                // Only cancel if we're not interacting with a GUI
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