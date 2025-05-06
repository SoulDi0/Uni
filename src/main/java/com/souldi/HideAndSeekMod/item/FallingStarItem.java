package com.souldi.HideAndSeekMod.item;

import com.souldi.HideAndSeekMod.entity.FallingStarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

public class FallingStarItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SPAWN_HEIGHT_OFFSET = 30; // Уменьшенная высота появления (было 80)

    // Цвета для чередования
    private static final ChatFormatting[] FORMATS = new ChatFormatting[] {
            ChatFormatting.YELLOW,
            ChatFormatting.GOLD
    };

    public FallingStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Отладочное сообщение
            player.sendSystemMessage(Component.literal("Запуск падающих звезд..."));

            List<ServerPlayer> allPlayers = null;
            if (level instanceof ServerLevel serverLevel) {
                allPlayers = serverLevel.getServer().getPlayerList().getPlayers();
            }

            if (allPlayers != null) {
                for (ServerPlayer target : allPlayers) {
                    if (!target.getUUID().equals(player.getUUID())) {
                        // Находим высокую позицию над игроком, но ниже чем раньше
                        BlockPos spawnPos = findSpawnPosition(level, target.blockPosition());

                        // Позиция игрока-цели
                        Vec3 targetPos = target.position();

                        // Создаем и добавляем сущность
                        FallingStarEntity fallingstar = new FallingStarEntity(
                                level,
                                spawnPos.getX() + 0.5,
                                spawnPos.getY() + 0.5,
                                spawnPos.getZ() + 0.5,
                                targetPos
                        );

                        level.addFreshEntity(fallingstar);

                        // Отладочное сообщение с координатами
                        LOGGER.info("Падающая звезда создана: " +
                                spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() +
                                " -> " + targetPos.x + ", " + targetPos.y + ", " + targetPos.z);
                    }
                }
            }

            if (!player.getAbilities().instabuild) {
                itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
            }
        }

        return InteractionResultHolder.success(itemStack);
    }

    private BlockPos findSpawnPosition(Level level, BlockPos playerPos) {
        // Находим позицию над игроком, но с уменьшенной высотой (30 блоков)
        int spawnY = Math.min(level.getMaxBuildHeight() - 20, playerPos.getY() + SPAWN_HEIGHT_OFFSET);

        // Близко к позиции игрока по X и Z для более точного попадания
        int spawnX = playerPos.getX() + (level.getRandom().nextInt(5) - 2); // ±2 блока
        int spawnZ = playerPos.getZ() + (level.getRandom().nextInt(5) - 2); // ±2 блока

        return new BlockPos(spawnX, spawnY, spawnZ);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        // Создаем текст с чередующимися цветами
        String text = "Загадай желание!";
        MutableComponent coloredText = Component.empty();

        for (int i = 0; i < text.length(); i++) {
            String letter = String.valueOf(text.charAt(i));
            MutableComponent letterComponent = Component.literal(letter).withStyle(FORMATS[i % 2]);
            coloredText = coloredText.append(letterComponent);
        }

        tooltip.add(coloredText);
        tooltip.add(Component.translatable("item.hide_and_seek.falling_star.tooltip.2"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}