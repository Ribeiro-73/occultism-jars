package com.fjgoncalves.occultismjars.blockentity;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.content.SpiritJob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SpiritJarBlockEntity extends SpiritWorkerBlockEntity {

    // {tier, job, entity, data:<full entity nbt>}
    private CompoundTag contained;

    public SpiritJarBlockEntity(BlockPos pos, BlockState state) {
        super(OccultismJars.SPIRIT_JAR_BE.get(), pos, state);
    }

    public boolean isEmpty() {
        return this.contained == null;
    }

    @Nullable
    @Override
    public CompoundTag getSpiritData() {
        return this.contained == null ? null : this.contained.getCompound("data");
    }

    @Override
    public int getTier() {
        return this.contained == null ? 0 : this.contained.getInt("tier");
    }

    @Nullable
    @Override
    public SpiritJob getJob() {
        if (this.contained == null) {
            return null;
        }
        SpiritJob job = SpiritJob.byName(this.contained.getString("job"));
        return job != null ? job : SpiritJob.CRUSHER;
    }

    @Override
    protected Component getBlockName() {
        return Component.translatable("block.occultismjars.spirit_jar");
    }

    // spawn the spirit back into the world, holding whatever was mid-crush
    public void extractSpirit() {
        if (this.level == null || this.level.isClientSide || this.contained == null) {
            return;
        }
        EntityType.create(this.contained.getCompound("data"), this.level).ifPresent(spirit -> {
            ItemStack held = this.inventory.extractItem(INPUT_SLOT, Integer.MAX_VALUE, false);
            if (!held.isEmpty()) {
                if (spirit instanceof LivingEntity living) {
                    living.setItemInHand(InteractionHand.MAIN_HAND, held);
                } else {
                    Block.popResource(this.level, this.worldPosition.above(), held);
                }
            }
            spirit.moveTo(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                    spirit.getYRot(), spirit.getXRot());
            this.level.addFreshEntity(spirit);
        });
        this.level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.contained = null;
        this.onSpiritChanged();
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
        CompoundTag stored = input.get(ModComponents.CONTAINED_SPIRIT.get());
        if (stored == null) {
            this.contained = null;
            return;
        }
        this.contained = stored.copy();
        CompoundTag heldTag = this.contained.contains("heldItem") ? this.contained.getCompound("heldItem") : null;
        this.contained.remove("heldItem");
        if (heldTag != null && this.level != null && this.inventory.getStackInSlot(INPUT_SLOT).isEmpty()) {
            ItemStack held = ItemStack.parseOptional(this.level.registryAccess(), heldTag);
            if (!held.isEmpty()) {
                this.inventory.setStackInSlot(INPUT_SLOT, held);
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.contained != null) {
            components.set(ModComponents.CONTAINED_SPIRIT.get(), this.contained.copy());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("contained");
        tag.remove("hasSpirit");
    }
}
