package com.fjgoncalves.occultismjars.blockentity;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CrusherJarBlockEntity extends BlockEntity {

    public static final int INPUT_SLOT = 0;
    public static final int FIRST_OUTPUT_SLOT = 1;
    private static final int SLOT_COUNT = 3;
    private static final int MAX_PROGRESS = 100;

    private CompoundTag contained;
    private int progress;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT;
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
        this.progress = 0;
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

    // --- Processing ---------------------------------------------------------

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherJarBlockEntity jar) {
        ItemStack input = jar.inventory.getStackInSlot(INPUT_SLOT);

        if (level.getGameTime() % 40L == 0L) {
            LOGGER.info("[occultismjars] tick @ {} empty={} input={} progress={}",
                    pos, jar.isEmpty(), input, jar.progress);
        }

        if (jar.isEmpty()) {
            jar.progress = 0;
            return;
        }

        // TODO (next part): real occultism:crushing recipe + tier multiplier. For now: passthrough.
        ItemStack result = input.isEmpty() ? ItemStack.EMPTY : input.copyWithCount(1);

        if (result.isEmpty() || !jar.canFitInOutput(result)) {
            jar.progress = 0;
            return;
        }

        jar.progress++;
        if (jar.progress >= MAX_PROGRESS) {
            jar.progress = 0;
            jar.inventory.extractItem(INPUT_SLOT, 1, false);
            jar.pushToOutput(result);
            jar.setChanged();
            LOGGER.info("[occultismjars] moved 1 {} to output", result.getItem());
        }
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
            return slot == INPUT_SLOT ? inventory.insertItem(slot, stack, simulate) : stack;
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
            return slot == INPUT_SLOT && inventory.isItemValid(slot, stack);
        }
    }
}
