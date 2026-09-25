package com.fjgoncalves.occultismjars.blockentity;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.Config;
import com.fjgoncalves.occultismjars.content.SpiritJob;
import com.fjgoncalves.occultismjars.menu.SpiritWorkerMenu;
import com.klikli_dev.occultism.Occultism;
import com.klikli_dev.occultism.config.OccultismServerConfig;
import com.klikli_dev.occultism.crafting.recipe.CrushingRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// shared by the jar and the holographic base: inventory, automation, crushing, menu
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
        if (!worker.hasSpirit() || worker.getJob() != SpiritJob.CRUSHER) {
            worker.stopProgress();
            return;
        }

        ItemStack input = worker.inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            worker.stopProgress();
            return;
        }

        var settings = crusherSettings(Mth.clamp(worker.getTier(), 1, 4));
        int tier = settings.tier.get();
        Optional<RecipeHolder<CrushingRecipe>> match = level.getRecipeManager()
                .getRecipeFor(OccultismRecipes.CRUSHING_TYPE.get(), new TieredSingleRecipeInput(input, tier), level);
        if (match.isEmpty()) {
            worker.stopProgress();
            return;
        }

        CrushingRecipe recipe = match.get().value();
        float outputMultiplier = recipe.getIgnoreCrushingMultiplier() ? 1.0F : settings.outputMultiplier.get().floatValue();
        int operations = Math.min(settings.operationCount.get(), input.getCount());
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        result.setCount(Mth.floor(result.getCount() * operations * outputMultiplier));

        if (result.isEmpty() || !worker.canFitInOutput(result)) {
            worker.stopProgress();
            return;
        }

        float timeFactor = settings.timeMultiplier.get().floatValue() * Config.JAR_TIME_MULTIPLIER.get().floatValue();
        int needed = Math.max(1, Mth.ceil(recipe.getCrushingTime() * timeFactor));
        worker.maxProgress = needed;
        worker.progress++;
        if (worker.progress % 40 == 0) {
            playCrunch(level, pos);
        }

        if (worker.progress >= needed) {
            worker.progress = 0;
            worker.inventory.extractItem(INPUT_SLOT, operations, false);
            worker.pushToOutput(result);
            worker.setChanged();
            playCrunch(level, pos);
        }
    }

    private static void playCrunch(Level level, BlockPos pos) {
        if (!Config.PLAY_CRUSHING_SOUND.get()) {
            return;
        }
        level.playSound(null, pos, OccultismSounds.CRUNCHING.get(), SoundSource.BLOCKS,
                1.0F, 1.0F + 0.5F * level.random.nextFloat());
    }

    private static OccultismServerConfig.SpiritJobSettings.TierSpiritSettings crusherSettings(int tier) {
        var jobs = Occultism.SERVER_CONFIG.spiritJobs;
        return switch (tier) {
            case 2 -> jobs.crusherDjinni;
            case 3 -> jobs.crusherAfrit;
            case 4 -> jobs.crusherMarid;
            default -> jobs.crusherFoliot;
        };
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

    // does the spirit have a crushing recipe for this item at its tier
    public boolean canCrush(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return false;
        }
        SpiritJob job = this.getJob();
        if (job != null && job != SpiritJob.CRUSHER) {
            return false;
        }
        int tier = crusherSettings(Mth.clamp(this.getTier(), 1, 4)).tier.get();
        return this.level.getRecipeManager()
                .getRecipeFor(OccultismRecipes.CRUSHING_TYPE.get(), new TieredSingleRecipeInput(stack, tier), this.level)
                .isPresent();
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
            if (slot != INPUT_SLOT || !canCrush(stack)) {
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
            return slot == INPUT_SLOT && canCrush(stack);
        }
    }
}
