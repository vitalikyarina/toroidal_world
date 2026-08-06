package com.toroidalworld;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ToroidalWorld.MODID)
public class ToroidalWorld {
    public static final String MODID = "toroidal_world";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ToroidalWorld(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Toroidal World initializing");

        WorldLoop.init(modEventBus, modContainer);
    }
}
