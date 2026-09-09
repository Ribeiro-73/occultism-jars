package com.fjgoncalves.occultismjars.block;

import com.fjgoncalves.occultismjars.blockentity.CrusherJarBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CrusherJarBlock extends Block implements EntityBlock {

    public CrusherJarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherJarBlockEntity(pos, state);
    }
}
