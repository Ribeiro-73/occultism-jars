package com.fjgoncalves.occultismjars.blockentity;

import com.fjgoncalves.occultismjars.OccultismJars;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrusherJarBlockEntity extends BlockEntity {

    private int tier;

    public CrusherJarBlockEntity(BlockPos pos, BlockState state) {
        super(OccultismJars.CRUSHER_JAR_BE.get(), pos, state);
    }

    public int getTier() {
        return this.tier;
    }

    public boolean isEmpty() {
        return this.tier == 0;
    }

    public void setTier(int tier) {
        this.tier = tier;
        this.setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tier = tag.getInt("tier");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("tier", this.tier);
    }
}
