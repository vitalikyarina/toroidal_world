package com.toroidalworld.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.options.WorldLoopBounds;
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

// The vanilla noise generator, plus the bounds this dimension wraps at.
//
// The bounds have to travel with the generator, because that is the only thing a world keeps: the world type and the
// world shape are both read once, while the world is being created, and what is written out is the generator they
// produced. So this is where "does this level wrap, and how wide" survives a restart.
//
// It takes a BiomeSource and NoiseGeneratorSettings rather than fixing them, which is exactly what lets the Looped
// shape compose with any noise-based world type: Default, Large Biomes and Amplified differ only by the settings,
// Single Biome only by the biome source, and this generator simply adopts whichever the chosen type produced.
public class LoopedChunkGenerator extends NoiseBasedChunkGenerator implements ShapedChunkGenerator {
    private static final String BIOME_SOURCE_KEY = "biome_source";

    public static final MapCodec<LoopedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf(BIOME_SOURCE_KEY).forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf(SETTINGS_KEY).forGetter(NoiseBasedChunkGenerator::generatorSettings),
                    WorldLoopBounds.CODEC.fieldOf(WRAPPING_KEY).forGetter(LoopedChunkGenerator::wrapping)
            ).apply(instance, instance.stable(LoopedChunkGenerator::new)));

    // Past this many columns per heightmap type the cache is dropped whole rather than grown without bound: a single
    // /locate over the full search radius touches on the order of tens of thousands of distinct wrapped columns, and a
    // clear costs less than tracking an eviction order for a cache that is only a speed-up.
    private static final int BASE_HEIGHT_CACHE_CAP = 1 << 18;

    private final WorldLoopBounds wrapping;
    private final WorldLoopTransformer transformer;

    // The whole density field is periodic with period = world width, so a column and its wrapped twin return the same
    // base height. A /locate spiral folds thousands of raw columns onto that far smaller set of wrapped columns, so
    // keying by the wrapped column turns most of those queries into a lookup. Per heightmap type, because getBaseHeight
    // answers a different height for each. Concurrent because spawn selection runs it off worker threads.
    private final List<Map<Long, Integer>> baseHeightCache;

    public LoopedChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings,
            WorldLoopBounds wrapping) {
        super(biomeSource, settings);
        this.wrapping = wrapping;
        this.transformer = new WorldLoopTransformer(wrapping);

        List<Map<Long, Integer>> caches = new ArrayList<>();
        for (int i = 0; i < Heightmap.Types.values().length; i++) {
            caches.add(new ConcurrentHashMap<>());
        }
        this.baseHeightCache = List.copyOf(caches);
    }

    @Override
    public WorldLoopBounds wrapping() {
        return this.wrapping;
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

    // The heavy per-column work sits one level down in iterateNoiseColumn (a whole-height noise fill), so the wrapped
    // twin is caught here, before super delegates to it. A miss falls through to the real query and is remembered under
    // its wrapped column; a hit never builds a NoiseChunk at all.
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

    // Height queries reach the noise through here without ever passing a chunk step, so nothing would have bound the
    // transformer: spawn selection and structure placement would read vanilla, non-periodic noise and answer for terrain
    // that never gets generated. getBaseHeight and getBaseColumn both funnel into this method, so binding it covers both.
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
