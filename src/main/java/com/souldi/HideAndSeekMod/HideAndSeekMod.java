package com.souldi.HideAndSeekMod;

import com.souldi.HideAndSeekMod.entity.ModEntities;
import com.souldi.HideAndSeekMod.item.ModItems;
import com.souldi.HideAndSeekMod.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(HideAndSeekMod.MOD_ID)
public class HideAndSeekMod {
    public static final String MOD_ID = "hide_and_seek";
    private static final Logger LOGGER = LogManager.getLogger();

    public static final CreativeModeTab HIDE_AND_SEEK_TAB = new CreativeModeTab(MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.COMPASS);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("itemGroup." + MOD_ID);
        }
    };

    public HideAndSeekMod() {
        LOGGER.info("Initializing Hide and Seek Mod...");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register items
        ModItems.register(modEventBus);

        // Register entities
        ModEntities.register(modEventBus);

        // Регистрируем обработчик сетевых пакетов
        modEventBus.addListener(this::setup);

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Hide and Seek Mod initialized successfully!");
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Регистрация сетевых пакетов должна быть на серверной стороне для синхронизации
        event.enqueueWork(NetworkHandler::register);
    }
}