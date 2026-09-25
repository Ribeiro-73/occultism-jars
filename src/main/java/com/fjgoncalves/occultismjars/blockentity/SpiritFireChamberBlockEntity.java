package com.fjgoncalves.occultismjars.blockentity;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.block.SpiritFireChamberBlock;
import com.klikli_dev.occultism.crafting.recipe.SpiritFireRecipe;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import com.klikli_dev.occultism.registry.OccultismSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// no ticking and no menu: an item offered by a hopper or pipe is turned into its spirit fire result
// right away, and the results wait in a small buffer until something pulls them out
public class SpiritFireChamberBlockEntity extends BlockEntity {

    private static final int OUTPUT_SLOTS = 4;

    private final ItemStackHandler outputs = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // slot 0 is where things go in (always shows empty), slots 1..4 are the results
    private final IItemHandler automationView = new AutomationView();

    // last recipe lookup, so a stream of the same item doesn't search every time
    private ItemStack cachedInput = ItemStack.EMPTY;
    private RecipeHolder<SpiritFireRecipe> cachedRecipe;

    private long lastSound;

    public SpiritFireChamberBlockEntity(BlockPos pos, BlockState state) {
        super(OccultismJars.SPIRIT_FIRE_CHAMBER_BE.get(), pos, state);
    }

    public IItemHandler getAutomationView() {
        return this.automationView;
    }

    @Nullable
    private SpiritFireRecipe recipeFor(ItemStack input) {
        if (this.level == null || input.isEmpty()) {
            return null;
        }
        if (!ItemStack.isSameItemSameComponents(this.cachedInput, input)) {
            this.cachedInput = input.copyWithCount(1);
            this.cachedRecipe = this.level.getRecipeManager()
                    .getRecipeFor(OccultismRecipes.SPIRIT_FIRE_TYPE.get(), new SingleRecipeInput(this.cachedInput), this.level)
                    .orElse(null);
        }
        return this.cachedRecipe == null ? null : this.cachedRecipe.value();
    }

    private boolean isLit() {
        return SpiritFireChamberBlock.isLit(this.getBlockState());
    }

    // how many of this result the buffer can still take
    private int roomFor(ItemStack result) {
        int room = 0;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack held = this.outputs.getStackInSlot(slot);
            if (held.isEmpty()) {
                room += result.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(held, result)) {
                room += held.getMaxStackSize() - held.getCount();
            }
        }
        return room;
    }

    private ItemStack burn(ItemStack stack, boolean simulate) {
        if (!this.isLit()) {
            return stack;
        }
        SpiritFireRecipe recipe = this.recipeFor(stack);
        if (recipe == null) {
            return stack;
        }
        ItemStack result = recipe.assemble(new SingleRecipeInput(stack.copyWithCount(1)), this.level.registryAccess());
        if (result.isEmpty()) {
            return stack;
        }
        int burned = Math.min(stack.getCount(), this.roomFor(result) / result.getCount());
        if (burned <= 0) {
            return stack;
        }
        if (!simulate) {
            ItemStack produced = result.copyWithCount(result.getCount() * burned);
            for (int slot = 0; slot < OUTPUT_SLOTS && !produced.isEmpty(); slot++) {
                produced = this.outputs.insertItem(slot, produced, false);
            }
            this.playBurnSound();
        }
        return stack.copyWithCount(stack.getCount() - burned);
    }

    // Occultism's ritual chime, at most once a second so a busy pipe doesn't drown everything
    private void playBurnSound() {
        long now = this.level.getGameTime();
        if (now - this.lastSound >= 20) {
            this.lastSound = now;
            this.level.playSound(null, this.worldPosition, OccultismSounds.START_RITUAL.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    public void dropContents() {
        if (this.level == null) {
            return;
        }
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack stack = this.outputs.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(this.level, this.worldPosition, stack);
                this.outputs.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("outputs")) {
            this.outputs.deserializeNBT(registries, tag.getCompound("outputs"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("outputs", this.outputs.serializeNBT(registries));
    }

    private final class AutomationView implements IItemHandler {
        @Override
        public int getSlots() {
            return OUTPUT_SLOTS + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? ItemStack.EMPTY : outputs.getStackInSlot(slot - 1);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 && !stack.isEmpty() ? burn(stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? ItemStack.EMPTY : outputs.extractItem(slot - 1, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && isLit() && recipeFor(stack) != null;
        }
    }
}
