package com.toroidalworld;

import com.toroidalworld.gen.LoopedChunkGenerator;
import com.toroidalworld.gen.LoopedFlatChunkGenerator;
import com.toroidalworld.gen.WorldLoopGenerators;
import com.toroidalworld.gen.WorldLoopTicketTypes;
import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.platform.FabricPlatform;
import com.toroidalworld.platform.Platforms;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

// The Fabric half of the loader seam — the same wiring WorldLoop does on NeoForge: platform first, then the game
// registries and the bounds payload. The NeoForge-only rewriters (auxiliary light, block particle position) have no
// Fabric counterpart to rewrite, so nothing registers them here.
public class ToroidalWorldFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ToroidalWorld.LOGGER.info("Toroidal World initializing");
        Platforms.set(new FabricPlatform());

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_ID),
                LoopedChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_FLAT_ID),
                LoopedFlatChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.TICKET_TYPE,
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopTicketTypes.SEAM_GENERATION_ID),
                WorldLoopTicketTypes.SEAM_GENERATION);

        PayloadTypeRegistry.playS2C().register(WrappingSettingsPayload.TYPE, WrappingSettingsPayload.STREAM_CODEC);
    }
}
