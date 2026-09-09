package com.fjgoncalves.occultismjars.client;

import com.fjgoncalves.occultismjars.OccultismJars;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = OccultismJars.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OccultismJars.CRUSHER_JAR_BE.get(), CrusherJarRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(OccultismJars.CRUSHER_JAR_MENU.get(), CrusherJarScreen::new);
    }
}
