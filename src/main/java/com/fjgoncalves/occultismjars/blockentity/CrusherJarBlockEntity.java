package com.fjgoncalves.occultismjars.blockentity;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrusherJarBlockEntity extends BlockEntity {

    private CompoundTag contained;

    /** Client-only: cached entity used to render a mini spirit inside the jar. */
    private Entity displayEntity;
    private boolean displayDirty = true;

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
        this.displayDirty = true;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Nullable
    public Entity getDisplayEntity() {
        if (this.level == null || !this.level.isClientSide) {
            return null;
        }
        if (this.contained == null) {
            this.displayEntity = null;
            return null;
        }
        if (this.displayEntity == null || this.displayDirty) {
            this.displayDirty = false;
            try {
                this.displayEntity = EntityType.create(this.contained.getCompound("data"), this.level).orElse(null);
            } catch (Exception e) {
                this.displayEntity = null;
            }
            if (this.displayEntity != null) {
                this.displayEntity.setNoGravity(true);
                this.displayEntity.setPos(this.worldPosition.getX() + 0.5, this.worldPosition.getY(), this.worldPosition.getZ() + 0.5);
            }
        }
        return this.displayEntity;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.contained = tag.contains("contained") ? tag.getCompound("contained") : null;
        this.displayDirty = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Always write a key so the client sync packet is never empty (empty packets are ignored).
        tag.putBoolean("hasCrusher", this.contained != null);
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
        tag.remove("hasCrusher");
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
