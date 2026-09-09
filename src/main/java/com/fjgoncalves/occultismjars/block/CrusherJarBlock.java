package com.fjgoncalves.occultismjars.block;

import com.fjgoncalves.occultismjars.blockentity.CrusherJarBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CrusherJarBlock extends Block implements EntityBlock {

    public CrusherJarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherJarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof CrusherJarBlockEntity jar) || jar.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            CompoundTag contained = jar.takeContained();
            CompoundTag entityData = contained.getCompound("data");

            EntityType.create(entityData, level).ifPresent(spirit -> {
                spirit.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, spirit.getYRot(), spirit.getXRot());
                level.addFreshEntity(spirit);
            });

            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
