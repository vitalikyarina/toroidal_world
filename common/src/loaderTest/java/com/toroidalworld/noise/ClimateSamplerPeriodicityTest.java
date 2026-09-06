package com.toroidalworld.noise;

import static com.toroidalworld.noise.ClimateScanFixture.SCAN_Y_BLOCKS;
import static com.toroidalworld.noise.ClimateScanFixture.SEED_BASE;
import static com.toroidalworld.noise.ClimateScanFixture.TYPES;
import static com.toroidalworld.noise.ClimateScanFixture.biomeSource;
import static com.toroidalworld.noise.ClimateScanFixture.cylinderOfWidth;
import static com.toroidalworld.noise.ClimateScanFixture.randomState;
import static com.toroidalworld.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.ClimateScanFixture.WorldType;
import com.toroidalworld.options.WorldLoopPresets;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

class ClimateSamplerPeriodicityTest {
    private static final int SAMPLES = 64;

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void theSamplerRepeatsOneWidthAwayOnATorus() {
        WorldType type = TYPES.getFirst();
        MultiNoiseBiomeSource source = biomeSource(type);
        int width = WorldLoopPresets.TINY.blockWidth();
        WorldFold fold = torusOfWidth(width);
        Climate.Sampler sampler = randomState(type, fold, SEED_BASE).sampler();
        int quartY = QuartPos.fromBlock(SCAN_Y_BLOCKS);
        Map<String, Integer> broken = new HashMap<>();

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int i = 0; i < SAMPLES; i++) {
                int x = i * (width / SAMPLES);
                int z = (i * 37) % width;
                Climate.TargetPoint here = sampler.sample(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z));
                Climate.TargetPoint lapAway =
                        sampler.sample(QuartPos.fromBlock(x + width), quartY, QuartPos.fromBlock(z));
                collect(broken, "temperature", here.temperature(), lapAway.temperature());
                collect(broken, "humidity", here.humidity(), lapAway.humidity());
                collect(broken, "continentalness", here.continentalness(), lapAway.continentalness());
                collect(broken, "erosion", here.erosion(), lapAway.erosion());
                collect(broken, "depth", here.depth(), lapAway.depth());
                collect(broken, "weirdness", here.weirdness(), lapAway.weirdness());
                source.getNoiseBiome(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z), sampler);
            }
        });

        assertTrue(broken.isEmpty(), "the sampler is not reading a folded world, mismatches of " + SAMPLES
                + " samples per field: " + broken);
    }

    @Test
    void theCylinderSamplerRepeatsOnItsLoopedAxisAlone() {
        WorldType type = TYPES.getFirst();
        int width = WorldLoopPresets.TINY.blockWidth();
        WorldFold fold = cylinderOfWidth(width);
        Climate.Sampler sampler = randomState(type, fold, SEED_BASE).sampler();
        int quartY = QuartPos.fromBlock(SCAN_Y_BLOCKS);
        Map<String, Integer> broken = new HashMap<>();
        Map<String, Integer> varyingAcross = new HashMap<>();

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int i = 0; i < SAMPLES; i++) {
                int x = i * (width / SAMPLES);
                int z = (i * 37) % width;
                Climate.TargetPoint here = sampler.sample(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z));
                Climate.TargetPoint alongX =
                        sampler.sample(QuartPos.fromBlock(x + width), quartY, QuartPos.fromBlock(z));
                Climate.TargetPoint alongZ =
                        sampler.sample(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z + width));
                collect(broken, "temperature", here.temperature(), alongX.temperature());
                collect(broken, "humidity", here.humidity(), alongX.humidity());
                collect(broken, "continentalness", here.continentalness(), alongX.continentalness());
                collect(broken, "erosion", here.erosion(), alongX.erosion());
                collect(broken, "depth", here.depth(), alongX.depth());
                collect(broken, "weirdness", here.weirdness(), alongX.weirdness());
                collect(varyingAcross, "temperature", alongZ.temperature(), here.temperature());
                collect(varyingAcross, "continentalness", alongZ.continentalness(), here.continentalness());
            }
        });

        assertTrue(broken.isEmpty(), "the looped axis does not repeat one width away, mismatches of " + SAMPLES
                + " samples per field: " + broken);
        assertEquals(2, varyingAcross.size(),
                "the unbounded axis repeats one width away, so the sampler is not reading a cylinder");
    }

    private static void collect(Map<String, Integer> broken, String field, long here, long lapAway) {
        if (here != lapAway) {
            broken.merge(field, 1, Integer::sum);
        }
    }
}
