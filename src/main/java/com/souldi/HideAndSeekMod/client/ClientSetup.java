package com.souldi.HideAndSeekMod.client;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.client.gui.DrillSelectionOverlay;
import com.souldi.HideAndSeekMod.client.model.FallingStarModel;
import com.souldi.HideAndSeekMod.client.renderer.DrillSeatRenderer;
import com.souldi.HideAndSeekMod.client.renderer.FallingStarRenderer;
import com.souldi.HideAndSeekMod.entity.DrillSeatEntity;
import com.souldi.HideAndSeekMod.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Registering FallingStarRenderer");
            EntityRenderers.register(ModEntities.FALLING_STAR.get(), FallingStarRenderer::new);

            LOGGER.info("Registering DrillSeatRenderer");
            EntityRenderers.register(DrillSeatEntity.DRILL_SEAT.get(), DrillSeatRenderer::new);
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LOGGER.info("Registering FallingStarModel layer definition");
        event.registerLayerDefinition(FallingStarModel.LAYER_LOCATION, FallingStarModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        LOGGER.info("Registering DrillSelectionOverlay");
        event.registerAboveAll("drill_selection", DrillSelectionOverlay.DRILL_SELECTION_OVERLAY);
    }
}