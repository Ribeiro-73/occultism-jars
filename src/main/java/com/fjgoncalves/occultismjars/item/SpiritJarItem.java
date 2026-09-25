package com.fjgoncalves.occultismjars.item;

import java.util.List;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.content.SpiritJob;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class SpiritJarItem extends BlockItem {

    public SpiritJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        CompoundTag stored = stack.get(ModComponents.CONTAINED_SPIRIT.get());
        if (stored != null) {
            SpiritJob job = SpiritJob.byName(stored.getString("job"));
            if (job == null) {
                job = SpiritJob.CRUSHER;
            }
            tooltip.add(Component.translatable("tooltip.occultismjars.contains", job.describe(stored.getString("entity"), SpiritJob.factoryIdOf(stored.getCompound("data"))))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.occultismjars.spirit_jar.capture").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.occultismjars.spirit_jar.release").withStyle(ChatFormatting.DARK_GRAY));
    }
}
