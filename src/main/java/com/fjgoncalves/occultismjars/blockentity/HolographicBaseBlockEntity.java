package com.fjgoncalves.occultismjars.blockentity;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.content.SpiritJob;
import com.klikli_dev.occultism.registry.OccultismItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class HolographicBaseBlockEntity extends SpiritWorkerBlockEntity {

    private ItemStack gem = ItemStack.EMPTY;

    // read from the gem, cached so the ticker doesn't re-parse it every tick
    private CompoundTag spiritData;
    private SpiritJob job;
    private int tier;

    public HolographicBaseBlockEntity(BlockPos pos, BlockState state) {
        super(OccultismJars.HOLOGRAPHIC_BASE_BE.get(), pos, state);
    }

    public static boolean isGem(ItemStack stack) {
        return stack.is(OccultismItems.SOUL_GEM_ITEM.get()) || stack.is(OccultismItems.TRINITY_GEM_ITEM.get());
    }

    // a filled soul/trinity gem holding a spirit with a job we can run, at any tier
    public static boolean accepts(ItemStack stack) {
        if (!isGem(stack)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.ENTITY_DATA);
        if (data == null) {
            return false;
        }
        String factoryId = data.copyTag().getCompound("spiritJob").getString("factoryId");
        SpiritJob job = SpiritJob.byFactoryId(factoryId);
        if (job == null) {
            return false;
        }
        int tier = job.tierOf(factoryId);
        return tier >= 1 && tier <= job.maxTier();
    }

    public ItemStack getGem() {
        return this.gem;
    }

    public boolean hasGem() {
        return !this.gem.isEmpty();
    }

    public void insertGem(ItemStack stack) {
        this.gem = stack.copyWithCount(1);
        this.readSpirit();
        this.onSpiritChanged();
    }

    public ItemStack removeGem() {
        ItemStack taken = this.gem;
        this.gem = ItemStack.EMPTY;
        this.readSpirit();
        this.onSpiritChanged();
        return taken;
    }

    private void readSpirit() {
        this.spiritData = null;
        this.job = null;
        this.tier = 0;
        CustomData data = this.gem.get(DataComponents.ENTITY_DATA);
        if (data == null) {
            return;
        }
        CompoundTag tag = data.copyTag();
        String factoryId = tag.getCompound("spiritJob").getString("factoryId");
        SpiritJob found = SpiritJob.byFactoryId(factoryId);
        if (found == null) {
            return;
        }
        this.spiritData = tag;
        this.job = found;
        this.tier = found.tierOf(factoryId);
    }

    @Nullable
    @Override
    public CompoundTag getSpiritData() {
        return this.spiritData;
    }

    @Nullable
    @Override
    public SpiritJob getJob() {
        return this.job;
    }

    @Override
    public int getTier() {
        return this.tier;
    }

    @Override
    protected Component getBlockName() {
        return Component.translatable("block.occultismjars.holographic_base");
    }

    // just the spirit ("Marid Crusher"), the block name doesn't fit next to it
    @Override
    public Component getDisplayName() {
        return this.spiritData != null && this.job != null
                ? this.job.describe(this.spiritData.getString("id"), SpiritJob.factoryIdOf(this.spiritData))
                : this.getBlockName();
    }

    public void dropContents() {
        this.dropInventory();
        if (this.level != null && !this.gem.isEmpty()) {
            Block.popResource(this.level, this.worldPosition, this.gem);
            this.gem = ItemStack.EMPTY;
            this.readSpirit();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.gem = tag.contains("gem") ? ItemStack.parseOptional(registries, tag.getCompound("gem")) : ItemStack.EMPTY;
        this.readSpirit();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.gem.isEmpty()) {
            tag.put("gem", this.gem.save(registries));
        }
    }
}
