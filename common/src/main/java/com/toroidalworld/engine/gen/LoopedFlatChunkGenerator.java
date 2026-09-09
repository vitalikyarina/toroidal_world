package com.toroidalworld.engine.gen;

import com.toroidalworld.core.CarriedShape;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class LoopedFlatChunkGenerator extends FlatLevelSource implements ShapedChunkGenerator {
    public static final MapCodec<LoopedFlatChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    FlatLevelGeneratorSettings.CODEC.fieldOf(SETTINGS_KEY).forGetter(LoopedFlatChunkGenerator::settings),
                    CARRIED_CODEC.forGetter(LoopedFlatChunkGenerator::carriedShape)
            ).apply(instance, instance.stable(LoopedFlatChunkGenerator::new)));

    private final CarriedShape carriedShape;

    public LoopedFlatChunkGenerator(FlatLevelGeneratorSettings settings, CarriedShape carriedShape) {
        super(settings);
        this.carriedShape = carriedShape;
    }

    @Override
    public CarriedShape carriedShape() {
        return this.carriedShape;
    }

    @Override
    public ChunkGenerator unshaped() {
        return new FlatLevelSource(settings());
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState,
            long legacyLevelSeed) {
        return stampTransformer(super.createState(structureSets, randomState, legacyLevelSeed));
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
}
