package com.toroidalworld.engine.noise;

import static com.toroidalworld.engine.noise.DensityFunctionFixture.CLIMATE_AMPLITUDES;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.CLIMATE_FIRST_OCTAVE;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.CLIMATE_NOISE;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.SQUARE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClimateWarpDivisorTest {
    private static final double HORIZONTAL_SHARE = 0.0;

    private static final boolean CLIMATE_FIELD = true;

    @Test
    void aCompactedFieldWarpsAgainstItsOwnLattice() {
        double factor = ClimateScaleCompression.factor(SQUARE, CLIMATE_FIELD, CLIMATE_AMPLITUDES,
                Math.pow(2.0, CLIMATE_FIRST_OCTAVE), CLIMATE_XZ_SCALE, HORIZONTAL_SHARE);

        assertTrue(factor > 1.0, "the fixture sits outside the compressed regime, so the case proves nothing");
        assertEquals(CLIMATE_XZ_SCALE * factor,
                DomainWarp.divisor(CLIMATE_NOISE, SQUARE, CLIMATE_XZ_SCALE, HORIZONTAL_SHARE));
    }
}
