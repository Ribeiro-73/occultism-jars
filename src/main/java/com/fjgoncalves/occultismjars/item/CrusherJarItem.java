package com.fjgoncalves.occultismjars.item;

import java.util.List;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.content.CrusherType;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class CrusherJarItem extends BlockItem {

    public CrusherJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        CompoundTag stored = stack.get(ModComponents.CONTAINED_CRUSHER.get());
        if (stored != null) {
            CrusherType type = CrusherType.byTier(stored.getInt("tier"));
            String spirit = type != null ? type.spirit() : "foliot";
            tooltip.add(Component.translatable("tooltip.occultismjars.contains",
                            Component.translatable("entity.occultism." + spirit))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
