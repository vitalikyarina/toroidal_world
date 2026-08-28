package com.toroidalworld;

import com.toroidalworld.compat.aeronautics.AeronauticsTranslation;
import com.toroidalworld.compat.sable.SableMod;
import com.toroidalworld.config.WorldLoopConfig;
import com.toroidalworld.gen.LoopedChunkGenerator;
import com.toroidalworld.gen.LoopedFlatChunkGenerator;
import com.toroidalworld.gen.WorldLoopGenerators;
import com.toroidalworld.net.AuxiliaryLightTranslation;
import com.toroidalworld.net.BlockParticleTranslation;
import com.toroidalworld.platform.NeoForgePlatform;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.WorldShapeSetup;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WorldLoop {
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ToroidalWorld.MODID);

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        Platforms.set(new NeoForgePlatform(modContainer));
        WorldShapeSetup.registerAll();
        SableMod.register();
        AeronauticsTranslation.register();

        CHUNK_GENERATORS.register(WorldLoopGenerators.TOROIDAL_ID, () -> LoopedChunkGenerator.CODEC);
        CHUNK_GENERATORS.register(WorldLoopGenerators.TOROIDAL_FLAT_ID, () -> LoopedFlatChunkGenerator.CODEC);
        CHUNK_GENERATORS.register(modEventBus);

        AuxiliaryLightTranslation.register();
        BlockParticleTranslation.register();
        modContainer.registerConfig(ModConfig.Type.CLIENT, WorldLoopConfig.SPEC);

        if (Platforms.get().isClient() && !WorldLoopConfig.SPEC.isEmpty()) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    private WorldLoop() {
    }
}
