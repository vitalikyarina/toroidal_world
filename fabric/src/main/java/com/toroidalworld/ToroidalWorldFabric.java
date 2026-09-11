package com.toroidalworld;

import com.toroidalworld.engine.gen.LoopedChunkGenerator;
import com.toroidalworld.engine.gen.LoopedFlatChunkGenerator;
import com.toroidalworld.engine.gen.WorldLoopGenerators;
import com.toroidalworld.engine.gen.WorldLoopTicketTypes;
import com.toroidalworld.engine.net.OpenMenuTranslation;
import com.toroidalworld.engine.net.WrappingSettingsPayload;
import com.toroidalworld.engine.seam.circumnavigation.WorldLoopCriteria;
import com.toroidalworld.platform.FabricPlatform;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.GenerationHookSetup;
import com.toroidalworld.shape.WorldOptionSetup;
import com.toroidalworld.shape.WorldShapeSetup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ToroidalWorldFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ToroidalWorld.LOGGER.info("Toroidal World initializing");
        Platforms.set(new FabricPlatform());
        WorldOptionSetup.registerAll();
        WorldShapeSetup.registerAll();
        GenerationHookSetup.registerAll();

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                Identifier.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_ID),
                LoopedChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                Identifier.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_FLAT_ID),
                LoopedFlatChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.TICKET_TYPE,
                Identifier.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopTicketTypes.SEAM_GENERATION_ID),
                WorldLoopTicketTypes.SEAM_GENERATION);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Identifier.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopCriteria.CIRCUMNAVIGATE_ID),
                WorldLoopCriteria.CIRCUMNAVIGATE);

        PayloadTypeRegistry.clientboundPlay().register(WrappingSettingsPayload.TYPE, WrappingSettingsPayload.STREAM_CODEC);

        OpenMenuTranslation.register();
    }
}
