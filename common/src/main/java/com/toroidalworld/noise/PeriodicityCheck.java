package com.toroidalworld.noise;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.toroidalworld.core.WorldLoopTransformer;
import com.mojang.logging.LogUtils;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public final class PeriodicityCheck {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int[] SAMPLE_X = {0, 100, 255};
    private static final int[] SAMPLE_Z = {0, 137, -211};

    private static final int SAMPLE_Y = 64;

    private static final Set<String> DONE = ConcurrentHashMap.newKeySet();

    public static void runOnce(ServerLevel level, WorldLoopTransformer transformer) {
        String levelName = level.dimension().identifier().toString();
        if (!DONE.add(levelName)) {
            return;
        }

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        int widthBlocks = transformer.coords.x.domainLength;

        Set<String> brokenFields = new LinkedHashSet<>();
        int brokenSamples = 0;

        for (int z : SAMPLE_Z) {
            for (int x : SAMPLE_X) {
                NoiseColumn columnHere = generator.getBaseColumn(x, z, level, randomState);
                NoiseColumn columnLapAway = generator.getBaseColumn(x + widthBlocks, z, level, randomState);
                boolean broken = collectColumn(brokenFields, level, columnHere, columnLapAway);

                int quartX = QuartPos.fromBlock(x);
                int quartZ = QuartPos.fromBlock(z);
                int quartY = QuartPos.fromBlock(SAMPLE_Y);
                int lapAwayQuartX = QuartPos.fromBlock(x + widthBlocks);

                Climate.Sampler sampler = randomState.sampler();
                Climate.TargetPoint climateHere = GenerationTransformerContext.withTransformer(transformer,
                        () -> sampler.sample(quartX, quartY, quartZ));
                Climate.TargetPoint climateLapAway = GenerationTransformerContext.withTransformer(transformer,
                        () -> sampler.sample(lapAwayQuartX, quartY, quartZ));
                broken |= collectClimate(brokenFields, climateHere, climateLapAway);

                Holder<Biome> biomeHere = GenerationTransformerContext.withTransformer(transformer,
                        () -> generator.getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, sampler));
                Holder<Biome> biomeLapAway = GenerationTransformerContext.withTransformer(transformer,
                        () -> generator.getBiomeSource().getNoiseBiome(lapAwayQuartX, quartY, quartZ, sampler));
                if (!biomeHere.equals(biomeLapAway)) {
                    brokenFields.add("biome");
                    broken = true;
                }

                if (broken) {
                    brokenSamples++;
                }
            }
        }

        if (!brokenFields.isEmpty()) {
            LOGGER.warn(
                    "[world-loop] periodicity_broken level={} width_blocks={} fields={} broken_samples={} samples={}",
                    levelName, widthBlocks, String.join(",", brokenFields), brokenSamples,
                    SAMPLE_X.length * SAMPLE_Z.length);
        }
    }

    private static boolean collectColumn(Set<String> brokenFields, LevelHeightAccessor heightAccessor,
            NoiseColumn here, NoiseColumn lapAway) {
        for (int y = heightAccessor.getMinY(); y <= heightAccessor.getMaxY(); y++) {
            if (here.getBlock(y) != lapAway.getBlock(y)) {
                brokenFields.add("column");
                return true;
            }
        }

        return false;
    }

    private static boolean collectClimate(Set<String> brokenFields, Climate.TargetPoint here,
            Climate.TargetPoint lapAway) {
        boolean broken = collect(brokenFields, "temperature", here.temperature(), lapAway.temperature());
        broken |= collect(brokenFields, "humidity", here.humidity(), lapAway.humidity());
        broken |= collect(brokenFields, "continentalness", here.continentalness(), lapAway.continentalness());
        broken |= collect(brokenFields, "erosion", here.erosion(), lapAway.erosion());
        broken |= collect(brokenFields, "depth", here.depth(), lapAway.depth());
        return broken | collect(brokenFields, "weirdness", here.weirdness(), lapAway.weirdness());
    }

    private static boolean collect(Set<String> brokenFields, String field, long here, long lapAway) {
        if (here == lapAway) {
            return false;
        }

        brokenFields.add(field);
        return true;
    }

    private PeriodicityCheck() {
    }
}
