package com.souldi.HideAndSeekMod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShacklesItem extends Item {

    private static final UUID SHACKLES_MODIFIER_UUID = UUID.fromString("b3ee5361-e6ab-4578-a1c7-6834b95b33a2");

    private static final Map<UUID, Long> AFFECTED_PLAYERS = new HashMap<>();

    public ShacklesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide && level.getServer() != null) {
            List<ServerPlayer> allPlayers = level.getServer().getPlayerList().getPlayers();

            long currentTime = System.currentTimeMillis();
            long endTime = currentTime + 10000;

            for (ServerPlayer target : allPlayers) {
                if (!target.getUUID().equals(player.getUUID())) {
                    AttributeInstance moveSpeed = target.getAttribute(Attributes.MOVEMENT_SPEED);

                    if (moveSpeed != null) {
                        if (moveSpeed.getModifier(SHACKLES_MODIFIER_UUID) != null) {
                            moveSpeed.removeModifier(SHACKLES_MODIFIER_UUID);
                        }

                        AttributeModifier speedModifier = new AttributeModifier(
                                SHACKLES_MODIFIER_UUID,
                                "Shackles slowdown",
                                -0.7,
                                AttributeModifier.Operation.MULTIPLY_TOTAL
                        );

                        moveSpeed.addPermanentModifier(speedModifier);

                        AFFECTED_PLAYERS.put(target.getUUID(), endTime);
                    }
                }
            }

            if (!player.getAbilities().instabuild) {
                itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
            }
        }

        return InteractionResultHolder.success(itemStack);
    }

    public static void checkShacklesEffect(ServerPlayer player) {
        if (AFFECTED_PLAYERS.containsKey(player.getUUID())) {
            long currentTime = System.currentTimeMillis();
            long endTime = AFFECTED_PLAYERS.get(player.getUUID());

            if (currentTime >= endTime) {
                AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (moveSpeed != null && moveSpeed.getModifier(SHACKLES_MODIFIER_UUID) != null) {
                    moveSpeed.removeModifier(SHACKLES_MODIFIER_UUID);
                }

                AFFECTED_PLAYERS.remove(player.getUUID());
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hide_and_seek.shackles.tooltip"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}