package com.toroidalworld.gen;

import java.util.function.Supplier;

import com.toroidalworld.ToroidalWorld;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WorldLoopGenerators {
    public static final String TOROIDAL_ID = "toroidal";
    public static final String TOROIDAL_FLAT_ID = "toroidal_flat";

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ToroidalWorld.MODID);

    public static final Supplier<MapCodec<LoopedChunkGenerator>> TOROIDAL =
            CHUNK_GENERATORS.register(TOROIDAL_ID, () -> LoopedChunkGenerator.CODEC);

    public static final Supplier<MapCodec<LoopedFlatChunkGenerator>> TOROIDAL_FLAT =
            CHUNK_GENERATORS.register(TOROIDAL_FLAT_ID, () -> LoopedFlatChunkGenerator.CODEC);

    private WorldLoopGenerators() {
    }
}
