package com.fjgoncalves.occultismjars.client;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

// the spirit's model, posed once and stored as plain vertices, then redrawn every frame as a purple ghost
final class HologramMesh {

    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(
            ResourceLocation.fromNamespaceAndPath(OccultismJars.MODID, "textures/misc/hologram.png"));

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
    @SuppressWarnings("unchecked")
    static HologramMesh bake(Entity entity, EntityRenderDispatcher dispatcher, int version) {
        Capture capture = new Capture();
        try {
            EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) dispatcher.getRenderer(entity);
            renderer.render(entity, 0.0F, 0.0F, new PoseStack(), capture, LightTexture.FULL_BRIGHT);
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

    void render(PoseStack pose, MultiBufferSource buffers, int color) {
        VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);
        PoseStack.Pose last = pose.last();
        float[] v = this.vertices;
        for (int i = 0; i < this.count; i++) {
            int o = i * STRIDE;
            consumer.addVertex(last, v[o], v[o + 1], v[o + 2])
                    .setColor(color)
                    .setUv(0.5F, 0.5F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(last, v[o + 3], v[o + 4], v[o + 5]);
        }
    }

    // records only positions and normals of whatever the entity renderer draws
    private static final class Capture implements MultiBufferSource, VertexConsumer {
        private float[] data = new float[STRIDE * 1024];
        private int count;

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return this;
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
    }
}
