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

// A looped world's terrain is periodic by construction: the ground at x and at x plus one world width is the same
// ground, so every generator query about the two must answer identically — not similarly, identically. Comparing them
// is a check the world either passes or fails, and it runs without a seam ever being looked at.
//
// It exists because the way this mod's folds break is silent. A fold is a mixin on a vanilla primitive, and another mod
// can take that primitive away — C2ME's natives-math @Overwrite of the End islands did exactly that, from a config of
// higher priority — without the injection failing, without a warning, and without anything in the log looking wrong.
// Mixin reports what fails to APPLY; nothing reports what stops being REACHED. Periodicity is the property those folds
// exist to produce, so asking after the property catches every cause: an overwrite, a bypassed call site, a target that
// moved in a new game version, a fold that was never written.
//
// **The check is one-way.** A mismatch proves something is broken. Agreement does not prove health: a field that is
// constant across the sample — the End's island function reads its -100 floor at 5 of the 9 points, void against void
// — passes while folding nothing at all. Read a silent run as "nothing was caught here", never as "the fold works".
//
// Once per level per session, on the first generation step that level takes, which is the earliest moment the router
// exists and the transformer is bound.
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

        // Insertion-ordered so the warning names the fields in the order they were first found to differ, which is the
        // order of the router itself — the first one is usually the one that dragged the rest with it.
        Set<String> brokenFields = new LinkedHashSet<>();
        int brokenSamples = 0;

        for (int z : SAMPLE_Z) {
            for (int x : SAMPLE_X) {
                // The column, not getBaseHeight: LoopedChunkGenerator caches base heights by the WRAPPED column, so the
                // two queries hit one key and the second is handed the first one's number whether the fold works or
                // not — a comparison that cannot come out unequal, read through the very primitive under test. On
                // build .18, with erosion broken on 4 of 9 samples, all 9 height comparisons still said equal.
                // getBaseColumn reaches iterateNoiseColumn without passing that cache.
                NoiseColumn columnHere = generator.getBaseColumn(x, z, level, randomState);
                NoiseColumn columnLapAway = generator.getBaseColumn(x + widthBlocks, z, level, randomState);
                boolean broken = collectColumn(brokenFields, level, columnHere, columnLapAway);

                // The height router and the climate router are different sets of density functions. A biome is what
                // puts deep ocean on one side of a seam and land on the other, and it is chosen from this sample — so
                // it gets asked the same question the terrain was.
                //
                // Bound explicitly, and that is the whole point: the height above answers through
                // LoopedChunkGenerator.getBaseHeight, which binds the transformer itself, while a sampler called
                // straight from here has nobody to bind it. Sampling unbound measures vanilla noise against itself and
                // reports the periodicity broken however well the mod works — a check failing its own subject.
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

    // Block states are interned, so identity is the comparison; the first disagreeing block is enough to name the
    // column broken and there is nothing more to learn from walking the rest of it.
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
