package com.fjgoncalves.occultismjars.blockentity;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.content.SpiritJob;
import com.klikli_dev.occultism.registry.OccultismItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

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
        CompoundTag tag = gemSpirit(stack);
        if (tag == null) {
            return false;
        }
        SpiritJob job = SpiritJob.of(tag);
        if (job == null) {
            return false;
        }
        int tier = job.tierOf(tag);
        return tier >= 1 && tier <= job.maxTier();
    }

    // the gem keeps the entity type apart from its data; put the "id" back so it reads like a saved entity
    @Nullable
    private static CompoundTag gemSpirit(ItemStack gem) {
        TypedEntityData<EntityType<?>> data = gem.get(DataComponents.ENTITY_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTagWithoutId();
        tag.putString("id", EntityType.getKey(data.type()).toString());
        return tag;
    }

    public ItemStack getGem() {
        return this.gem;
    }

    public boolean hasGem() {
        return !this.gem.isEmpty();
    }

    // a spirit caught mid-job keeps the item in its hand: that becomes the base's input
    public void insertGem(ItemStack stack) {
        this.gem = stack.copyWithCount(1);
        this.readSpirit();
        if (this.holdsWork() && this.getStack(INPUT_SLOT).isEmpty()) {
            ItemStack held = this.heldItem();
            if (!held.isEmpty()) {
                this.setHeldItem(ItemStack.EMPTY);
                this.setStack(INPUT_SLOT, held);
            }
        }
        this.onSpiritChanged();
    }

    // the input goes back into the spirit's hand, so it carries on with it once released;
    // whatever it can't hold stays in the input slot for the caller to hand back
    public ItemStack removeGem() {
        this.returnInputToSpirit();
        ItemStack taken = this.gem;
        this.gem = ItemStack.EMPTY;
        this.readSpirit();
        this.onSpiritChanged();
        return taken;
    }

    private void returnInputToSpirit() {
        ItemStack input = this.getStack(INPUT_SLOT);
        if (!input.isEmpty() && this.holdsWork() && this.heldItem().isEmpty()) {
            this.setHeldItem(input.copy());
            this.setStack(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    // working spirits keep what they're processing in their main hand; partners don't work that way
    private boolean holdsWork() {
        return this.level != null && this.job != null && this.job != SpiritJob.PARTNER;
    }

    // an Occultism spirit's main hand is slot 0 of its own "inventory", not the vanilla equipment
    private ItemStack heldItem() {
        ItemStacksResourceHandler inventory = spiritInventory(this.spiritData, this.level.registryAccess());
        return inventory.getResource(0).toStack(inventory.getAmountAsInt(0));
    }

    private void setHeldItem(ItemStack stack) {
        HolderLookup.Provider registries = this.level.registryAccess();
        this.updateSpiritData(tag -> {
            ItemStacksResourceHandler inventory = spiritInventory(tag, registries);
            inventory.set(0, ItemResource.of(stack), stack.getCount());
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
            inventory.serialize(output);
            tag.put("inventory", output.buildResult());
        });
    }

    private static ItemStacksResourceHandler spiritInventory(CompoundTag spirit, HolderLookup.Provider registries) {
        ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(1);
        spirit.getCompound("inventory").ifPresent(tag ->
                inventory.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag)));
        return inventory;
    }

    private void readSpirit() {
        this.spiritData = null;
        this.job = null;
        this.tier = 0;
        CompoundTag tag = gemSpirit(this.gem);
        if (tag == null) {
            return;
        }
        SpiritJob found = SpiritJob.of(tag);
        if (found == null) {
            return;
        }
        this.spiritData = tag;
        this.job = found;
        this.tier = found.tierOf(tag);
    }

    // the partner's heart cooldown lives in its own data, so it follows the gem around
    @Override
    protected void updateSpiritData(Consumer<CompoundTag> edit) {
        TypedEntityData<EntityType<?>> data = this.gem.get(DataComponents.ENTITY_DATA);
        if (data == null) {
            return;
        }
        CompoundTag tag = data.copyTagWithoutId();
        edit.accept(tag);
        this.gem.set(DataComponents.ENTITY_DATA, TypedEntityData.of(data.type(), tag));
        this.readSpirit();
        this.setChanged();
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
                ? this.job.describe(this.spiritData.getStringOr("id", ""), SpiritJob.factoryIdOf(this.spiritData))
                : this.getBlockName();
    }

    public void dropContents() {
        this.returnInputToSpirit();
        this.dropInventory();
        if (this.level != null && !this.gem.isEmpty()) {
            Block.popResource(this.level, this.worldPosition, this.gem);
            this.gem = ItemStack.EMPTY;
            this.readSpirit();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        this.dropContents();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.gem = input.read("gem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.readSpirit();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.gem.isEmpty()) {
            output.store("gem", ItemStack.CODEC, this.gem);
        }
    }
}
