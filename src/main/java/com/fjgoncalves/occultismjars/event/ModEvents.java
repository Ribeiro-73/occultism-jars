package com.fjgoncalves.occultismjars.event;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.block.HolographicBaseBlock;
import com.fjgoncalves.occultismjars.blockentity.HolographicBaseBlockEntity;

import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = OccultismJars.MODID)
public final class ModEvents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                OccultismJars.SPIRIT_JAR_BE.get(),
                (jar, side) -> jar.getAutomationView());

        // both halves of the base expose the lower half's inventory, so pipes can connect anywhere
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> {
                    var lower = state.getValue(HolographicBaseBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
                    return level.getBlockEntity(lower) instanceof HolographicBaseBlockEntity base
                            ? base.getAutomationView() : null;
                },
                OccultismJars.HOLOGRAPHIC_BASE.get());
    }
}
