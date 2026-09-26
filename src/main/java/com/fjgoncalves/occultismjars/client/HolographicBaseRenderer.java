package com.fjgoncalves.occultismjars.client;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.block.HolographicBaseBlock;
import com.fjgoncalves.occultismjars.blockentity.HolographicBaseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class HolographicBaseRenderer implements BlockEntityRenderer<HolographicBaseBlockEntity> {

    // free space between the gold rings, in blocks (1 pixel = 1/16)
    private static final float SPACE_BOTTOM = 4.0F / 16.0F;
    private static final float SPACE_TOP = 30.0F / 16.0F;
    private static final float MARGIN = 1.0F / 16.0F;
    private static final float MAX_WIDTH = 12.0F / 16.0F;

    // ARGB: alpha 0x88 = about half see-through
    private static final int HOLOGRAM_COLOR = 0x88A259FF;

    // where the gem sits: on the tilted panel at the front (the side facing whoever placed it)
    private static final float GEM_HEIGHT = 4.1F / 16.0F;
    private static final float GEM_FORWARD = 6.55F / 16.0F;
    private static final float GEM_TILT = 45.0F;
    private static final float GEM_SCALE = 0.3F;

    private final EntityRenderDispatcher entityRenderer;
    private final ItemRenderer itemRenderer;

    public HolographicBaseRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    public AABB getRenderBoundingBox(HolographicBaseBlockEntity base) {
        return new AABB(base.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
    }

    @Override
    public void render(HolographicBaseBlockEntity base, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Direction facing = base.getBlockState().getValue(HolographicBaseBlock.FACING);
        this.renderGem(base.getGem(), facing, base, pose, buffers, packedLight, packedOverlay);
        this.renderHologram(base, pose, buffers);
    }

    private void renderGem(ItemStack gem, Direction facing, HolographicBaseBlockEntity base, PoseStack pose,
                           MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (gem.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5F, GEM_HEIGHT, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(0.0F, 0.0F, GEM_FORWARD);
        pose.mulPose(Axis.XP.rotationDegrees(-GEM_TILT));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(GEM_SCALE, GEM_SCALE, GEM_SCALE);
        this.itemRenderer.renderStatic(gem, ItemDisplayContext.FIXED, packedLight, packedOverlay, pose, buffers,
                base.getLevel(), 0);
        pose.popPose();
    }

    // the model is measured from its real vertices, then scaled to fit the free space and centred in it
    private void renderHologram(HolographicBaseBlockEntity base, PoseStack pose, MultiBufferSource buffers) {
        HologramMesh mesh = this.mesh(base);
        if (mesh == null) {
            return;
        }
        float maxHeight = SPACE_TOP - SPACE_BOTTOM - 2.0F * MARGIN;
        float scale = Math.min(maxHeight / Math.max(mesh.height(), 0.01F), MAX_WIDTH / Math.max(mesh.width(), 0.01F));
        float y = SPACE_BOTTOM + (SPACE_TOP - SPACE_BOTTOM - mesh.height() * scale) / 2.0F;

        pose.pushPose();
        pose.translate(0.5F, y, 0.5F);
        pose.scale(scale, scale, scale);
        pose.translate(-mesh.centerX(), -mesh.bottom(), -mesh.centerZ());
        mesh.render(pose, buffers, HOLOGRAM_COLOR);
        pose.popPose();
    }

    @Nullable
    private HologramMesh mesh(HolographicBaseBlockEntity base) {
        int version = base.getSpiritVersion();
        Object cache = base.getRenderCache();
        if (cache instanceof HologramMesh cached && cached.version == version) {
            return cached;
        }
        // nothing to show for this version (no spirit, or it couldn't be drawn): don't retry every frame
        if (cache instanceof Integer empty && empty == version) {
            return null;
        }
        Entity spirit = base.getDisplayEntity();
        HologramMesh mesh = spirit == null ? null : HologramMesh.bake(spirit, this.entityRenderer, version);
        base.setRenderCache(mesh != null ? mesh : Integer.valueOf(version));
        return mesh;
    }
}
