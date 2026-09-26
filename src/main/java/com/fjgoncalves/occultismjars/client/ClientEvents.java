package com.fjgoncalves.occultismjars.client;

import java.util.List;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.block.SpiritFireChamberBlock;
import com.klikli_dev.occultism.common.block.SpiritFireBlock;
import com.klikli_dev.occultism.registry.OccultismBlocks;

import net.minecraft.client.color.block.BlockTintSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = OccultismJars.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OccultismJars.SPIRIT_JAR_BE.get(), SpiritJarRenderer::new);
        event.registerBlockEntityRenderer(OccultismJars.HOLOGRAPHIC_BASE_BE.get(), HolographicBaseRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(OccultismJars.SPIRIT_WORKER_MENU.get(), SpiritWorkerScreen::new);
    }

    // the chamber's fire is tinted by the real spirit fire, so chalk colours look exactly the same
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(fireTint(0), fireTint(1)), OccultismJars.SPIRIT_FIRE_CHAMBER.get());
    }

    private static BlockTintSource fireTint(int layer) {
        return state -> {
            if (!SpiritFireChamberBlock.isLit(state)) {
                return -1;
            }
            SpiritFireBlock fire = (SpiritFireBlock) OccultismBlocks.SPIRIT_FIRE.get();
            int color = fire.getColor(fire.defaultBlockState().setValue(SpiritFireBlock.COLOR,
                    state.getValue(SpiritFireChamberBlock.COLOR)), layer);
            return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
        };
    }
}
