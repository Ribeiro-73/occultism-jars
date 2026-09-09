package com.fjgoncalves.occultismjars.blockentity;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.menu.CrusherJarMenu;
import com.klikli_dev.occultism.Occultism;
import com.klikli_dev.occultism.config.OccultismServerConfig;
import com.klikli_dev.occultism.crafting.recipe.CrushingRecipe;
import com.klikli_dev.occultism.crafting.recipe.TieredSingleRecipeInput;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import com.klikli_dev.occultism.registry.OccultismSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CrusherJarBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int FIRST_OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 3;

    private CompoundTag contained;
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

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** What hoppers / pipes see: insert into the input slot, extract from the output slots. */
    private final IItemHandler automationView = new AutomationView();

    /** Client-only: a frozen copy of the captured spirit, used to render it inside the jar. */
    private Entity displayEntity;
    private boolean displayDirty = true;

    public CrusherJarBlockEntity(BlockPos pos, BlockState state) {
        super(OccultismJars.CRUSHER_JAR_BE.get(), pos, state);
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

    public boolean isEmpty() {
        return this.contained == null;
    }

    @Nullable
    public CompoundTag getContainedTag() {
        return this.contained;
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
        this.stopProgress();
        this.onContentsChanged();
        return taken;
    }

    private void stopProgress() {
        this.progress = 0;
        this.maxProgress = 0;
    }

    private void onContentsChanged() {
        this.displayDirty = true;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Spawns the trapped spirit back into the world, handing it whatever the jar was crushing. */
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
        this.takeContained();
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
        return Component.translatable("block.occultismjars.crusher_jar");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CrusherJarMenu(id, playerInventory, this);
    }

    // --- Processing ---------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherJarBlockEntity jar) {
        if (jar.isEmpty()) {
            jar.stopProgress();
            return;
        }

        ItemStack input = jar.inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            jar.stopProgress();
            return;
        }

        var settings = crusherSettings(Mth.clamp(jar.getTier(), 1, 4));
        int tier = settings.tier.get();
        Optional<RecipeHolder<CrushingRecipe>> match = level.getRecipeManager()
                .getRecipeFor(OccultismRecipes.CRUSHING_TYPE.get(), new TieredSingleRecipeInput(input, tier), level);
        if (match.isEmpty()) {
            jar.stopProgress();
            return;
        }

        CrushingRecipe recipe = match.get().value();
        float outputMultiplier = recipe.getIgnoreCrushingMultiplier() ? 1.0F : settings.outputMultiplier.get().floatValue();
        int operations = Math.min(settings.operationCount.get(), input.getCount());
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        result.setCount(Mth.floor(result.getCount() * operations * outputMultiplier));

        if (result.isEmpty() || !jar.canFitInOutput(result)) {
            jar.stopProgress();
            return;
        }

        int needed = Math.max(1, Mth.ceil(recipe.getCrushingTime() * settings.timeMultiplier.get().floatValue()));
        jar.maxProgress = needed;
        jar.progress++;
        if (jar.progress % 40 == 0) {
            playCrunch(level, pos);
        }

        if (jar.progress >= needed) {
            jar.progress = 0;
            jar.inventory.extractItem(INPUT_SLOT, operations, false);
            jar.pushToOutput(result);
            jar.setChanged();
            playCrunch(level, pos);
        }
    }

    private static void playCrunch(Level level, BlockPos pos) {
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

    /** True when the jar's spirit has a matching occultism:crushing recipe for this item at its tier. */
    public boolean canCrush(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return false;
        }
        int tier = crusherSettings(Mth.clamp(this.getTier(), 1, 4)).tier.get();
        return this.level.getRecipeManager()
                .getRecipeFor(OccultismRecipes.CRUSHING_TYPE.get(), new TieredSingleRecipeInput(stack, tier), this.level)
                .isPresent();
    }

    // --- Rendering ---------------------------------------------------------

    @Nullable
    public Entity getDisplayEntity() {
        if (this.contained == null) {
            this.displayEntity = null;
            return null;
        }
        if (this.level == null || !this.level.isClientSide) {
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
                this.displayEntity.setYRot(-20.0F);
                this.displayEntity.setPos(this.worldPosition.getX() + 0.5, this.worldPosition.getY(), this.worldPosition.getZ() + 0.5);
            }
        }
        return this.displayEntity;
    }

    // --- Save / load -----------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.contained = tag.contains("contained") ? tag.getCompound("contained") : null;
        this.progress = tag.getInt("progress");
        if (tag.contains("inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        this.displayDirty = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Always write a key so the client sync packet is never empty (empty packets are ignored).
        tag.putBoolean("hasCrusher", this.contained != null);
        tag.putInt("progress", this.progress);
        tag.put("inventory", this.inventory.serializeNBT(registries));
        if (this.contained != null) {
            tag.put("contained", this.contained);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag stored = input.get(ModComponents.CONTAINED_CRUSHER.get());
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

    // --- Automation wrapper ---------------------------------------------

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
