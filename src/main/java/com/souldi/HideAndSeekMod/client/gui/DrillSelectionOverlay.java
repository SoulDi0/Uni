package com.souldi.HideAndSeekMod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.item.SeekerDrillItem;
import com.souldi.HideAndSeekMod.network.DrillMenuClosePacket;
import com.souldi.HideAndSeekMod.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = HideAndSeekMod.MOD_ID, value = Dist.CLIENT)
public class DrillSelectionOverlay extends GuiComponent {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SELECTION_MODE = "SelectionMode";
    private static final String TARGET_PLAYER_KEY = "TargetPlayerUUID";
    private static final String SELECTED_INDEX_KEY = "SelectedIndex";
    private static final String IS_ACTIVE_KEY = "IsActive";

    private static final int SELECTION_FRAMES = 6;

    private static final ResourceLocation[] SELECTION_FRAMES_TEXTURES = new ResourceLocation[SELECTION_FRAMES];

    static {
        for (int i = 0; i < SELECTION_FRAMES; i++) {
            SELECTION_FRAMES_TEXTURES[i] = new ResourceLocation(
                    HideAndSeekMod.MOD_ID + ":textures/gui/selection_frame_" + i + ".png");
        }
    }

    private static final float[][] HEX_POSITIONS = {
            {0.5f, 0.20f},
            {0.80f, 0.35f},
            {0.80f, 0.65f},
            {0.5f, 0.80f},
            {0.20f, 0.65f},
            {0.20f, 0.35f}
    };

    private static final List<PlayerInfo> playerInfoCache = new ArrayList<>();
    private static long lastUpdateTime = 0;

    private static boolean wasLeftMouseButtonDown = false;

    public static final IGuiOverlay DRILL_SELECTION_OVERLAY = (gui, poseStack, partialTick, screenWidth, screenHeight) -> {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null) {
            return;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof SeekerDrillItem)) {
            wasLeftMouseButtonDown = false;
            return;
        }

        CompoundTag tag = mainHandItem.getOrCreateTag();
        boolean selectionMode = tag.getBoolean(SELECTION_MODE);

        if (selectionMode) {
            boolean isLeftMouseButtonDown = minecraft.mouseHandler.isLeftPressed();

            if (isLeftMouseButtonDown && !wasLeftMouseButtonDown) {
                // Сбрасываем все флаги локально
                tag.putBoolean(SELECTION_MODE, false);
                tag.putBoolean(IS_ACTIVE_KEY, false);
                tag.putInt(SELECTED_INDEX_KEY, 0);
                tag.remove(TARGET_PLAYER_KEY);

                // ВАЖНО: отправляем пакет на сервер, чтобы сбросить состояние бура там тоже
                NetworkHandler.INSTANCE.sendToServer(new DrillMenuClosePacket());

                LOGGER.info("[DrillSelectionOverlay] Меню закрыто по ЛКМ, пакет отправлен на сервер");
            }

            wasLeftMouseButtonDown = isLeftMouseButtonDown;

            updatePlayerCache();

            renderSelectionWheel(minecraft, poseStack, tag, screenWidth, screenHeight);
        } else {
            wasLeftMouseButtonDown = false;
        }
    };

    private static void renderSelectionWheel(Minecraft minecraft, PoseStack poseStack,
                                             CompoundTag drillTag, int screenWidth, int screenHeight) {
        int selectedIndex = drillTag.getInt(SELECTED_INDEX_KEY);

        selectedIndex = selectedIndex % SELECTION_FRAMES;

        ResourceLocation frameTexture = SELECTION_FRAMES_TEXTURES[selectedIndex];

        renderSelectionFrame(poseStack, frameTexture, screenWidth, screenHeight);

        renderPlayerNames(minecraft, poseStack, screenWidth, screenHeight, selectedIndex);

        updateSelectedTarget(drillTag, selectedIndex);
    }

    private static void renderSelectionFrame(PoseStack poseStack, ResourceLocation texture, int screenWidth, int screenHeight) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);

        int frameSize = Math.min(screenWidth, screenHeight) * 3 / 4;

        int halfSize = frameSize / 2;

        blit(
                poseStack,
                screenWidth / 2 - halfSize, screenHeight / 2 - halfSize,
                0, 0,
                frameSize, frameSize,
                frameSize, frameSize
        );
    }

    private static void renderPlayerNames(Minecraft minecraft, PoseStack poseStack,
                                          int screenWidth, int screenHeight, int selectedIndex) {
        if (playerInfoCache.isEmpty()) {
            Font font = minecraft.font;
            int frameSize = Math.min(screenWidth, screenHeight) * 3 / 4;
            int halfSize = frameSize / 2;
            int frameX = screenWidth / 2 - halfSize;
            int frameY = screenHeight / 2 - halfSize;

            int textX = frameX + (int)(HEX_POSITIONS[selectedIndex][0] * frameSize);
            int textY = frameY + (int)(HEX_POSITIONS[selectedIndex][1] * frameSize);

            drawCenteredString(poseStack, font, "Нет игроков", textX, textY, 0xFF6666);
            return;
        }

        Font font = minecraft.font;

        int frameSize = Math.min(screenWidth, screenHeight) * 3 / 4;

        int halfSize = frameSize / 2;

        int frameX = screenWidth / 2 - halfSize;
        int frameY = screenHeight / 2 - halfSize;

        int playerCount = Math.min(SELECTION_FRAMES, playerInfoCache.size());

        for (int i = 0; i < SELECTION_FRAMES; i++) {
            int textX = frameX + (int)(HEX_POSITIONS[i][0] * frameSize);
            int textY = frameY + (int)(HEX_POSITIONS[i][1] * frameSize);

            int textColor = (i == selectedIndex) ? 0xFFFFFF : 0xAAAAAA;

            if (i < playerCount) {
                String playerName = playerInfoCache.get(i).name;

                drawCenteredString(poseStack, font, playerName, textX, textY, textColor);
            } else if (i == selectedIndex) {
                drawCenteredString(poseStack, font, "Пусто", textX, textY, 0xFF6666);
            }
        }
    }

    private static void updateSelectedTarget(CompoundTag drillTag, int selectedIndex) {
        if (!playerInfoCache.isEmpty() && selectedIndex < playerInfoCache.size()) {
            UUID targetUUID = playerInfoCache.get(selectedIndex).uuid;

            if (!drillTag.hasUUID(TARGET_PLAYER_KEY) ||
                    !drillTag.getUUID(TARGET_PLAYER_KEY).equals(targetUUID)) {
                drillTag.putUUID(TARGET_PLAYER_KEY, targetUUID);
            }
        } else {
            if (drillTag.contains(TARGET_PLAYER_KEY)) {
                drillTag.remove(TARGET_PLAYER_KEY);
            }
        }
    }

    private static void updatePlayerCache() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < 1000 && !playerInfoCache.isEmpty()) {
            return;
        }

        lastUpdateTime = currentTime;

        playerInfoCache.clear();

        Player currentPlayer = minecraft.player;
        if (currentPlayer == null) {
            return;
        }

        for (Player player : minecraft.level.players()) {
            if (!player.getUUID().equals(currentPlayer.getUUID())) {
                playerInfoCache.add(new PlayerInfo(
                        player.getUUID(),
                        player.getName().getString()
                ));
            }
        }
    }

    private static class PlayerInfo {
        final UUID uuid;
        final String name;

        PlayerInfo(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}