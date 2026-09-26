package com.fjgoncalves.occultismjars.client;

import java.util.Arrays;
import java.util.List;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

// the spirit's model, posed once and stored as plain vertices, then redrawn every frame as a purple ghost
final class HologramMesh {

    static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(
            Identifier.fromNamespaceAndPath(OccultismJars.MODID, "textures/misc/hologram.png"));

    // x, y, z, nx, ny, nz per vertex
    private static final int STRIDE = 6;

    final int version;
    private final float[] vertices;
    private final int count;
    private final float minX, minY, minZ, maxX, maxY, maxZ;

    private HologramMesh(int version, float[] vertices, int count) {
        this.version = version;
        this.vertices = vertices;
        this.count = count;
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, z0 = Float.MAX_VALUE;
        float x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE, z1 = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            int o = i * STRIDE;
            x0 = Math.min(x0, vertices[o]);
            y0 = Math.min(y0, vertices[o + 1]);
            z0 = Math.min(z0, vertices[o + 2]);
            x1 = Math.max(x1, vertices[o]);
            y1 = Math.max(y1, vertices[o + 1]);
            z1 = Math.max(z1, vertices[o + 2]);
        }
        this.minX = x0;
        this.minY = y0;
        this.minZ = z0;
        this.maxX = x1;
        this.maxY = y1;
        this.maxZ = z1;
    }

    @Nullable
    static HologramMesh bake(Entity entity, EntityRenderDispatcher dispatcher, int version) {
        Capture capture = new Capture();
        try {
            EntityRenderState state = dispatcher.extractEntity(entity, 0.0F);
            state.shadowPieces.clear();
            state.nameTag = null;
            state.displayFireAnimation = false;
            dispatcher.submit(state, new CameraRenderState(), 0.0, 0.0, 0.0, new PoseStack(), capture);
        } catch (Exception e) {
            return null;
        }
        // entity models are drawn as quads; drop a dangling partial one if anything else slipped in
        int count = capture.count - capture.count % 4;
        return count == 0 ? null : new HologramMesh(version, capture.data, count);
    }

    float width() {
        return Math.max(this.maxX - this.minX, this.maxZ - this.minZ);
    }

    float height() {
        return this.maxY - this.minY;
    }

    float centerX() {
        return (this.minX + this.maxX) / 2.0F;
    }

    float centerZ() {
        return (this.minZ + this.maxZ) / 2.0F;
    }

    float bottom() {
        return this.minY;
    }

    void render(PoseStack.Pose pose, VertexConsumer consumer, int color) {
        float[] v = this.vertices;
        for (int i = 0; i < this.count; i++) {
            int o = i * STRIDE;
            consumer.addVertex(pose, v[o], v[o + 1], v[o + 2])
                    .setColor(color)
                    .setUv(0.5F, 0.5F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightCoordsUtil.FULL_BRIGHT)
                    .setNormal(pose, v[o + 3], v[o + 4], v[o + 5]);
        }
    }

    // stands in for the real renderer: every model the entity submits is drawn straight away into a
    // consumer that records only positions and normals
    private static final class Capture implements SubmitNodeCollector, VertexConsumer {
        private float[] data = new float[STRIDE * 1024];
        private int count;

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return this;
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                    int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite,
                                    int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            model.setupAnim(state);
            model.renderToBuffer(poseStack, this, lightCoords, overlayCoords, tintedColor);
        }

        @Override
        public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords,
                                    int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil,
                                    int tintedColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
                                    int outlineColor) {
            modelPart.render(poseStack, this, lightCoords, overlayCoords, tintedColor);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
                                         SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
            customGeometryRenderer.render(poseStack.last(), this);
        }

        @Override
        public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        }

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name,
                                  boolean seeThrough, int lightCoords, double distanceToCameraSq, CameraRenderState camera) {
        }

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow,
                               Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts,
                                     int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        }

        @Override
        public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords,
                               int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        }

        @Override
        public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer particleGroupRenderer) {
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if ((this.count + 1) * STRIDE > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            int o = this.count * STRIDE;
            this.data[o] = x;
            this.data[o + 1] = y;
            this.data[o + 2] = z;
            this.data[o + 3] = 0.0F;
            this.data[o + 4] = 1.0F;
            this.data[o + 5] = 0.0F;
            this.count++;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            if (this.count > 0) {
                int o = (this.count - 1) * STRIDE;
                this.data[o + 3] = x;
                this.data[o + 4] = y;
                this.data[o + 5] = z;
            }
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
