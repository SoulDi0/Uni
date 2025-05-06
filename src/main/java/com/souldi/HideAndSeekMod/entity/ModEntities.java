package com.souldi.HideAndSeekMod.entity;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HideAndSeekMod.MOD_ID);

    public static final RegistryObject<EntityType<FallingStarEntity>> FALLING_STAR =
            ENTITY_TYPES.register("falling_star",
                    () -> EntityType.Builder.<FallingStarEntity>of(FallingStarEntity::new, MobCategory.MISC)
                            .sized(0.8F, 0.8F) // Size of the entity
                            .clientTrackingRange(16) // Tracking range
                            .updateInterval(1) // Update frequency
                            .build(HideAndSeekMod.MOD_ID + ":falling_star"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}