package com.fjgoncalves.occultismjars.block;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.fjgoncalves.occultismjars.blockentity.SpiritFireChamberBlockEntity;
import com.klikli_dev.occultism.api.common.data.ColorBlockState;
import com.klikli_dev.occultism.common.block.SpiritFireBlock;
import com.klikli_dev.occultism.registry.OccultismItems;
import com.klikli_dev.occultism.registry.OccultismTags;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;

// a stone and gold frame with Occultism's spirit fire inside; items piped in are converted on the spot
public class SpiritFireChamberBlock extends Block implements EntityBlock {

    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    // same property as the real spirit fire, so chalk colours and tints line up with it
    public static final EnumProperty<ColorBlockState> COLOR = SpiritFireBlock.COLOR;

    // the frame stands 1 pixel in from every side
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    private static Map<Item, ColorBlockState> chalkColors;

    public SpiritFireChamberBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(STAGE, Stage.EMPTY)
                .setValue(COLOR, ColorBlockState.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, COLOR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static boolean isLit(BlockState state) {
        return state.getValue(STAGE) == Stage.LIT;
    }

    // lit like the real thing: datura first, then a flint and steel (or anything else that lights fires)
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        Stage stage = state.getValue(STAGE);

        if (stage == Stage.EMPTY && stack.is(OccultismTags.Items.START_SPIRIT_FIRE)) {
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, state.setValue(STAGE, Stage.DATURA));
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (stage == Stage.DATURA && stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)) {
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, state.setValue(STAGE, Stage.LIT));
                if (stack.isDamageableItem()) {
                    stack.hurtAndBreak(1, player, hand);
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    stack.consume(1, player);
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            return InteractionResult.SUCCESS;
        }

        ColorBlockState color = stage == Stage.LIT ? chalkColor(stack) : null;
        if (color != null && color != state.getValue(COLOR)) {
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, state.setValue(COLOR, color));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    // changed your mind before lighting it: shift + right-click takes the datura back
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() || state.getValue(STAGE) != Stage.DATURA) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, state.setValue(STAGE, Stage.EMPTY));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OccultismItems.DATURA.get()));
        }
        return InteractionResult.SUCCESS;
    }

    // the same chalks the spirit fire reacts to
    @Nullable
    private static ColorBlockState chalkColor(ItemStack stack) {
        if (chalkColors == null) {
            chalkColors = Map.ofEntries(
                    Map.entry(OccultismItems.CHALK_WHITE.get(), ColorBlockState.WHITE),
                    Map.entry(OccultismItems.CHALK_LIGHT_GRAY.get(), ColorBlockState.LIGHT_GRAY),
                    Map.entry(OccultismItems.CHALK_GRAY.get(), ColorBlockState.GRAY),
                    Map.entry(OccultismItems.CHALK_BLACK.get(), ColorBlockState.BLACK),
                    Map.entry(OccultismItems.CHALK_BROWN.get(), ColorBlockState.BROWN),
                    Map.entry(OccultismItems.CHALK_RED.get(), ColorBlockState.RED),
                    Map.entry(OccultismItems.CHALK_ORANGE.get(), ColorBlockState.ORANGE),
                    Map.entry(OccultismItems.CHALK_YELLOW.get(), ColorBlockState.YELLOW),
                    Map.entry(OccultismItems.CHALK_LIME.get(), ColorBlockState.LIME),
                    Map.entry(OccultismItems.CHALK_GREEN.get(), ColorBlockState.GREEN),
                    Map.entry(OccultismItems.CHALK_CYAN.get(), ColorBlockState.CYAN),
                    Map.entry(OccultismItems.CHALK_LIGHT_BLUE.get(), ColorBlockState.LIGHT_BLUE),
                    Map.entry(OccultismItems.CHALK_BLUE.get(), ColorBlockState.BLUE),
                    Map.entry(OccultismItems.CHALK_PURPLE.get(), ColorBlockState.PURPLE),
                    Map.entry(OccultismItems.CHALK_MAGENTA.get(), ColorBlockState.MAGENTA),
                    Map.entry(OccultismItems.CHALK_PINK.get(), ColorBlockState.PINK),
                    Map.entry(OccultismItems.CHALK_RAINBOW.get(), ColorBlockState.RAINBOW),
                    Map.entry(OccultismItems.CHALK_VOID.get(), ColorBlockState.VOID));
        }
        return chalkColors.get(stack.getItem());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiritFireChamberBlockEntity(pos, state);
    }


    public enum Stage implements StringRepresentable {
        EMPTY("empty"),
        DATURA("datura"),
        LIT("lit");

        private final String name;

        Stage(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
