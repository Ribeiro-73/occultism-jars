package com.fjgoncalves.occultismjars.blockentity;

import com.fjgoncalves.occultismjars.OccultismJars;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrusherJarBlockEntity extends BlockEntity {

    public CrusherJarBlockEntity(BlockPos pos, BlockState state) {
        super(OccultismJars.CRUSHER_JAR_BE.get(), pos, state);
    }
}
