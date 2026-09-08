package com.toroidalworld.engine.gen;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.GenerationOptions;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
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
                    SHAPE_CODEC.fieldOf(WRAPPING_KEY).forGetter(LoopedFlatChunkGenerator::shape),
                    GENERATION_OPTIONS_CODEC.forGetter(LoopedFlatChunkGenerator::generationOptions)
            ).apply(instance, instance.stable(LoopedFlatChunkGenerator::new)));

    private final FlatShape shape;
    private final GenerationOptions generationOptions;
    private final WorldFold transformer;

    public LoopedFlatChunkGenerator(FlatLevelGeneratorSettings settings, FlatShape shape,
            GenerationOptions generationOptions) {
        super(settings);
        this.shape = shape;
        this.generationOptions = generationOptions;
        this.transformer = WorldFolds.of(shape, generationOptions);
    }

    @Override
    public FlatShape shape() {
        return this.shape;
    }

    @Override
    public GenerationOptions generationOptions() {
        return this.generationOptions;
    }

    @Override
    public WorldFold transformer() {
        return this.transformer;
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
