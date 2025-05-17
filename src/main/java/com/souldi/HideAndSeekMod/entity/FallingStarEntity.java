package com.souldi.HideAndSeekMod.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FallingStarEntity extends Entity {
    private static final Logger LOGGER = LogManager.getLogger();

    private int lifetime = 0;
    private int maxLifetime = 400;
    private Vec3 targetPos;
    private double targetY;
    private boolean hasImpacted = false;
    private double fallSpeed = 0.7D;
    private UUID targetPlayerUUID;


    private static final int STOP_HEIGHT_ABOVE_PLAYER = 3;


    private List<BlockPos> destroyedBlocks = new ArrayList<>();
    private int blockDestroyTick = 0;
    private static final int DESTROY_FREQUENCY = 1; // Проверка блоков каждый тик


    private static final int STAR_POINTS = 5;
    private static final int MAIN_RADIUS = 8;
    private static final int INNER_RADIUS = 3;
    private static final int VERTICAL_RANGE = 2;

    public FallingStarEntity(EntityType<? extends FallingStarEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;


        this.setDeltaMovement(0, -fallSpeed, 0);
    }

    public FallingStarEntity(Level level, double x, double y, double z, Vec3 targetPos, UUID targetPlayerUUID) {
        this(ModEntities.FALLING_STAR.get(), level);


        this.setPos(x, y, z);
        this.targetPos = targetPos;


        this.targetY = targetPos.y + STOP_HEIGHT_ABOVE_PLAYER;
        this.targetPlayerUUID = targetPlayerUUID;


        Vec3 direction = targetPos.subtract(x, y, z).normalize();


        this.setDeltaMovement(
                direction.x * fallSpeed,
                direction.y * fallSpeed,
                direction.z * fallSpeed
        );

        LOGGER.info("Created FallingStarEntity at " + x + ", " + y + ", " + z + " targeting " + targetPos + " for player " + targetPlayerUUID);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TargetX") && tag.contains("TargetY") && tag.contains("TargetZ")) {
            double tx = tag.getDouble("TargetX");
            double ty = tag.getDouble("TargetY");
            double tz = tag.getDouble("TargetZ");
            this.targetPos = new Vec3(tx, ty, tz);
            this.targetY = ty;
        } else {

            this.targetPos = null;
            this.targetY = this.getY() - 10.0;
            this.setDeltaMovement(0, -fallSpeed, 0);
        }

        this.lifetime = tag.getInt("Lifetime");
        this.maxLifetime = tag.getInt("MaxLifetime");
        this.hasImpacted = tag.getBoolean("HasImpacted");

        if (tag.hasUUID("TargetPlayerUUID")) {
            this.targetPlayerUUID = tag.getUUID("TargetPlayerUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.targetPos != null) {
            tag.putDouble("TargetX", this.targetPos.x);
            tag.putDouble("TargetY", this.targetPos.y);
            tag.putDouble("TargetZ", this.targetPos.z);
        }

        tag.putInt("Lifetime", this.lifetime);
        tag.putInt("MaxLifetime", this.maxLifetime);
        tag.putBoolean("HasImpacted", this.hasImpacted);

        if (this.targetPlayerUUID != null) {
            tag.putUUID("TargetPlayerUUID", this.targetPlayerUUID);
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void tick() {
        super.tick();


        this.lifetime++;


        if (this.lifetime > this.maxLifetime) {
            this.discard();
            return;
        }


        if (!this.hasImpacted) {

            BlockPos currentBlockPos = new BlockPos(this.getX(), this.getY(), this.getZ());


            this.move(MoverType.SELF, this.getDeltaMovement());


            if (this.getY() <= this.targetY) {
                LOGGER.info("Star reached target height: " + this.getY() + " <= " + this.targetY);
                impact();
                return;
            }


            blockDestroyTick++;
            if (blockDestroyTick >= DESTROY_FREQUENCY) {
                blockDestroyTick = 0;


                destroyBlocksInStarPattern(currentBlockPos);


                if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            this.getX(), this.getY(), this.getZ(),
                            10, 0.5, 0.5, 0.5, 0.01);
                }
            }


            BlockPos blockPos = new BlockPos(this.getX(), this.getY(), this.getZ());
            BlockState blockState = this.level.getBlockState(blockPos);

            if (!blockState.isAir()) {
                LOGGER.info("Star hit a block: " + blockState.getBlock().getName().getString());


                Player targetPlayer = getTargetPlayer();
                if (targetPlayer != null) {
                    double playerY = targetPlayer.getY() + STOP_HEIGHT_ABOVE_PLAYER;


                    if (this.getY() <= playerY) {
                        LOGGER.info("Star stopped above player: " + this.getY() + " <= " + playerY);
                        impact();
                        return;
                    }
                }


                destroyBlocksInStarPattern(blockPos);


                double distanceToTarget = (targetPos != null)
                        ? new Vec3(this.getX(), this.getY(), this.getZ()).distanceTo(targetPos)
                        : 0;

                if (targetPos != null && distanceToTarget > 3.0) {

                    Vec3 newDirection = targetPos.subtract(this.getX(), this.getY(), this.getZ()).normalize();
                    this.setDeltaMovement(
                            newDirection.x * fallSpeed,
                            newDirection.y * fallSpeed,
                            newDirection.z * fallSpeed
                    );
                    return;
                }
                impact();
            }
        }
    }


    private Player getTargetPlayer() {
        if (targetPlayerUUID != null) {
            for (Player player : level.players()) {
                if (player.getUUID().equals(targetPlayerUUID)) {
                    return player;
                }
            }
        }
        return null;
    }


    private void destroyBlocksInStarPattern(BlockPos center) {

        List<BlockPos> blocksToDestroy = new ArrayList<>();


        for (int y = -VERTICAL_RANGE; y <= VERTICAL_RANGE; y++) {
            for (int x = -MAIN_RADIUS; x <= MAIN_RADIUS; x++) {
                for (int z = -MAIN_RADIUS; z <= MAIN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);


                    if (x == 0 && z == 0 && y == 0) {
                        blocksToDestroy.add(pos);
                        continue;
                    }


                    double distance = Math.sqrt(x * x + z * z);


                    if (distance > MAIN_RADIUS) continue;


                    double angle = Math.atan2(z, x);

                    if (angle < 0) angle += 2 * Math.PI;


                    double sectorAngle = 2 * Math.PI / STAR_POINTS;
                    double normalizedAngle = angle % sectorAngle;


                    double threshold;
                    if (normalizedAngle < sectorAngle / 2) {

                        threshold = MAIN_RADIUS - (MAIN_RADIUS - INNER_RADIUS) * (normalizedAngle * 2 / sectorAngle);
                    } else {

                        threshold = INNER_RADIUS + (MAIN_RADIUS - INNER_RADIUS) * ((normalizedAngle - sectorAngle/2) * 2 / sectorAngle);
                    }


                    if (distance <= threshold) {
                        blocksToDestroy.add(pos);
                    }
                }
            }
        }


        for (BlockPos pos : blocksToDestroy) {

            if (destroyedBlocks.contains(pos)) {
                continue;
            }

            breakBlockSafely(pos);
        }
    }

    private void impact() {
        if (!level.isClientSide && !this.hasImpacted) {
            this.hasImpacted = true;


            destroyBlocksInStarPattern(new BlockPos(this.getX(), this.getY(), this.getZ()));


            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0);


                serverLevel.sendParticles(ParticleTypes.LAVA,
                        this.getX(), this.getY(), this.getZ(),
                        30, 2.0, 0.5, 2.0, 0.1);

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        50, 2.0, 0.5, 2.0, 0.05);
            }

            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F, 0.5F);


            this.discard();
        }
    }


    private void breakBlockSafely(BlockPos pos) {

        if (destroyedBlocks.contains(pos)) {
            return;
        }


        BlockState blockState = this.level.getBlockState(pos);
        if (!blockState.isAir() &&
                blockState.getDestroySpeed(level, pos) >= 0 &&
                level.getBlockEntity(pos) == null) {



            destroyedBlocks.add(pos);


            level.destroyBlock(pos, false);


            if (!level.isClientSide && level instanceof ServerLevel serverLevel && random.nextFloat() < 0.1) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }


    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(2.0);
    }
}