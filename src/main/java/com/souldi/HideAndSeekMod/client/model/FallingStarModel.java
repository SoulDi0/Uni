package com.souldi.HideAndSeekMod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.souldi.HideAndSeekMod.HideAndSeekMod;
import com.souldi.HideAndSeekMod.entity.FallingStarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FallingStarModel extends EntityModel<FallingStarEntity> {
    private static final Logger LOGGER = LogManager.getLogger();

    // Используем для идентификации слоя модели при регистрации
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.tryParse(HideAndSeekMod.MOD_ID + ":falling_star"), "main");

    private final ModelPart mainPart;

    public FallingStarModel(ModelPart root) {
        LOGGER.info("Initializing FallingStarModel with root: " + root);
        this.mainPart = root;
    }

    /**
     * Создание резервной модели звезды
     */
    public static LayerDefinition createBodyLayer() {
        LOGGER.info("Creating body layer for FallingStarModel");
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Используем координаты UV для нашей текстуры

        // Горизонтальная полоса звезды
        partdefinition.addOrReplaceChild("horizontal", CubeListBuilder.create()
                        .texOffs(0, 0) // Использует верхнюю часть текстуры (0,0 - 16,4)
                        .addBox(-8.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Вертикальная полоса звезды
        partdefinition.addOrReplaceChild("vertical", CubeListBuilder.create()
                        .texOffs(0, 4) // Использует нижнюю часть текстуры (0,4 - 16,20)
                        .addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(FallingStarEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Анимация вращения только по горизонтали (Y-ось)
        // Делаем вращение медленнее
        float rotationSpeed = 0.1F; // Уменьшили с 0.3F до 0.1F
        this.mainPart.yRot = ageInTicks * rotationSpeed;

        // Фиксируем другие оси - никакого движения по вертикали
        this.mainPart.xRot = 0;
        this.mainPart.zRot = 0;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        // Рендерим с высокой яркостью для лучшей видимости
        this.mainPart.render(poseStack, buffer, packedLight, packedOverlay,
                1.0F, 0.9F, 0.0F, 1.0F); // Яркий желтый цвет
    }
}