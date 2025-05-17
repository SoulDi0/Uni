package com.souldi.HideAndSeekMod.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TNTCannonItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final float SHOOT_POWER = 1.5f;
    private static final float TNT_FUSE = 80.0f;

    private static final int COLOR_RED = 0xFF0000;
    private static final int COLOR_WHITE = 0xFFFFFF;

    private static final Map<UUID, UUID> OWNER_TNT_MAP = new ConcurrentHashMap<>();

    public TNTCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            shootTNT(level, player);

            return InteractionResultHolder.success(itemStack);
        }

        return InteractionResultHolder.pass(itemStack);
    }

    /**
     * Создает и запускает ТНТ в направлении взгляда игрока
     */
    private void shootTNT(Level level, Player player) {
        Vec3 playerPos = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0f);

        Vec3 spawnPos = playerPos.add(viewVector.multiply(1.2, 1.2, 1.2));

        PrimedTnt tnt = new PrimedTnt(level, spawnPos.x, spawnPos.y, spawnPos.z, player);

        tnt.setFuse((int) TNT_FUSE);

        CompoundTag data = tnt.getPersistentData();
        data.putUUID("CannonOwnerUUID", player.getUUID());

        OWNER_TNT_MAP.put(tnt.getUUID(), player.getUUID());

        Vec3 velocity = viewVector.multiply(SHOOT_POWER, SHOOT_POWER, SHOOT_POWER);

        double spreadFactor = 0.1;
        velocity = velocity.add(
                (Math.random() - 0.5) * spreadFactor,
                (Math.random()) * spreadFactor,
                (Math.random() - 0.5) * spreadFactor
        );

        tnt.setDeltaMovement(velocity);

        level.addFreshEntity(tnt);

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TNT_PRIMED,
                SoundSource.PLAYERS,
                1.0F, 1.0F
        );

        LOGGER.info("Игрок " + player.getName().getString() + " выстрелил из ТНТ пушки");
    }

    public static boolean isTntOwnedByPlayer(PrimedTnt tnt, UUID playerUUID) {
        CompoundTag data = tnt.getPersistentData();
        if (data.hasUUID("CannonOwnerUUID")) {
            UUID ownerUUID = data.getUUID("CannonOwnerUUID");
            return ownerUUID.equals(playerUUID);
        }

        return OWNER_TNT_MAP.containsKey(tnt.getUUID()) &&
                OWNER_TNT_MAP.get(tnt.getUUID()).equals(playerUUID);
    }

    /**
     * Очищает старые записи о ТНТ
     */
    public static void cleanupOldTntEntries() {
        OWNER_TNT_MAP.clear();
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent coloredText = Component.empty()
                .append(Component.literal("Т").withStyle(Style.EMPTY.withColor(COLOR_RED)))
                .append(Component.literal("Н").withStyle(Style.EMPTY.withColor(COLOR_WHITE)))
                .append(Component.literal("Т ").withStyle(Style.EMPTY.withColor(COLOR_RED)))
                .append(Component.literal("пу").withStyle(Style.EMPTY.withColor(COLOR_RED)))
                .append(Component.literal("ш").withStyle(Style.EMPTY.withColor(COLOR_WHITE)))
                .append(Component.literal("ка").withStyle(Style.EMPTY.withColor(COLOR_RED)));

        return coloredText;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        MutableComponent coloredDesc = Component.empty()
                .append(Component.literal("Пушка с бесконечным запасом ").withStyle(Style.EMPTY.withColor(COLOR_WHITE)))
                .append(Component.literal("динамитов").withStyle(Style.EMPTY.withColor(COLOR_RED)));

        tooltip.add(coloredDesc);
        super.appendHoverText(stack, level, tooltip, flag);
    }
}