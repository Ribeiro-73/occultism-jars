package com.fjgoncalves.occultismjars.blockentity;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.content.SpiritJob;
import com.klikli_dev.occultism.registry.OccultismItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

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
        CompoundTag tag = data.copyTag();
        SpiritJob job = SpiritJob.of(tag);
        if (job == null) {
            return false;
        }
        int tier = job.tierOf(tag);
        return tier >= 1 && tier <= job.maxTier();
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
        if (this.holdsWork() && this.inventory.getStackInSlot(INPUT_SLOT).isEmpty()) {
            ItemStack held = this.heldItem();
            if (!held.isEmpty()) {
                this.setHeldItem(ItemStack.EMPTY);
                this.inventory.setStackInSlot(INPUT_SLOT, held);
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
        ItemStack input = this.inventory.getStackInSlot(INPUT_SLOT);
        if (!input.isEmpty() && this.holdsWork() && this.heldItem().isEmpty()) {
            this.setHeldItem(input.copy());
            this.inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    // working spirits keep what they're processing in their main hand; partners don't work that way
    private boolean holdsWork() {
        return this.level != null && this.job != null && this.job != SpiritJob.PARTNER;
    }

    // an Occultism spirit's main hand is slot 0 of its own "inventory", not the vanilla HandItems;
    // HandItems is still read as a fallback so anything left there by older builds isn't lost
    private ItemStack heldItem() {
        HolderLookup.Provider registries = this.level.registryAccess();
        ItemStack held = spiritInventory(this.spiritData, registries).getStackInSlot(0);
        if (held.isEmpty()) {
            ListTag hands = this.spiritData.getList("HandItems", Tag.TAG_COMPOUND);
            held = hands.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, hands.getCompound(0));
        }
        return held;
    }

    private void setHeldItem(ItemStack stack) {
        HolderLookup.Provider registries = this.level.registryAccess();
        this.updateSpiritData(tag -> {
            ItemStackHandler inventory = spiritInventory(tag, registries);
            inventory.setStackInSlot(0, stack);
            tag.put("inventory", inventory.serializeNBT(registries));
            ListTag hands = tag.getList("HandItems", Tag.TAG_COMPOUND);
            if (!hands.isEmpty()) {
                hands.set(0, new CompoundTag());
                tag.put("HandItems", hands);
            }
        });
    }

    private static ItemStackHandler spiritInventory(CompoundTag spirit, HolderLookup.Provider registries) {
        ItemStackHandler inventory = new ItemStackHandler(1);
        if (spirit.contains("inventory")) {
            inventory.deserializeNBT(registries, spirit.getCompound("inventory"));
        }
        return inventory;
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
        if (this.gem.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.ENTITY_DATA, this.gem, edit);
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
                ? this.job.describe(this.spiritData.getString("id"), SpiritJob.factoryIdOf(this.spiritData))
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
