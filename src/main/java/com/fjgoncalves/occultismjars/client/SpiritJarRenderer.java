package com.fjgoncalves.occultismjars.client;

import org.jspecify.annotations.Nullable;

import com.fjgoncalves.occultismjars.blockentity.SpiritJarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SpiritJarRenderer implements BlockEntityRenderer<SpiritJarBlockEntity, SpiritJarRenderer.State> {

    private final EntityRenderDispatcher entityRenderer;

    public SpiritJarRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 24;
    }

    @Override
    public void extractRenderState(SpiritJarBlockEntity jar, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(jar, state, partialTicks, cameraPosition, breakProgress);
        state.spirit = null;
        Entity spirit = jar.getDisplayEntity();
        if (spirit == null) {
            return;
        }
        try {
            // partial tick 0 keeps it frozen in place
            EntityRenderState spiritState = this.entityRenderer.extractEntity(spirit, 0.0F);
            spiritState.shadowPieces.clear();
            spiritState.nameTag = null;
            spiritState.lightCoords = state.lightCoords;
            state.spirit = spiritState;
        } catch (Exception ignored) {
            // some renderers choke on an entity that isn't in a real world
        }
        // fixed on-screen height regardless of the spirit's real size
        state.scale = Mth.clamp(0.7375F / Math.max(spirit.getBbHeight(), 0.1F), 0.15F, 0.5F);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.spirit == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5F, 0.05F, 0.5F);
        pose.scale(state.scale, state.scale, state.scale);
        this.entityRenderer.submit(state.spirit, camera, 0.0, 0.0, 0.0, pose, collector);
        pose.popPose();
    }

    public static class State extends BlockEntityRenderState {
        @Nullable
        EntityRenderState spirit;
        float scale;
    }
}
