package com.fjgoncalves.occultismjars.client;

import com.fjgoncalves.occultismjars.Config;
import com.fjgoncalves.occultismjars.blockentity.SpiritWorkerBlockEntity;
import com.fjgoncalves.occultismjars.menu.SpiritWorkerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

// laid out like the horse screen: the spirit in the black frame, its slots in the grey panel next to it
public class SpiritWorkerScreen extends AbstractContainerScreen<SpiritWorkerMenu> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/horse.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    // fixed pose so the portrait never follows the mouse
    private static final float PORTRAIT_YAW = (float) Math.atan(-62.0 / 40.0);
    private static final float PORTRAIT_PITCH = (float) Math.atan(-18.0 / 40.0);

    private LivingEntity portrait;
    private boolean portraitResolved;

    public SpiritWorkerScreen(SpiritWorkerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 133, y + 25, 18, 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 133, y + 53, 18, 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + 151, y + 53, 18, 18);

        this.renderProgress(graphics, x, y);
        this.renderPortrait(graphics, x, y);
    }

    private void renderProgress(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x + 140, y + 43, x + 145, y + 53, 0xFF373737);
        int max = this.menu.getMaxProgress();
        int p = this.menu.getProgress();
        if (max > 0 && p > 0) {
            int h = Math.max(1, Math.round(10.0F * Math.min(p, max) / max));
            graphics.fill(x + 141, y + 43, x + 144, y + 43 + h, 0xFFB98FE0);
        }
    }

    private void renderPortrait(GuiGraphicsExtractor graphics, int x, int y) {
        LivingEntity spirit = this.portrait();
        if (spirit == null) {
            return;
        }
        float height = Math.max(spirit.getBbHeight(), 0.5F);
        int size = Mth.clamp(Math.round(40.0F / height), 8, 24);
        try {
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, x + 26, y + 18, x + 78, y + 70, size, 0.0625F,
                    PORTRAIT_YAW, PORTRAIT_PITCH, spirit);
        } catch (Throwable ignored) {
        }
    }

    private LivingEntity portrait() {
        if (!this.portraitResolved) {
            this.portraitResolved = true;
            CompoundTag tag = this.menu.getSpiritData();
            if (tag != null && Config.RENDER_TRAPPED_SPIRIT.get() && this.minecraft != null && this.minecraft.level != null) {
                Entity entity = SpiritWorkerBlockEntity.createEntity(tag, this.minecraft.level);
                if (entity instanceof LivingEntity living) {
                    this.portrait = living;
                }
            }
        }
        return this.portrait;
    }
}
