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

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.tryParse(HideAndSeekMod.MOD_ID + ":falling_star"), "main");

    private final ModelPart mainPart;

    public FallingStarModel(ModelPart root) {
        LOGGER.info("Initializing FallingStarModel with root: " + root);
        this.mainPart = root;
    }

    public static LayerDefinition createBodyLayer() {
        LOGGER.info("Creating body layer for FallingStarModel from Blockbench JSON");
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();




        partdefinition.addOrReplaceChild("part1", CubeListBuilder.create()
                        .texOffs(4, 9)
                        .addBox(7, 0, 0, 2, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part2", CubeListBuilder.create()
                        .texOffs(8, 9)
                        .addBox(7, 0, 2, 2, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part3", CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(5, 0, 4, 6, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part4", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1, 0, 6, 18, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part5", CubeListBuilder.create()
                        .texOffs(0, 4)
                        .addBox(3, 0, 8, 10, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part6", CubeListBuilder.create()
                        .texOffs(3, 7)
                        .addBox(5, 0, 10, 6, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part7", CubeListBuilder.create()
                        .texOffs(6, 8)
                        .addBox(3, 0, 12, 4, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part8", CubeListBuilder.create()
                        .texOffs(9, 10)
                        .addBox(3, 0, 14, 2, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part9", CubeListBuilder.create()
                        .texOffs(11, 1)
                        .addBox(1, 0, 16, 2, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part10", CubeListBuilder.create()
                        .texOffs(9, 1)
                        .addBox(9, 0, 12, 4, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part11", CubeListBuilder.create()
                        .texOffs(11, 7)
                        .addBox(11, 0, 14, 2, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));


        partdefinition.addOrReplaceChild("part12", CubeListBuilder.create()
                        .texOffs(11, 4)
                        .addBox(13, 0, 16, 2, 2, 2),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(FallingStarEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.mainPart.yRot = 0;
        this.mainPart.xRot = 0;
        this.mainPart.zRot = 0;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.mainPart.render(poseStack, buffer, packedLight, packedOverlay,
                1.0F, 0.9F, 0.0F, 1.0F);
    }
}