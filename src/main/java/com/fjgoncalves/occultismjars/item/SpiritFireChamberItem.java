package com.fjgoncalves.occultismjars.item;

import java.util.function.Consumer;

import com.fjgoncalves.occultismjars.block.SpiritFireChamberBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

public class SpiritFireChamberItem extends BlockItem {

    public SpiritFireChamberItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        // a chamber broken while lit keeps its fire
        SpiritFireChamberBlock.Stage stage = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                .get(SpiritFireChamberBlock.STAGE);
        if (stage == SpiritFireChamberBlock.Stage.LIT) {
            tooltip.accept(Component.translatable("tooltip.occultismjars.spirit_fire_chamber.lit").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.accept(Component.translatable("tooltip.occultismjars.spirit_fire_chamber.light").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.occultismjars.spirit_fire_chamber.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
