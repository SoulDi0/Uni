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
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.List;

public class FallingStarItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SPAWN_HEIGHT_OFFSET = 50;

    // Цвета для названия предмета
    private static final int COLOR_1 = 0xFFFF00;
    private static final int COLOR_2 = 0xFFD966;

    public FallingStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {

            player.sendSystemMessage(Component.literal("Запуск падающих звезд..."));

            List<ServerPlayer> allPlayers = null;
            if (level instanceof ServerLevel serverLevel) {
                allPlayers = serverLevel.getServer().getPlayerList().getPlayers();
            }

            if (allPlayers != null) {
                for (ServerPlayer target : allPlayers) {

                    if (!target.getUUID().equals(player.getUUID())) {

                        BlockPos spawnPos = findSpawnPosition(level, target.blockPosition());


                        Vec3 targetPos = target.position();


                        FallingStarEntity fallingstar = new FallingStarEntity(
                                level,
                                spawnPos.getX() + 0.5,
                                spawnPos.getY() + 0.5,
                                spawnPos.getZ() + 0.5,
                                targetPos,
                                target.getUUID()
                        );

                        level.addFreshEntity(fallingstar);


                        LOGGER.info("Падающая звезда создана: " +
                                spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() +
                                " -> " + targetPos.x + ", " + targetPos.y + ", " + targetPos.z +
                                " для игрока " + target.getName().getString());


                        level.playSound(
                                null,
                                player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
                                net.minecraft.sounds.SoundSource.PLAYERS,
                                1.0F, 0.7F
                        );
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

        int spawnY = Math.min(level.getMaxBuildHeight() - 20, playerPos.getY() + SPAWN_HEIGHT_OFFSET);


        int spawnX = playerPos.getX();
        int spawnZ = playerPos.getZ();

        return new BlockPos(spawnX, spawnY, spawnZ);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {

        tooltip.add(Component.translatable("item.hide_and_seek.falling_star.tooltip.2"));
        super.appendHoverText(stack, level, tooltip, flag);
    }


    @Override
    public Component getName(ItemStack stack) {

        String text = "Загадай желание!";
        MutableComponent coloredText = Component.empty();

        for (int i = 0; i < text.length(); i++) {
            String letter = String.valueOf(text.charAt(i));
            int color = (i % 2 == 0) ? COLOR_1 : COLOR_2;


            MutableComponent letterComponent = Component.literal(letter).withStyle(
                    Style.EMPTY.withColor(color)
            );


            coloredText = coloredText.append(letterComponent);
        }

        return coloredText;
    }
}