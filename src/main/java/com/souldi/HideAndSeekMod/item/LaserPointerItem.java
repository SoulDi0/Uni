package com.souldi.HideAndSeekMod.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.ClipContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Vector3f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaserPointerItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<UUID, BlockPos> PLAYER_TARGET_BLOCKS = new HashMap<>();

    private static int tickCounter = 0;

    private static final int COLOR_BLUE = 0x0066FF;
    private static final int COLOR_ORANGE = 0xFF8800;

    public LaserPointerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            HitResult hitResult = rayTraceExtended(level, player, ClipContext.Fluid.NONE, 10000.0);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockPos targetPos = blockHit.getBlockPos();
                BlockState targetBlock = level.getBlockState(targetPos);

                if (!targetBlock.isAir()) {
                    BlockPos safePos = targetPos.above();
                    BlockState aboveBlock = level.getBlockState(safePos);
                    BlockState aboveAboveBlock = level.getBlockState(safePos.above());

                    if (aboveBlock.isAir() && aboveAboveBlock.isAir()) {
                        if (player instanceof ServerPlayer) {
                            ServerPlayer serverPlayer = (ServerPlayer) player;

                            Vec3 startPos = player.position();

                            serverPlayer.teleportTo(
                                    safePos.getX() + 0.5,
                                    safePos.getY(),
                                    safePos.getZ() + 0.5
                            );

                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(
                                        ParticleTypes.PORTAL,
                                        startPos.x, startPos.y + 1, startPos.z,
                                        30, 0.5, 1.0, 0.5, 0.1
                                );

                                serverLevel.sendParticles(
                                        ParticleTypes.PORTAL,
                                        safePos.getX() + 0.5, safePos.getY() + 1, safePos.getZ() + 0.5,
                                        30, 0.5, 1.0, 0.5, 0.1
                                );

                                level.playSound(
                                        null,
                                        safePos,
                                        SoundEvents.ENDERMAN_TELEPORT,
                                        SoundSource.PLAYERS,
                                        1.0F, 1.0F
                                );
                            }

                            LOGGER.info("Телепортация игрока " + player.getName().getString() +
                                    " на координаты: " + safePos.getX() + ", " +
                                    safePos.getY() + ", " + safePos.getZ() +
                                    ", расстояние: " + startPos.distanceTo(new Vec3(safePos.getX(), safePos.getY(), safePos.getZ())));

                            return InteractionResultHolder.success(itemStack);
                        }
                    } else {
                        player.displayClientMessage(
                                Component.literal("Недостаточно места для телепортации!"),
                                true
                        );
                    }
                }
            }
        }

        return InteractionResultHolder.fail(itemStack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !isSelected || !(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;

        tickCounter++;

        if (tickCounter % 2 == 0) {
            HitResult hitResult = rayTraceExtended(level, player, ClipContext.Fluid.NONE, 1000.0);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockPos targetPos = blockHit.getBlockPos();

                BlockState targetBlock = level.getBlockState(targetPos);
                BlockPos safePos = targetPos.above();
                BlockState aboveBlock = level.getBlockState(safePos);
                BlockState aboveAboveBlock = level.getBlockState(safePos.above());

                Vector3f highlightColor;
                if (!targetBlock.isAir() && aboveBlock.isAir() && aboveAboveBlock.isAir()) {
                    highlightColor = new Vector3f(0.3F, 1.0F, 0.3F);
                } else {
                    highlightColor = new Vector3f(1.0F, 0.3F, 0.3F);
                }

                if (level instanceof ServerLevel serverLevel) {
                    createCompleteOutlineEffect(serverLevel, targetPos, highlightColor);
                }
            }
        }
    }

    private void createCompleteOutlineEffect(ServerLevel level, BlockPos pos, Vector3f color) {
        float size = 0.5f;

        DustParticleOptions outlineParticle = new DustParticleOptions(color, size);

        int particlesPerEdge = 10;

        int particlesPerFace = 5;

        float pulse = (float)(0.5 + 0.5 * Math.sin(tickCounter * 0.1));

        float offset = 0.05f + pulse * 0.02f;

        for (int i = 0; i < particlesPerEdge; i++) {
            float t = (float)i / (particlesPerEdge - 1); // От 0 до 1

            sendGlowingParticle(level, pos.getX() + t, pos.getY() - offset, pos.getZ() - offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() + t, pos.getY() - offset, pos.getZ() + 1 + offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() - offset, pos.getY() - offset, pos.getZ() + t, outlineParticle);
            sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() - offset, pos.getZ() + t, outlineParticle);

            sendGlowingParticle(level, pos.getX() + t, pos.getY() + 1 + offset, pos.getZ() - offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() + t, pos.getY() + 1 + offset, pos.getZ() + 1 + offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() - offset, pos.getY() + 1 + offset, pos.getZ() + t, outlineParticle);
            sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() + 1 + offset, pos.getZ() + t, outlineParticle);

            sendGlowingParticle(level, pos.getX() - offset, pos.getY() + t, pos.getZ() - offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() - offset, pos.getY() + t, pos.getZ() + 1 + offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() + t, pos.getZ() - offset, outlineParticle);
            sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() + t, pos.getZ() + 1 + offset, outlineParticle);
        }

        DustParticleOptions innerParticle = new DustParticleOptions(color, size * 0.8f);

        for (int ix = 1; ix < particlesPerFace; ix++) {
            for (int iz = 1; iz < particlesPerFace; iz++) {
                float tx = (float)ix / particlesPerFace;
                float tz = (float)iz / particlesPerFace;

                sendGlowingParticle(level, pos.getX() + tx, pos.getY() - offset, pos.getZ() + tz, innerParticle);

                sendGlowingParticle(level, pos.getX() + tx, pos.getY() + 1 + offset, pos.getZ() + tz, innerParticle);
            }
        }

        for (int ix = 1; ix < particlesPerFace; ix++) {
            for (int iy = 1; iy < particlesPerFace; iy++) {
                float tx = (float)ix / particlesPerFace;
                float ty = (float)iy / particlesPerFace;

                sendGlowingParticle(level, pos.getX() + tx, pos.getY() + ty, pos.getZ() - offset, innerParticle);

                sendGlowingParticle(level, pos.getX() + tx, pos.getY() + ty, pos.getZ() + 1 + offset, innerParticle);
            }
        }

        for (int iz = 1; iz < particlesPerFace; iz++) {
            for (int iy = 1; iy < particlesPerFace; iy++) {
                float tz = (float)iz / particlesPerFace;
                float ty = (float)iy / particlesPerFace;

                sendGlowingParticle(level, pos.getX() - offset, pos.getY() + ty, pos.getZ() + tz, innerParticle);

                sendGlowingParticle(level, pos.getX() + 1 + offset, pos.getY() + ty, pos.getZ() + tz, innerParticle);
            }
        }

        DustParticleOptions centerParticle = new DustParticleOptions(color, size * 0.5f);
        sendGlowingParticle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, centerParticle);
    }

    private void sendGlowingParticle(ServerLevel level, double x, double y, double z, DustParticleOptions particle) {
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                particle,
                true,
                x, y, z,
                0, 0, 0,
                0,
                0
        );


        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {

            if (player.level.dimension() == level.dimension()) {

                player.connection.send(packet);
            }
        }
    }


    private HitResult rayTraceExtended(Level level, Player player, ClipContext.Fluid fluidMode, double maxDistance) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVector.x * maxDistance, viewVector.y * maxDistance, viewVector.z * maxDistance);

        ClipContext context = new ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                fluidMode,
                player
        );

        return level.clip(context);
    }


    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }


    @Override
    public Component getName(ItemStack stack) {

        MutableComponent coloredText = Component.empty()
                .append(Component.literal("Портативный").withStyle(Style.EMPTY.withColor(COLOR_BLUE)))
                .append(Component.literal(" телепорт").withStyle(Style.EMPTY.withColor(COLOR_ORANGE)));

        return coloredText;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {


        tooltip.add(Component.translatable("item.hide_and_seek.laser_pointer.tooltip.2"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}