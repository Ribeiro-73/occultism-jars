package com.fjgoncalves.occultismjars.blockentity;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrusherJarBlockEntity extends BlockEntity {

    private CompoundTag contained;

    public CrusherJarBlockEntity(BlockPos pos, BlockState state) {
        super(OccultismJars.CRUSHER_JAR_BE.get(), pos, state);
    }

    public boolean isEmpty() {
        return this.contained == null;
    }

    public int getTier() {
        return this.contained == null ? 0 : this.contained.getInt("tier");
    }

    public void setContained(CompoundTag tag) {
        this.contained = tag;
        this.onContentsChanged();
    }

    public CompoundTag takeContained() {
        CompoundTag taken = this.contained;
        this.contained = null;
        this.onContentsChanged();
        return taken;
    }

    private void onContentsChanged() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.contained = tag.contains("contained") ? tag.getCompound("contained") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.contained != null) {
            tag.put("contained", this.contained);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag stored = input.get(ModComponents.CONTAINED_CRUSHER.get());
        this.contained = stored != null ? stored.copy() : null;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.contained != null) {
            components.set(ModComponents.CONTAINED_CRUSHER.get(), this.contained.copy());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("contained");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
