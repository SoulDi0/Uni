package com.souldi.HideAndSeekMod.item;

import com.souldi.HideAndSeekMod.HideAndSeekMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HideAndSeekMod.MOD_ID);

    public static final RegistryObject<Item> SHACKLES = ITEMS.register("shackles",
            () -> new ShacklesItem(new Item.Properties()
                    .tab(HideAndSeekMod.HIDE_AND_SEEK_TAB)
                    .stacksTo(1)));

    public static final RegistryObject<Item> FALLING_STAR = ITEMS.register("falling_star",
            () -> new FallingStarItem(new Item.Properties()
                    .tab(HideAndSeekMod.HIDE_AND_SEEK_TAB)
                    .stacksTo(1)));

    public static final RegistryObject<Item> LASER_POINTER = ITEMS.register("laser_pointer",
            () -> new LaserPointerItem(new Item.Properties()
                    .tab(HideAndSeekMod.HIDE_AND_SEEK_TAB)
                    .stacksTo(1)));  // Убрана durability, так как предмет неразрушимый

    public static final RegistryObject<Item> TNT_CANNON = ITEMS.register("tnt_cannon",
            () -> new TNTCannonItem(new Item.Properties()
                    .tab(HideAndSeekMod.HIDE_AND_SEEK_TAB)
                    .stacksTo(1)));

    public static final RegistryObject<Item> SEEKER_DRILL = ITEMS.register("seeker_drill",
            () -> new SeekerDrillItem(new Item.Properties()
                    .tab(HideAndSeekMod.HIDE_AND_SEEK_TAB)
                    .stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}