package com.souldi.HideAndSeekMod.client;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.client.model.FallingStarModel;
import com.souldi.HideAndSeekMod.client.renderer.FallingStarRenderer;
import com.souldi.HideAndSeekMod.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
            // Регистрируем рендерер для нашей сущности
            LOGGER.info("Registering FallingStarRenderer");
            EntityRenderers.register(ModEntities.FALLING_STAR.get(), FallingStarRenderer::new);
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Регистрируем слой модели
        LOGGER.info("Registering FallingStarModel layer definition");
        event.registerLayerDefinition(FallingStarModel.LAYER_LOCATION, FallingStarModel::createBodyLayer);
    }
}