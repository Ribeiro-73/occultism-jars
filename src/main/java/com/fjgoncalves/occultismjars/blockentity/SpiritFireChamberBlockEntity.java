package com.fjgoncalves.occultismjars.blockentity;

import org.jspecify.annotations.Nullable;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.block.SpiritFireChamberBlock;
import com.klikli_dev.occultism.crafting.recipe.SpiritFireRecipe;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import com.klikli_dev.occultism.registry.OccultismSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// no ticking and no menu: an item offered by a hopper or pipe is turned into its spirit fire result
// right away, and the results wait in a small buffer until something pulls them out
public class SpiritFireChamberBlockEntity extends BlockEntity {

    private static final int OUTPUT_SLOTS = 4;

    private final ItemStacksResourceHandler outputs = new ItemStacksResourceHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };

    // slot 0 is where things go in (always shows empty), slots 1..4 are the results
    private final ResourceHandler<ItemResource> automationView = new AutomationView();

    // the chime only plays once the burn really happens, not for a pipe that is just asking
    private final RootCommitJournal burnSound = new RootCommitJournal(this::playBurnSound);

    // last recipe lookup, so a stream of the same item doesn't search every time
    private ItemStack cachedInput = ItemStack.EMPTY;
    private RecipeHolder<SpiritFireRecipe> cachedRecipe;

    private long lastSound;

    public SpiritFireChamberBlockEntity(BlockPos pos, BlockState state) {
        super(OccultismJars.SPIRIT_FIRE_CHAMBER_BE.get(), pos, state);
    }

    public ResourceHandler<ItemResource> getAutomationView() {
        return this.automationView;
    }

    @Nullable
    private SpiritFireRecipe recipeFor(ItemStack input) {
        if (!(this.level instanceof ServerLevel server) || input.isEmpty()) {
            return null;
        }
        if (!ItemStack.isSameItemSameComponents(this.cachedInput, input)) {
            this.cachedInput = input.copyWithCount(1);
            this.cachedRecipe = server.recipeAccess()
                    .getRecipeFor(OccultismRecipes.SPIRIT_FIRE_TYPE.get(), new SingleRecipeInput(this.cachedInput), server)
                    .orElse(null);
        }
        return this.cachedRecipe == null ? null : this.cachedRecipe.value();
    }

    private boolean isLit() {
        return SpiritFireChamberBlock.isLit(this.getBlockState());
    }

    // how many of this result the buffer can still take
    private int roomFor(ItemResource result) {
        int room = 0;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemResource held = this.outputs.getResource(slot);
            if (held.isEmpty() || held.equals(result)) {
                room += result.getMaxStackSize() - this.outputs.getAmountAsInt(slot);
            }
        }
        return room;
    }

    private int burn(ItemResource input, int amount, TransactionContext transaction) {
        if (!this.isLit()) {
            return 0;
        }
        ItemStack stack = input.toStack();
        SpiritFireRecipe recipe = this.recipeFor(stack);
        if (recipe == null) {
            return 0;
        }
        ItemStack result = recipe.assemble(new SingleRecipeInput(stack));
        if (result.isEmpty()) {
            return 0;
        }
        ItemResource resultResource = ItemResource.of(result);
        int burned = Math.min(amount, this.roomFor(resultResource) / result.getCount());
        if (burned <= 0) {
            return 0;
        }
        int produced = result.getCount() * burned;
        int inserted = 0;
        for (int slot = 0; slot < OUTPUT_SLOTS && inserted < produced; slot++) {
            inserted += this.outputs.insert(slot, resultResource, produced - inserted, transaction);
        }
        this.burnSound.updateSnapshots(transaction);
        return burned;
    }

    // Occultism's ritual chime, at most once a second so a busy pipe doesn't drown everything
    private void playBurnSound() {
        if (this.level == null) {
            return;
        }
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
            ItemStack stack = this.outputs.getResource(slot).toStack(this.outputs.getAmountAsInt(slot));
            if (!stack.isEmpty()) {
                Block.popResource(this.level, this.worldPosition, stack);
                this.outputs.set(slot, ItemResource.EMPTY, 0);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        this.dropContents();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("outputs").ifPresent(this.outputs::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.outputs.serialize(output.child("outputs"));
    }

    private final class AutomationView implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return OUTPUT_SLOTS + 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return index == 0 ? ItemResource.EMPTY : outputs.getResource(index - 1);
        }

        @Override
        public long getAmountAsLong(int index) {
            return index == 0 ? 0 : outputs.getAmountAsLong(index - 1);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return index == 0 ? resource.getMaxStackSize() : outputs.getCapacityAsLong(index - 1, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return index == 0 && isLit() && recipeFor(resource.toStack()) != null;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return index == 0 && !resource.isEmpty() && amount > 0 ? burn(resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return index == 0 ? 0 : outputs.extract(index - 1, resource, amount, transaction);
        }
    }
}
