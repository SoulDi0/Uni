package com.souldi.HideAndSeekMod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.client.model.FallingStarModel;
import com.souldi.HideAndSeekMod.entity.FallingStarEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FallingStarRenderer extends EntityRenderer<FallingStarEntity> {
    private static final Logger LOGGER = LogManager.getLogger();

    // Используем правильный путь к текстуре
    private static final ResourceLocation TEXTURE = new ResourceLocation(HideAndSeekMod.MOD_ID, "textures/entity/falling_star.png");

    private final FallingStarModel model;

    public FallingStarRenderer(EntityRendererProvider.Context context) {
        super(context);
        LOGGER.info("Creating FallingStarRenderer with context: " + context);
        this.model = new FallingStarModel(context.bakeLayer(FallingStarModel.LAYER_LOCATION));
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(FallingStarEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Отладочная информация
        LOGGER.info("Rendering falling star at: " + entity.getX() + ", " + entity.getY() + ", " + entity.getZ());

        poseStack.pushPose();

        // Увеличиваем масштаб для лучшей видимости
        float scale = 3.0F;
        poseStack.scale(scale, scale, scale);

        // Перемещаем модель для правильного центрирования
        poseStack.translate(0, -0.25F, 0);

        // Вращаем модель для правильной ориентации
        // Поворачиваем так, чтобы звезда летела горизонтально
        poseStack.mulPose(Vector3f.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(entity.getXRot()));

        // Добавляем небольшой наклон для эффекта полета
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(45));

        // Получаем время для анимации
        float ageInTicks = entity.tickCount + partialTicks;

        // Настройка анимации в модели
        this.model.setupAnim(entity, 0, 0, ageInTicks, 0, 0);

        // Максимальная яркость для лучшей видимости
        int overlayCoords = OverlayTexture.NO_OVERLAY;
        int lightmapCoord = 15728880; // Полная яркость

        // Выбираем тип рендеринга с эффектом свечения
        RenderType renderType = RenderType.entityTranslucentEmissive(getTextureLocation(entity));
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        // Рендерим модель
        this.model.renderToBuffer(poseStack, vertexConsumer, lightmapCoord, overlayCoords,
                1.0F, 1.0F, 0.2F, 1.0F); // Яркий желтый цвет

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FallingStarEntity entity) {
        return TEXTURE;
    }
}