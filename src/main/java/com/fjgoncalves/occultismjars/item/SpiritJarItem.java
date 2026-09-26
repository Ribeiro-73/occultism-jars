package com.fjgoncalves.occultismjars.item;

import java.util.function.Consumer;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.content.SpiritJob;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class SpiritJarItem extends BlockItem {

    public SpiritJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        CompoundTag stored = stack.get(ModComponents.CONTAINED_SPIRIT.get());
        if (stored != null) {
            SpiritJob job = SpiritJob.byName(stored.getStringOr("job", ""));
            if (job == null) {
                job = SpiritJob.CRUSHER;
            }
            tooltip.accept(Component.translatable("tooltip.occultismjars.contains", job.describe(stored.getStringOr("entity", ""), SpiritJob.factoryIdOf(stored.getCompoundOrEmpty("data"))))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.occultismjars.spirit_jar.capture").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.occultismjars.spirit_jar.release").withStyle(ChatFormatting.DARK_GRAY));
    }
}
