package com.toroidalworld;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ToroidalWorld.MODID)
public class ToroidalWorldNeoForge {
    public ToroidalWorldNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ToroidalWorld.LOGGER.info("Toroidal World initializing");

        WorldLoop.init(modEventBus, modContainer);
    }
}
