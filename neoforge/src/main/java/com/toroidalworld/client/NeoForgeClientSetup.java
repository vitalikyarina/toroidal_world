package com.toroidalworld.client;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.client.shape.WorldShapeSetup;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = ToroidalWorld.MODID, value = Dist.CLIENT)
public final class NeoForgeClientSetup {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        WorldShapeSetup.registerAll();
    }

    private NeoForgeClientSetup() {
    }
}
