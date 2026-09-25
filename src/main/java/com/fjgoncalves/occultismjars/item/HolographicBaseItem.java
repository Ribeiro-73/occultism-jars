package com.fjgoncalves.occultismjars.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class HolographicBaseItem extends BlockItem {

    public HolographicBaseItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.occultismjars.holographic_base.insert").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.occultismjars.holographic_base.remove").withStyle(ChatFormatting.DARK_GRAY));
    }
}
