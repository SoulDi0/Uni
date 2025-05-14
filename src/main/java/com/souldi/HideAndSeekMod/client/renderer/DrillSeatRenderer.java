package com.souldi.HideAndSeekMod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.souldi.HideAndSeekMod.entity.DrillSeatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DrillSeatRenderer extends EntityRenderer<DrillSeatEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/boat/oak.png");

    public DrillSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DrillSeatEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(DrillSeatEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // Не рендерим ничего - делаем сущность полностью невидимой
    }
}