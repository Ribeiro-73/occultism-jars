package com.fjgoncalves.occultismjars.item;

import java.util.List;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.content.SpiritJob;
import com.fjgoncalves.occultismjars.content.SpiritType;

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
            tooltip.add(Component.translatable("tooltip.occultismjars.contains", describe(stored))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.occultismjars.spirit_jar.capture").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.occultismjars.spirit_jar.release").withStyle(ChatFormatting.DARK_GRAY));
    }

    // "Foliot Crusher", "Djinni Smelter"...
    public static Component describe(CompoundTag stored) {
        String entity = stored.getString("entity");
        Component spirit;
        if (entity.isEmpty()) {
            SpiritType type = SpiritType.byTier(stored.getInt("tier"));
            spirit = Component.translatable("entity.occultism." + (type != null ? type.spirit() : "foliot"));
        } else {
            spirit = Component.translatable("entity." + entity.replace(':', '.'));
        }
        SpiritJob job = SpiritJob.byName(stored.getString("job"));
        if (job == null) {
            job = SpiritJob.CRUSHER;
        }
        return Component.translatable("tooltip.occultismjars.spirit_name", spirit,
                Component.translatable("job.occultismjars." + job.getName()));
    }
}
