package com.fjgoncalves.occultismjars.client;

import com.fjgoncalves.occultismjars.Config;
import com.fjgoncalves.occultismjars.menu.SpiritJarMenu;
import com.klikli_dev.occultism.client.gui.spirit.SpiritGui;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

public class SpiritJarScreen extends AbstractContainerScreen<SpiritJarMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("occultism", "textures/gui/inventory_spirit.png");
    private static final int SLOT_SRC_X = 151;
    private static final int SLOT_SRC_Y = 53;

    private LivingEntity portrait;
    private boolean portraitResolved;

    public SpiritJarScreen(SpiritJarMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 175;
        this.imageHeight = 165;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // borrow the texture's own slot sprite for the two we place ourselves
        graphics.blit(TEXTURE, x + 133, y + 25, SLOT_SRC_X, SLOT_SRC_Y, 18, 18, 256, 256);
        graphics.blit(TEXTURE, x + 133, y + 53, SLOT_SRC_X, SLOT_SRC_Y, 18, 18, 256, 256);

        renderProgress(graphics, x, y);
        renderPortrait(graphics);
    }

    private void renderProgress(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 140, y + 42, x + 145, y + 54, 0xFF373737);
        int max = this.menu.getMaxProgress();
        int p = this.menu.getProgress();
        if (max > 0 && p > 0) {
            int h = Math.max(1, Math.round(12.0F * Math.min(p, max) / max));
            graphics.fill(x + 141, y + 42, x + 144, y + 42 + h, 0xFFB98FE0);
        }
    }

    private void renderPortrait(GuiGraphics graphics) {
        LivingEntity spirit = this.portrait();
        if (spirit == null) {
            return;
        }
        float height = Math.max(spirit.getBbHeight(), 0.5F);
        int scale = Mth.clamp(Math.round(40.0F / height), 8, 24);
        try {
            // fixed args so it doesn't follow the mouse
            SpiritGui.drawEntityToGui(graphics, this.leftPos + 31, this.topPos + 63, scale, -62.0F, -18.0F, spirit);
        } catch (Throwable ignored) {
        }
    }

    private LivingEntity portrait() {
        if (!this.portraitResolved) {
            this.portraitResolved = true;
            CompoundTag tag = this.menu.getContainedTag();
            if (tag != null && Config.RENDER_TRAPPED_SPIRIT.get() && this.minecraft != null && this.minecraft.level != null) {
                try {
                    Entity entity = EntityType.create(tag.getCompound("data"), this.minecraft.level).orElse(null);
                    if (entity instanceof LivingEntity living) {
                        this.portrait = living;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return this.portrait;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
