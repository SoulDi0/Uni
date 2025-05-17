package com.souldi.HideAndSeekMod.event;

import com.mojang.math.Vector3f;
import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.entity.DrillSeatEntity;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Полностью отключенный обработчик камеры - просто заглушка
 * Не выполняет никаких операций с камерой
 */
@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID, value = Dist.CLIENT)
public class DrillCameraHandler {
    private static final Logger LOGGER = LogManager.getLogger();

}