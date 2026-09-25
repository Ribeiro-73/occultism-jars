package com.fjgoncalves.occultismjars.menu;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.blockentity.SpiritJarBlockEntity;

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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SpiritJarMenu extends AbstractContainerMenu {

    private final SpiritJarBlockEntity jar;
    private final BlockPos pos;
    private final ContainerData data;

    public SpiritJarMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, resolve(playerInv, buf.readBlockPos()), new SimpleContainerData(2));
    }

    public SpiritJarMenu(int id, Inventory playerInv, SpiritJarBlockEntity jar) {
        this(id, playerInv, jar, jar.getDataAccess());
    }

    private SpiritJarMenu(int id, Inventory playerInv, SpiritJarBlockEntity jar, ContainerData data) {
        super(OccultismJars.SPIRIT_JAR_MENU.get(), id);
        this.jar = jar;
        this.pos = jar.getBlockPos();
        this.data = data;

        IItemHandler inv = jar.getInventory();
        this.addSlot(new SlotItemHandler(inv, SpiritJarBlockEntity.INPUT_SLOT, 134, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return jar.canCrush(stack);
            }
        });
        this.addSlot(new SlotItemHandler(inv, SpiritJarBlockEntity.FIRST_OUTPUT_SLOT, 134, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(inv, SpiritJarBlockEntity.FIRST_OUTPUT_SLOT + 1, 152, 54) {
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

    private static SpiritJarBlockEntity resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof SpiritJarBlockEntity found) {
            return found;
        }
        throw new IllegalStateException("No spirit jar at " + pos);
    }

    // client-side, for the portrait
    public CompoundTag getContainedTag() {
        return this.jar.getContainedTag();
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.jar.isRemoved()
                && player.level().getBlockEntity(this.pos) == this.jar
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

        if (index < SpiritJarBlockEntity.SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, SpiritJarBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, SpiritJarBlockEntity.INPUT_SLOT, SpiritJarBlockEntity.INPUT_SLOT + 1, false)) {
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
