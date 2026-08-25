package com.toroidalworld.gen;

import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.shape.FlatShape;
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
                    SHAPE_CODEC.fieldOf(WRAPPING_KEY).forGetter(LoopedFlatChunkGenerator::shape)
            ).apply(instance, instance.stable(LoopedFlatChunkGenerator::new)));

    private final FlatShape shape;
    private final WorldLoopTransformer transformer;

    public LoopedFlatChunkGenerator(FlatLevelGeneratorSettings settings, FlatShape shape) {
        super(settings);
        this.shape = shape;
        this.transformer = WorldFolds.of(shape);
    }

    @Override
    public FlatShape shape() {
        return this.shape;
    }

    @Override
    public WorldLoopTransformer transformer() {
        return this.transformer;
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
