package com.fjgoncalves.occultismjars.menu;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.blockentity.SpiritWorkerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class SpiritWorkerMenu extends AbstractContainerMenu {

    private final SpiritWorkerBlockEntity worker;
    private final BlockPos pos;
    private final ContainerData data;

    public SpiritWorkerMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, resolve(playerInv, buf.readBlockPos()), new SimpleContainerData(2));
    }

    public SpiritWorkerMenu(int id, Inventory playerInv, SpiritWorkerBlockEntity worker) {
        this(id, playerInv, worker, worker.getDataAccess());
    }

    private SpiritWorkerMenu(int id, Inventory playerInv, SpiritWorkerBlockEntity worker, ContainerData data) {
        super(OccultismJars.SPIRIT_WORKER_MENU.get(), id);
        this.worker = worker;
        this.pos = worker.getBlockPos();
        this.data = data;

        ItemStacksResourceHandler inv = worker.getInventory();
        this.addSlot(new ResourceHandlerSlot(inv, inv::set, SpiritWorkerBlockEntity.INPUT_SLOT, 134, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return worker.canProcess(stack);
            }
        });
        this.addSlot(new ResourceHandlerSlot(inv, inv::set, SpiritWorkerBlockEntity.FIRST_OUTPUT_SLOT, 134, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new ResourceHandlerSlot(inv, inv::set, SpiritWorkerBlockEntity.FIRST_OUTPUT_SLOT + 1, 152, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }

        this.addDataSlots(this.data);
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    private static SpiritWorkerBlockEntity resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof SpiritWorkerBlockEntity found) {
            return found;
        }
        throw new IllegalStateException("No spirit worker at " + pos);
    }

    // client-side, for the portrait
    public CompoundTag getSpiritData() {
        return this.worker.getSpiritData();
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.worker.isRemoved()
                && player.level().getBlockEntity(this.pos) == this.worker
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < SpiritWorkerBlockEntity.SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, SpiritWorkerBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, SpiritWorkerBlockEntity.INPUT_SLOT, SpiritWorkerBlockEntity.INPUT_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
