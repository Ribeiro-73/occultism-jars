package com.fjgoncalves.occultismjars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.blockentity.CrusherJarBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class CrusherJarRenderer implements BlockEntityRenderer<CrusherJarBlockEntity> {

    public static final ModelResourceLocation SPIRIT_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(OccultismJars.MODID, "block/trapped_spirit"));

    public CrusherJarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 24;
    }

    @Override
    public void render(CrusherJarBlockEntity jar, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (jar.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(SPIRIT_MODEL);

        pose.pushPose();
        pose.translate(0.0F, 0.02F, 0.0F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        minecraft.getBlockRenderer().getModelRenderer()
                .renderModel(pose.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        pose.popPose();
    }
}
