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

    // Путь к текстуре
    private static final ResourceLocation TEXTURE = new ResourceLocation(HideAndSeekMod.MOD_ID, "textures/entity/falling_star.png");

    private final FallingStarModel model;

    public FallingStarRenderer(EntityRendererProvider.Context context) {
        super(context);
        LOGGER.info("Creating FallingStarRenderer with context: " + context);
        this.model = new FallingStarModel(context.bakeLayer(FallingStarModel.LAYER_LOCATION));
        this.shadowRadius = 1.5F; // Тень для звезды
    }

    @Override
    public void render(FallingStarEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Увеличиваем размер модели в 5 раз
        float scale = 8.0F;
        poseStack.scale(scale, scale, scale);

        // После анализа модели, определён точный центр: (8, 1, 8)
        float centerX = 8.0F;
        float centerY = 1.0F;
        float centerZ = 8.0F;

        // Смещаем модель так, чтобы её центр совпадал с центром сущности
        poseStack.translate(-centerX/16.0F, -centerY/16.0F, -centerZ/16.0F);

        // Вращаем модель вокруг вертикальной оси (вращаемся только по оси Y)
        float rotationSpeed = 0.5F; // Медленное вращение
        poseStack.mulPose(Vector3f.YP.rotationDegrees(entity.tickCount * rotationSpeed));

        // Получаем буфер с эффектом свечения
        VertexConsumer vertexConsumer = buffer.getBuffer(
                RenderType.entityTranslucentEmissive(getTextureLocation(entity))
        );

        // Максимальная яркость для эффекта свечения
        int overlayCoords = OverlayTexture.NO_OVERLAY;
        int lightmapCoord = 15728880;

        // Рендерим модель
        this.model.renderToBuffer(
                poseStack,
                vertexConsumer,
                lightmapCoord,
                overlayCoords,
                1.0F, 0.9F, 0.1F, 1.0F
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FallingStarEntity entity) {
        return TEXTURE;
    }
}