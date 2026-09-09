package com.fjgoncalves.occultismjars.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
    public int getViewDistance() {
        return 24;
    }

    @Override
    public void render(CrusherJarBlockEntity jar, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Entity spirit = jar.getDisplayEntity();
        if (spirit == null) {
            return;
        }

        pose.pushPose();
        pose.translate(0.5F, 0.05F, 0.5F);
        pose.scale(0.42F, 0.42F, 0.42F);

        this.entityRenderer.setRenderShadow(false);
        try {
            this.entityRenderer.render(spirit, 0.0, 0.0, 0.0, 0.0F, 0.0F, pose, buffers, packedLight);
        } catch (Exception ignored) {
            // Some entity renderers dislike being drawn outside a live world; skip quietly.
        }
        this.entityRenderer.setRenderShadow(true);

        pose.popPose();
    }
}
