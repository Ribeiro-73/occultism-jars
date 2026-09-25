package com.fjgoncalves.occultismjars.blockentity;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.Config;
import com.fjgoncalves.occultismjars.content.SpiritJob;
import com.fjgoncalves.occultismjars.menu.SpiritWorkerMenu;
import com.klikli_dev.occultism.Occultism;
import com.klikli_dev.occultism.crafting.recipe.CrushingRecipe;
import com.klikli_dev.occultism.crafting.recipe.CrystallizeRecipe;
import com.klikli_dev.occultism.crafting.recipe.TieredSingleRecipeInput;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import com.klikli_dev.occultism.registry.OccultismSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// shared by the jar and the holographic base: inventory, automation, the spirit's job, menu
public abstract class SpiritWorkerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int FIRST_OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 3;

    private int progress;
    private int maxProgress;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : maxProgress;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            } else {
                maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    protected final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // what hoppers/pipes see: insert into input, extract from outputs
    private final IItemHandler automationView = new AutomationView();

    // client-only caches for the renderers
    private Entity displayEntity;
    protected boolean displayDirty = true;
    private int spiritVersion;
    private Object renderCache;

    // last recipe lookup, see recipeFor
    private ItemStack cachedInput = ItemStack.EMPTY;
    private RecipeHolder<?> cachedRecipe;
    private int cachedTier;
    private int cachedVersion = -1;

    protected SpiritWorkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // full entity nbt (with "id") of the spirit doing the work, null when there is none
    @Nullable
    public abstract CompoundTag getSpiritData();

    @Nullable
    public abstract SpiritJob getJob();

    public abstract int getTier();

    protected abstract Component getBlockName();

    public boolean hasSpirit() {
        return this.getSpiritData() != null;
    }

    public IItemHandler getAutomationView() {
        return this.automationView;
    }

    public IItemHandler getInventory() {
        return this.inventory;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    protected void stopProgress() {
        this.progress = 0;
        this.maxProgress = 0;
    }

    // bumped whenever the spirit changes, so renderers know to rebuild what they cached
    public int getSpiritVersion() {
        return this.spiritVersion;
    }

    public Object getRenderCache() {
        return this.renderCache;
    }

    public void setRenderCache(Object cache) {
        this.renderCache = cache;
    }

    protected void onSpiritChanged() {
        this.displayDirty = true;
        this.spiritVersion++;
        this.stopProgress();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void dropInventory() {
        if (this.level == null) {
            return;
        }
        for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(this.level, this.worldPosition, stack);
                this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        Component name = this.getBlockName();
        CompoundTag data = this.getSpiritData();
        SpiritJob job = this.getJob();
        if (data == null || job == null) {
            return name;
        }
        return Component.translatable("container.occultismjars.spirit_worker", name, job.describe(data.getString("id")));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SpiritWorkerMenu(id, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpiritWorkerBlockEntity worker) {
        ItemStack input = worker.inventory.getStackInSlot(INPUT_SLOT);
        Work work = input.isEmpty() ? null : worker.findWork(input);
        if (work == null || work.result().isEmpty() || !worker.canFitInOutput(work.result())) {
            worker.stopProgress();
            return;
        }

        worker.maxProgress = work.time();
        worker.progress++;
        if (worker.progress % 40 == 0) {
            playWorkSound(level, pos, worker.getJob());
        }

        if (worker.progress >= work.time()) {
            worker.progress = 0;
            worker.inventory.extractItem(INPUT_SLOT, work.consumed(), false);
            worker.pushToOutput(work.result());
            worker.setChanged();
            playWorkSound(level, pos, worker.getJob());
        }
    }

    // one operation: what comes out, how many ticks it takes, how many inputs it eats
    private record Work(ItemStack result, int time, int consumed) {
    }

    // same numbers as the spirit doing the job in the world, read live from occultism-server.toml
    @Nullable
    private Work findWork(ItemStack input) {
        SpiritJob job = this.getJob();
        if (this.level == null || job == null || input.isEmpty()) {
            return null;
        }
        int tier = Mth.clamp(this.getTier(), 1, 4);
        var jobs = Occultism.SERVER_CONFIG.spiritJobs;
        float jarTime = Config.JAR_TIME_MULTIPLIER.get().floatValue();
        var registries = this.level.registryAccess();

        switch (job) {
            case CRUSHER -> {
                var settings = pick(tier, jobs.crusherFoliot, jobs.crusherDjinni, jobs.crusherAfrit, jobs.crusherMarid);
                RecipeHolder<?> holder = this.recipeFor(job, input, settings.tier.get());
                if (holder == null || !(holder.value() instanceof CrushingRecipe recipe)) {
                    return null;
                }
                float output = recipe.getIgnoreCrushingMultiplier() ? 1.0F : settings.outputMultiplier.get().floatValue();
                return work(recipe.getResultItem(registries), input, settings.operationCount.get(), output,
                        recipe.getCrushingTime() * settings.timeMultiplier.get().floatValue() * jarTime);
            }
            case CRYSTALLIZER -> {
                var settings = pick(tier, jobs.crystallizerFoliot, jobs.crystallizerDjinni, jobs.crystallizerAfrit, jobs.crystallizerMarid);
                RecipeHolder<?> holder = this.recipeFor(job, input, settings.tier.get());
                if (holder == null || !(holder.value() instanceof CrystallizeRecipe recipe)) {
                    return null;
                }
                float output = recipe.getIgnoreCrystallizeMultiplier() ? 1.0F : settings.outputMultiplier.get().floatValue();
                return work(recipe.getResultItem(registries), input, settings.operationCount.get(), output,
                        recipe.getCrystallizeTime() * settings.timeMultiplier.get().floatValue() * jarTime);
            }
            case SMELTER -> {
                var settings = pick(tier, jobs.smelterFoliot, jobs.smelterDjinni, jobs.smelterAfrit, jobs.smelterMarid);
                RecipeHolder<?> holder = this.recipeFor(job, input, 0);
                if (holder == null || !(holder.value() instanceof AbstractCookingRecipe recipe)) {
                    return null;
                }
                return work(recipe.getResultItem(registries), input, settings.operationCount.get(), 1.0F,
                        recipe.getCookingTime() * settings.timeMultiplier.get().floatValue() * jarTime);
            }
        }
        return null;
    }

    private static Work work(ItemStack recipeResult, ItemStack input, int operationCount, float outputMultiplier, float time) {
        int operations = Math.min(operationCount, input.getCount());
        ItemStack result = recipeResult.copy();
        result.setCount(Mth.floor(result.getCount() * operations * outputMultiplier));
        return new Work(result, Math.max(1, Mth.ceil(time)), operations);
    }

    private static <T> T pick(int tier, T foliot, T djinni, T afrit, T marid) {
        return switch (tier) {
            case 2 -> djinni;
            case 3 -> afrit;
            case 4 -> marid;
            default -> foliot;
        };
    }

    // the last lookup is remembered, so a stack of the same item doesn't search the recipes every tick
    @Nullable
    private RecipeHolder<?> recipeFor(SpiritJob job, ItemStack input, int recipeTier) {
        if (this.cachedVersion == this.spiritVersion && this.cachedTier == recipeTier
                && ItemStack.isSameItemSameComponents(this.cachedInput, input)) {
            return this.cachedRecipe;
        }
        RecipeHolder<?> found = this.lookupRecipe(job, input, recipeTier);
        this.cachedVersion = this.spiritVersion;
        this.cachedTier = recipeTier;
        this.cachedInput = input.copyWithCount(1);
        this.cachedRecipe = found;
        return found;
    }

    @Nullable
    private RecipeHolder<?> lookupRecipe(SpiritJob job, ItemStack input, int recipeTier) {
        RecipeManager recipes = this.level.getRecipeManager();
        return switch (job) {
            case CRUSHER -> recipes.getRecipeFor(OccultismRecipes.CRUSHING_TYPE.get(),
                    new TieredSingleRecipeInput(input, recipeTier), this.level).orElse(null);
            case CRYSTALLIZER -> recipes.getRecipeFor(OccultismRecipes.CRYSTALLIZE_TYPE.get(),
                    new TieredSingleRecipeInput(input, recipeTier), this.level).orElse(null);
            // furnace first, then the other cookers, like the smelter spirit
            case SMELTER -> {
                SingleRecipeInput single = new SingleRecipeInput(input);
                RecipeHolder<?> found = recipes.getRecipeFor(RecipeType.SMELTING, single, this.level).orElse(null);
                if (found == null) {
                    found = recipes.getRecipeFor(RecipeType.BLASTING, single, this.level).orElse(null);
                }
                if (found == null) {
                    found = recipes.getRecipeFor(RecipeType.SMOKING, single, this.level).orElse(null);
                }
                if (found == null) {
                    found = recipes.getRecipeFor(RecipeType.CAMPFIRE_COOKING, single, this.level).orElse(null);
                }
                yield found;
            }
        };
    }

    private static void playWorkSound(Level level, BlockPos pos, @Nullable SpiritJob job) {
        if (job == null || !Config.PLAY_CRUSHING_SOUND.get()) {
            return;
        }
        SoundEvent sound = switch (job) {
            case CRUSHER -> OccultismSounds.CRUNCHING.get();
            case SMELTER -> SoundEvents.FIRE_AMBIENT;
            case CRYSTALLIZER -> SoundEvents.AMETHYST_CLUSTER_STEP;
        };
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F + 0.5F * level.random.nextFloat());
    }

    private boolean canFitInOutput(ItemStack stack) {
        for (int slot = FIRST_OUTPUT_SLOT; slot < SLOT_COUNT; slot++) {
            ItemStack remainder = this.inventory.insertItem(slot, stack, true);
            if (remainder.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void pushToOutput(ItemStack stack) {
        for (int slot = FIRST_OUTPUT_SLOT; slot < SLOT_COUNT && !stack.isEmpty(); slot++) {
            stack = this.inventory.insertItem(slot, stack, false);
        }
    }

    // can the spirit inside do its job on this item at its tier
    public boolean canProcess(ItemStack stack) {
        Work work = this.findWork(stack);
        return work != null && !work.result().isEmpty();
    }

    @Nullable
    public Entity getDisplayEntity() {
        CompoundTag data = this.getSpiritData();
        if (data == null) {
            this.displayEntity = null;
            return null;
        }
        if (this.level == null || !this.level.isClientSide || !Config.RENDER_TRAPPED_SPIRIT.get()) {
            return null;
        }
        if (this.displayEntity == null || this.displayDirty) {
            this.displayDirty = false;
            try {
                this.displayEntity = EntityType.create(data, this.level).orElse(null);
            } catch (Exception e) {
                this.displayEntity = null;
            }
            if (this.displayEntity != null) {
                this.displayEntity.setNoGravity(true);
                this.displayEntity.setCustomName(null);
                BlockState state = this.getBlockState();
                float yaw = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        ? state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() : 0.0F;
                this.displayEntity.setYRot(yaw);
                if (this.displayEntity instanceof LivingEntity living) {
                    living.yBodyRot = yaw;
                    living.yBodyRotO = yaw;
                    living.yHeadRot = yaw;
                    living.yHeadRotO = yaw;
                }
                this.displayEntity.setPos(this.worldPosition.getX() + 0.5, this.worldPosition.getY(), this.worldPosition.getZ() + 0.5);
            }
        }
        return this.displayEntity;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("progress");
        if (tag.contains("inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        this.displayDirty = true;
        this.spiritVersion++;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // empty update packets get dropped, so always write something
        tag.putBoolean("hasSpirit", this.hasSpirit());
        tag.putInt("progress", this.progress);
        tag.put("inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private final class AutomationView implements IItemHandler {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != INPUT_SLOT || !canProcess(stack)) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == INPUT_SLOT ? ItemStack.EMPTY : inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT && canProcess(stack);
        }
    }
}
