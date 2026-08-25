package com.toroidalworld;

import com.toroidalworld.gen.LoopedChunkGenerator;
import com.toroidalworld.gen.LoopedFlatChunkGenerator;
import com.toroidalworld.gen.WorldLoopGenerators;
import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.platform.FabricPlatform;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.WorldShapeSetup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ToroidalWorldFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ToroidalWorld.LOGGER.info("Toroidal World initializing");
        Platforms.set(new FabricPlatform());
        WorldShapeSetup.registerAll();

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_ID),
                LoopedChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_FLAT_ID),
                LoopedFlatChunkGenerator.CODEC);

        PayloadTypeRegistry.playS2C().register(WrappingSettingsPayload.TYPE, WrappingSettingsPayload.STREAM_CODEC);
    }
}
