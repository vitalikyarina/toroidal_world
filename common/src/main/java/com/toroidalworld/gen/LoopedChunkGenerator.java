package com.toroidalworld.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class LoopedChunkGenerator extends NoiseBasedChunkGenerator implements ShapedChunkGenerator {
    private static final String BIOME_SOURCE_KEY = "biome_source";

    public static final MapCodec<LoopedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf(BIOME_SOURCE_KEY).forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf(SETTINGS_KEY).forGetter(NoiseBasedChunkGenerator::generatorSettings),
                    SHAPE_CODEC.fieldOf(WRAPPING_KEY).forGetter(LoopedChunkGenerator::shape)
            ).apply(instance, instance.stable(LoopedChunkGenerator::new)));

    private static final int BASE_HEIGHT_CACHE_CAP = 1 << 18;

    private final FlatShape shape;
    private final WorldLoopTransformer transformer;

    private final List<Map<Long, Integer>> baseHeightCache;

    public LoopedChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, FlatShape shape) {
        super(biomeSource, settings);
        this.shape = shape;
        this.transformer = WorldFolds.of(shape);

        List<Map<Long, Integer>> caches = new ArrayList<>();
        for (int i = 0; i < Heightmap.Types.values().length; i++) {
            caches.add(new ConcurrentHashMap<>());
        }
        this.baseHeightCache = List.copyOf(caches);
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
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor,
            RandomState randomState) {
        long wrappedColumn = (((long) this.transformer.coords.x.wrap(x)) << 32)
                | (this.transformer.coords.z.wrap(z) & 0xFFFFFFFFL);
        Map<Long, Integer> cache = this.baseHeightCache.get(type.ordinal());

        Integer cached = cache.get(wrappedColumn);
        if (cached != null) {
            return cached;
        }

        int height = super.getBaseHeight(x, z, type, heightAccessor, randomState);
        if (cache.size() >= BASE_HEIGHT_CACHE_CAP) {
            cache.clear();
        }
        cache.put(wrappedColumn, height);
        return height;
    }

    @Override
    protected OptionalInt iterateNoiseColumn(
            LevelHeightAccessor heightAccessor,
            RandomState randomState,
            int blockX,
            int blockZ,
            @Nullable MutableObject<NoiseColumn> columnReference,
            @Nullable Predicate<BlockState> tester) {
        return GenerationTransformerContext.withTransformer(this.transformer,
                () -> super.iterateNoiseColumn(heightAccessor, randomState, blockX, blockZ, columnReference, tester));
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
}
