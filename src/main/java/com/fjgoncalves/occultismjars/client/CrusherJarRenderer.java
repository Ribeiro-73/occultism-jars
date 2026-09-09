package com.fjgoncalves.occultismjars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.fjgoncalves.occultismjars.blockentity.CrusherJarBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

public class CrusherJarRenderer implements BlockEntityRenderer<CrusherJarBlockEntity> {

    private final EntityRenderDispatcher entityRenderer;

    public CrusherJarRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(CrusherJarBlockEntity jar, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Entity spirit = jar.getDisplayEntity();
        if (spirit == null) {
            return;
        }

        long time = jar.getLevel() != null ? jar.getLevel().getGameTime() : 0L;
        float spin = (time + partialTick) * 2.5F;

        pose.pushPose();
        pose.translate(0.5F, 0.16F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(0.25F, 0.25F, 0.25F);

        this.entityRenderer.setRenderShadow(false);
        try {
            this.entityRenderer.render(spirit, 0.0, 0.0, 0.0, 0.0F, partialTick, pose, buffers, packedLight);
        } catch (Exception ignored) {
            // Some entity renderers dislike being drawn outside a live world; skip quietly.
        }
        this.entityRenderer.setRenderShadow(true);

        pose.popPose();
    }
}
