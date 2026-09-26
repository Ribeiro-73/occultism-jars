package com.fjgoncalves.occultismjars.client;

import org.jspecify.annotations.Nullable;

import com.fjgoncalves.occultismjars.block.HolographicBaseBlock;
import com.fjgoncalves.occultismjars.blockentity.HolographicBaseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HolographicBaseRenderer implements BlockEntityRenderer<HolographicBaseBlockEntity, HolographicBaseRenderer.State> {

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
    private final ItemModelResolver itemModelResolver;

    public HolographicBaseRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
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
    public void extractRenderState(HolographicBaseBlockEntity base, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(base, state, partialTicks, cameraPosition, breakProgress);
        state.facing = base.getBlockState().getValue(HolographicBaseBlock.FACING);
        this.itemModelResolver.updateForTopItem(state.gem, base.getGem(), ItemDisplayContext.FIXED, base.getLevel(), null,
                (int) base.getBlockPos().asLong());
        state.mesh = this.mesh(base);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        this.submitGem(state, pose, collector);
        this.submitHologram(state, pose, collector);
    }

    private void submitGem(State state, PoseStack pose, SubmitNodeCollector collector) {
        if (state.gem.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5F, GEM_HEIGHT, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        pose.translate(0.0F, 0.0F, GEM_FORWARD);
        pose.mulPose(Axis.XP.rotationDegrees(-GEM_TILT));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(GEM_SCALE, GEM_SCALE, GEM_SCALE);
        state.gem.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    // the model is measured from its real vertices, then scaled to fit the free space and centred in it
    private void submitHologram(State state, PoseStack pose, SubmitNodeCollector collector) {
        HologramMesh mesh = state.mesh;
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
        collector.submitCustomGeometry(pose, HologramMesh.RENDER_TYPE, (last, buffer) -> mesh.render(last, buffer, HOLOGRAM_COLOR));
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

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState gem = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        @Nullable
        HologramMesh mesh;
    }
}
