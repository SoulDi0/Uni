package com.souldi.HideAndSeekMod.item;

import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.souldi.HideAndSeekMod.network.DrillTargetPacket;
import com.souldi.HideAndSeekMod.network.NetworkHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public class SeekerDrillItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int COLOR_RED = 0xFF3300;
    private static final int COLOR_GRAY = 0x999999;

    private static final String TARGET_PLAYER_KEY = "TargetPlayerUUID";
    private static final String IS_ACTIVE_KEY = "IsActive";
    private static final String LAST_POSITION_X = "LastPosX";
    private static final String LAST_POSITION_Y = "LastPosY";
    private static final String LAST_POSITION_Z = "LastPosZ";
    private static final String SELECTION_MODE = "SelectionMode";
    private static final String SELECTED_INDEX_KEY = "SelectedIndex";
    private static final String USER_UUID_KEY = "UserUUID";
    private static final String START_TIME_KEY = "DrillStartTime"; // Для отслеживания времени начала бурения

    private static final int DRILL_RADIUS = 2;
    private static final float FORWARD_DRILL_DISTANCE = 1.5f;
    private static final int MAX_DISTANCE = 200;

    private static final int SELECTION_FRAMES = 6;

    private static final Set<UUID> ACTIVE_DRILLING_PLAYERS = new HashSet<>();

    private static final Map<UUID, List<UUID>> PLAYER_TARGET_CACHE = new HashMap<>();

    private static int tickCounter = 0;

    public SeekerDrillItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    public static boolean isPlayerActiveDrilling(UUID playerUUID) {
        return ACTIVE_DRILLING_PLAYERS.contains(playerUUID);
    }

    public static void resetDrillState(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(SELECTION_MODE, false);
        tag.putBoolean(IS_ACTIVE_KEY, false);
        tag.putInt(SELECTED_INDEX_KEY, 0);
        tag.remove(TARGET_PLAYER_KEY);
        tag.remove(START_TIME_KEY);

        if (tag.hasUUID(USER_UUID_KEY)) {
            ACTIVE_DRILLING_PLAYERS.remove(tag.getUUID(USER_UUID_KEY));
        }

        LOGGER.info("[SeekerDrillItem] Сброшено состояние бура через resetDrillState");
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slot, isSelected);

        tickCounter++;

        if (!(entity instanceof Player player)) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.hasUUID(USER_UUID_KEY)) {
            tag.putUUID(USER_UUID_KEY, player.getUUID());
        }

        boolean isActive = tag.getBoolean(IS_ACTIVE_KEY);
        boolean selectionMode = tag.getBoolean(SELECTION_MODE);

        if (isSelected && selectionMode && !level.isClientSide) {
            updatePlayerTargetList(player);
        }

        if (isActive && !level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            ACTIVE_DRILLING_PLAYERS.add(serverPlayer.getUUID());
            processDrilling(stack, (ServerLevel) level, serverPlayer);
        } else if (!isActive && ACTIVE_DRILLING_PLAYERS.contains(player.getUUID())) {
            ACTIVE_DRILLING_PLAYERS.remove(player.getUUID());
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();

        tag.putUUID(USER_UUID_KEY, player.getUUID());

        boolean isActive = tag.getBoolean(IS_ACTIVE_KEY);
        boolean selectionMode = tag.getBoolean(SELECTION_MODE);

        if (!level.isClientSide) {
            LOGGER.info("[SeekerDrillItem] use: isActive=" + isActive + ", selectionMode=" + selectionMode);

            if (isActive) {
                LOGGER.info("[SeekerDrillItem] Остановка бурения");
                tag.putBoolean(IS_ACTIVE_KEY, false);
                tag.remove(START_TIME_KEY);
                ACTIVE_DRILLING_PLAYERS.remove(player.getUUID());

                level.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.STONE_BREAK,
                        SoundSource.PLAYERS,
                        1.0F, 0.5F
                );
            }
            else if (selectionMode) {
                LOGGER.info("[SeekerDrillItem] Попытка начать бурение");

                int selectedIndex = tag.getInt(SELECTED_INDEX_KEY) % SELECTION_FRAMES;
                UUID playerUUID = player.getUUID();
                List<UUID> availablePlayers = PLAYER_TARGET_CACHE.getOrDefault(playerUUID, new ArrayList<>());

                if (selectedIndex < availablePlayers.size()) {
                    UUID targetUUID = availablePlayers.get(selectedIndex);

                    ServerPlayer targetPlayer = null;
                    for (ServerPlayer serverPlayer : ((ServerLevel) level).getServer().getPlayerList().getPlayers()) {
                        if (serverPlayer.getUUID().equals(targetUUID)) {
                            targetPlayer = serverPlayer;
                            break;
                        }
                    }

                    if (targetPlayer != null) {
                        tag.putUUID(TARGET_PLAYER_KEY, targetUUID);
                        tag.putBoolean(IS_ACTIVE_KEY, true);
                        tag.putBoolean(SELECTION_MODE, false);
                        // Сохраняем время начала бурения
                        tag.putLong(START_TIME_KEY, level.getGameTime());

                        ACTIVE_DRILLING_PLAYERS.add(player.getUUID());

                        // Сохраняем начальную позицию игрока для плавного ускорения
                        tag.putDouble(LAST_POSITION_X, player.getX());
                        tag.putDouble(LAST_POSITION_Y, player.getY());
                        tag.putDouble(LAST_POSITION_Z, player.getZ());

                        level.playSound(
                                null,
                                player.getX(), player.getY(), player.getZ(),
                                SoundEvents.STONE_BREAK,
                                SoundSource.PLAYERS,
                                1.0F, 0.7F
                        );

                        LOGGER.info("[SeekerDrillItem] Начато бурение к игроку: " + targetPlayer.getName().getString());
                    } else {
                        resetDrillState(stack);
                    }
                } else {
                    resetDrillState(stack);
                }
            }
            else {
                LOGGER.info("[SeekerDrillItem] Открытие меню выбора");

                tag.putBoolean(SELECTION_MODE, true);
                tag.putBoolean(IS_ACTIVE_KEY, false);
                tag.putInt(SELECTED_INDEX_KEY, 0);
                tag.remove(TARGET_PLAYER_KEY);
                tag.remove(START_TIME_KEY);

                tag.putUUID(USER_UUID_KEY, player.getUUID());

                updatePlayerTargetList(player);

                level.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK,
                        SoundSource.MASTER,
                        0.5F, 1.0F
                );
            }
        }

        return InteractionResultHolder.success(stack);
    }

    public boolean onScroll(ItemStack stack, Player player, int scrollDelta) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean selectionMode = tag.getBoolean(SELECTION_MODE);

        if (!selectionMode) {
            return false;
        }

        if (player.level.isClientSide) {
            NetworkHandler.INSTANCE.sendToServer(new DrillTargetPacket(
                    new UUID(0, 0),
                    scrollDelta
            ));

            return true;
        }

        int currentIndex = tag.getInt(SELECTED_INDEX_KEY);

        if (scrollDelta < 0) {
            currentIndex = (currentIndex + 1) % SELECTION_FRAMES;
        } else if (scrollDelta > 0) {
            currentIndex = (currentIndex - 1 + SELECTION_FRAMES) % SELECTION_FRAMES;
        }

        tag.putInt(SELECTED_INDEX_KEY, currentIndex);

        player.level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK,
                SoundSource.MASTER,
                0.3F, 1.2F
        );

        LOGGER.info("[SeekerDrillItem] Выбран индекс: " + currentIndex);
        return true;
    }

    private void updatePlayerTargetList(Player player) {
        UUID playerUUID = player.getUUID();

        if (player.level.isClientSide) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level;
        List<UUID> potentialTargets = new ArrayList<>();

        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            if (!serverPlayer.getUUID().equals(playerUUID) &&
                    serverPlayer.level.dimension().equals(player.level.dimension())) {
                potentialTargets.add(serverPlayer.getUUID());
            }
        }

        PLAYER_TARGET_CACHE.put(playerUUID, potentialTargets);
        LOGGER.info("[SeekerDrillItem] Обновлен список игроков, найдено: " + potentialTargets.size());
    }

    private void processDrilling(ItemStack stack, ServerLevel level, ServerPlayer player) {
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.getBoolean(IS_ACTIVE_KEY)) {
            return;
        }

        UUID targetUUID = null;
        if (tag.hasUUID(TARGET_PLAYER_KEY)) {
            targetUUID = tag.getUUID(TARGET_PLAYER_KEY);
        } else {
            tag.putBoolean(IS_ACTIVE_KEY, false);
            ACTIVE_DRILLING_PLAYERS.remove(player.getUUID());
            return;
        }

        ServerPlayer targetPlayer = null;
        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            if (serverPlayer.getUUID().equals(targetUUID)) {
                targetPlayer = serverPlayer;
                break;
            }
        }

        if (targetPlayer == null) {
            tag.putBoolean(IS_ACTIVE_KEY, false);
            ACTIVE_DRILLING_PLAYERS.remove(player.getUUID());
            return;
        }

        Vec3 playerPos = player.position();
        Vec3 targetPos = targetPlayer.position();

        Vec3 direction = targetPos.subtract(playerPos).normalize();
        double distance = playerPos.distanceTo(targetPos);

        // Обработка достижения цели - ближе 2 блоков
        if (distance < 2.0) {
            tag.putBoolean(IS_ACTIVE_KEY, false);
            ACTIVE_DRILLING_PLAYERS.remove(player.getUUID());

            level.sendParticles(
                    ParticleTypes.EXPLOSION,
                    targetPos.x, targetPos.y, targetPos.z,
                    10, 1.0, 1.0, 1.0, 0.1
            );

            level.playSound(
                    null,
                    targetPos.x, targetPos.y, targetPos.z,
                    SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F
            );

            return;
        }

        if (distance > MAX_DISTANCE) {
            tag.putBoolean(IS_ACTIVE_KEY, false);
            ACTIVE_DRILLING_PLAYERS.remove(player.getUUID());
            return;
        }

        // Для DrillSeatEntity необходимо только обновлять целевую позицию
        // Обновляем позицию цели в NBT, DrillSeatEntity будет использовать её для движения
        tag.putDouble(LAST_POSITION_X, targetPos.x);
        tag.putDouble(LAST_POSITION_Y, targetPos.y);
        tag.putDouble(LAST_POSITION_Z, targetPos.z);

        // Рассчитываем позицию для разрушения блоков впереди
        Vec3 forwardPos = playerPos.add(direction.scale(FORWARD_DRILL_DISTANCE));
        boolean buriedBlocks = drillBlocksInPath(level, playerPos, forwardPos);

        // Генерируем частицы и звуки
        if (tickCounter % 2 == 0) { // Уменьшаем частоту частиц для оптимизации
            level.sendParticles(
                    ParticleTypes.CRIT,
                    playerPos.x, playerPos.y, playerPos.z,
                    5, 0.3, 0.3, 0.3, 0.1 // Меньше частиц для более плавного эффекта
            );
        }

        if (buriedBlocks && tickCounter % 5 == 0) { // Уменьшаем частоту звуков
            level.playSound(
                    null,
                    playerPos.x, playerPos.y, playerPos.z,
                    SoundEvents.STONE_BREAK,
                    SoundSource.BLOCKS,
                    0.5F, 0.8F
            );
        }

        if (tickCounter % 10 == 0) {
            drawPathToTarget(level, playerPos, targetPos);
        }
    }

    private boolean drillBlocksInPath(ServerLevel level, Vec3 startPos, Vec3 endPos) {
        boolean anyBlockBroken = false;

        Vec3 direction = endPos.subtract(startPos);
        double length = direction.length();
        direction = direction.normalize();

        int steps = (int) Math.ceil(length * 2);

        if (steps <= 0) {
            return false;
        }

        double stepSize = length / steps;

        for (int i = 0; i < steps; i++) {
            Vec3 currentPos = startPos.add(direction.scale(i * stepSize));
            BlockPos blockPos = new BlockPos(currentPos);

            for (int dx = -DRILL_RADIUS; dx <= DRILL_RADIUS; dx++) {
                for (int dy = -DRILL_RADIUS; dy <= DRILL_RADIUS; dy++) {
                    for (int dz = -DRILL_RADIUS; dz <= DRILL_RADIUS; dz++) {
                        double radiusSquared = dx*dx + dy*dy + dz*dz;

                        if (radiusSquared <= DRILL_RADIUS*DRILL_RADIUS) {
                            BlockPos pos = blockPos.offset(dx, dy, dz);

                            if (tryBreakBlock(level, pos)) {
                                anyBlockBroken = true;
                            }
                        }
                    }
                }
            }
        }

        return anyBlockBroken;
    }

    private boolean tryBreakBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return false;
        }

        level.destroyBlock(pos, false);

        if (level.random.nextFloat() < 0.1f) {
            level.sendParticles(
                    ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.1, 0.1, 0.1, 0.02
            );
        }

        return true;
    }

    private void drawPathToTarget(ServerLevel level, Vec3 startPos, Vec3 targetPos) {
        Vec3 direction = targetPos.subtract(startPos);
        double distance = direction.length();

        int maxPathParticles = 20; // Уменьшено количество частиц для более эстетичного эффекта
        double particleSpacing = Math.max(1.0, distance / maxPathParticles);

        Vec3 normalizedDir = direction.normalize();

        for (int i = 0; i < maxPathParticles; i++) {
            double t = i * particleSpacing;
            if (t >= distance) break;

            Vec3 pos = startPos.add(normalizedDir.scale(t));

            float hue = 0.0f + (float)i / maxPathParticles * 0.16f;
            Vector3f color = hsvToRgb(hue, 1.0f, 1.0f);

            DustParticleOptions pathParticle = new DustParticleOptions(color, 0.7f);

            sendGlowingParticle(level, pos.x, pos.y, pos.z, pathParticle);
        }
    }

    private Vector3f hsvToRgb(float h, float s, float v) {
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        float r, g, b;
        switch(i % 6){
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }

        return new Vector3f(r, g, b);
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

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        initNBT(stack);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        initNBT(stack);
        return stack;
    }

    private void initNBT(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("CustomModelData", 1);
        tag.putBoolean(IS_ACTIVE_KEY, false);
        tag.putBoolean(SELECTION_MODE, false);
        tag.putInt(SELECTED_INDEX_KEY, 0);
        tag.remove(TARGET_PLAYER_KEY);
        tag.remove(START_TIME_KEY);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent coloredText = Component.empty()
                .append(Component.literal("Бур").withStyle(Style.EMPTY.withColor(COLOR_RED)))
                .append(Component.literal(" искателя").withStyle(Style.EMPTY.withColor(COLOR_GRAY)));

        return coloredText;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.literal("Никто не скроется от тебя...").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Покрути колесико мыши, чтобы выбрать к кому найти путь.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Нажми на ПКМ начать бурить.").withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}