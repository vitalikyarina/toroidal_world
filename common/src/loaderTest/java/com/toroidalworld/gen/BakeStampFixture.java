package com.toroidalworld.gen;

import java.util.List;
import java.util.OptionalLong;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.blending.Blender;

final class BakeStampFixture {
    static final String TEST_NAMESPACE = "toroidal_world_test";

    static final int OVERWORLD_CHUNK_WIDTH = 32;

    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;
    private static final float NO_AMBIENT_LIGHT = 0.0F;
    private static final int NO_MONSTER_LIGHT = 0;

    private static final String UNUSED_IN_THIS_HARNESS = "The bake fixture never generates";

    static FlatShape squareTorus(int chunkWidth) {
        return FlatShape.latticeTorus(WorldLoopBounds.ofWidth(chunkWidth), FlatShape.NO_SKEW);
    }

    static ResourceKey<LevelStem> stemKey(String path) {
        return ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(TEST_NAMESPACE, path));
    }

    static LevelStem stem(double coordinateScale, ChunkGenerator generator) {
        return new LevelStem(Holder.direct(dimensionType(coordinateScale)), generator);
    }

    static ChunkGenerator foreignGenerator() {
        return new ForeignChunkGenerator();
    }

    static ChunkGenerator shapedGenerator(FlatShape shape) {
        return new ShapedForeignChunkGenerator(shape);
    }

    static WorldDimensions selected(Map<ResourceKey<LevelStem>, LevelStem> stems) {
        return new WorldDimensions(stems);
    }

    static Registry<LevelStem> datapackRegistry(Map<ResourceKey<LevelStem>, LevelStem> stems) {
        WritableRegistry<LevelStem> registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());
        stems.forEach((key, stem) ->
                registry.register(key, stem, new RegistrationInfo(Optional.empty(), Lifecycle.stable())));
        return registry.freeze();
    }

    static @Nullable FlatShape shapeOf(Registry<LevelStem> baked, ResourceKey<LevelStem> key) {
        return ShapedChunkGenerator.wrappedShapeOf(baked.getOptional(key).orElseThrow().generator());
    }

    static int chunkWidth(FlatShape shape, Direction.Axis axis) {
        return shape.bounds().chunkWidth(axis);
    }

    static DimensionType dimensionType(double coordinateScale) {
        return new DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                true,
                coordinateScale,
                true,
                false,
                MIN_Y,
                HEIGHT,
                HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                NO_AMBIENT_LIGHT,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(NO_MONSTER_LIGHT),
                        NO_MONSTER_LIGHT));
    }

    private static class ForeignChunkGenerator extends ChunkGenerator {
        ForeignChunkGenerator() {
            super(new NoBiomeSource());
        }

        @Override
        protected MapCodec<? extends ChunkGenerator> codec() {
            throw new UnsupportedOperationException(UNUSED_IN_THIS_HARNESS);
        }

        @Override
        public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving) {
            throw new UnsupportedOperationException(UNUSED_IN_THIS_HARNESS);
        }

        @Override
        public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState,
                ChunkAccess chunk) {
            throw new UnsupportedOperationException(UNUSED_IN_THIS_HARNESS);
        }

        @Override
        public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
            throw new UnsupportedOperationException(UNUSED_IN_THIS_HARNESS);
        }

        @Override
        public int getGenDepth() {
            return HEIGHT;
        }

        @Override
        public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                StructureManager structureManager, ChunkAccess chunk) {
            throw new UnsupportedOperationException(UNUSED_IN_THIS_HARNESS);
        }

        @Override
        public int getSeaLevel() {
            return 0;
        }

        @Override
        public int getMinY() {
            return MIN_Y;
        }

        @Override
        public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor,
                RandomState randomState) {
            return MIN_Y;
        }

        @Override
        public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
            throw new UnsupportedOperationException(UNUSED_IN_THIS_HARNESS);
        }

        @Override
        public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
        }
    }

    private static final class ShapedForeignChunkGenerator extends ForeignChunkGenerator
            implements ShapedChunkGenerator {
        private final FlatShape shape;
        private final WorldFold transformer;

        private ShapedForeignChunkGenerator(FlatShape shape) {
            this.shape = shape;
            this.transformer = WorldFolds.of(shape);
        }

        @Override
        public FlatShape shape() {
            return this.shape;
        }

        @Override
        public WorldFold transformer() {
            return this.transformer;
        }

        @Override
        public ChunkGenerator unshaped() {
            return new ForeignChunkGenerator();
        }
    }

    private static final class NoBiomeSource extends BiomeSource {
        @Override
        protected MapCodec<? extends BiomeSource> codec() {
            throw new UnsupportedOperationException(UNUSED_IN_THIS_HARNESS);
        }

        @Override
        protected Stream<Holder<Biome>> collectPossibleBiomes() {
            return Stream.empty();
        }

        @Override
        public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
            throw new UnsupportedOperationException(UNUSED_IN_THIS_HARNESS);
        }
    }

    private BakeStampFixture() {
    }
}
