package com.fjgoncalves.occultismjars.blockentity;

import org.jspecify.annotations.Nullable;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.content.SpiritJob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
        return this.contained == null ? null : this.contained.getCompoundOrEmpty("data");
    }

    @Override
    public int getTier() {
        return this.contained == null ? 0 : this.contained.getIntOr("tier", 0);
    }

    @Nullable
    @Override
    public SpiritJob getJob() {
        if (this.contained == null) {
            return null;
        }
        SpiritJob job = SpiritJob.byName(this.contained.getStringOr("job", ""));
        return job != null ? job : SpiritJob.CRUSHER;
    }

    @Override
    protected Component getBlockName() {
        return Component.translatable("block.occultismjars.spirit_jar");
    }

    // spawn the spirit back into the world, holding whatever was mid-crush
    public void extractSpirit() {
        if (this.level == null || this.level.isClientSide() || this.contained == null) {
            return;
        }
        Entity spirit = createEntity(this.contained.getCompoundOrEmpty("data"), this.level);
        if (spirit != null) {
            ItemStack held = this.takeInput();
            if (!held.isEmpty()) {
                if (spirit instanceof LivingEntity living) {
                    living.setItemInHand(InteractionHand.MAIN_HAND, held);
                } else {
                    Block.popResource(this.level, this.worldPosition.above(), held);
                }
            }
            spirit.snapTo(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                    spirit.getYRot(), spirit.getXRot());
            this.level.addFreshEntity(spirit);
        }
        this.level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.contained = null;
        this.onSpiritChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.contained = input.read("contained", CompoundTag.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("contained", CompoundTag.CODEC, this.contained);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        CompoundTag stored = components.get(ModComponents.CONTAINED_SPIRIT.get());
        if (stored == null) {
            this.contained = null;
            return;
        }
        this.contained = stored.copy();
        CompoundTag heldTag = this.contained.getCompound("heldItem").orElse(null);
        this.contained.remove("heldItem");
        if (heldTag != null && this.level != null && this.getStack(INPUT_SLOT).isEmpty()) {
            ItemStack held = ItemStack.OPTIONAL_CODEC
                    .parse(this.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), heldTag)
                    .result().orElse(ItemStack.EMPTY);
            if (!held.isEmpty()) {
                this.setStack(INPUT_SLOT, held);
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
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("contained");
        output.discard("hasSpirit");
    }
}
