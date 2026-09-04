package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.ForeignSpan;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;

class SableFramesTest {
    private static final int PLOTS_PER_SIDE = 1 << SubLevelContainer.DEFAULT_LOG_SIZE_LENGTH;
    private static final int LOG_PLOT_SIZE = SubLevelContainer.DEFAULT_LOG_PLOT_SIZE;

    @Test
    void sablesDefaultPlotGridIsTheChunkSpanTheFrameReports() {
        ForeignSpan span = SableFrames.plotChunks(SubLevelContainer.DEFAULT_ORIGIN, PLOTS_PER_SIDE, LOG_PLOT_SIZE);

        assertEquals(1_280_000, span.min(), "plot origin 10000 at 128 chunks per plot");
        assertEquals(1_296_384, span.max(), "128 plots of 128 chunks past that origin");
    }

    @Test
    void theSideLengthIsTheWholeSpanRegardlessOfWhereTheOriginSits() {
        int side = PLOTS_PER_SIDE << LOG_PLOT_SIZE;

        assertEquals(side, SableFrames.plotChunks(0, PLOTS_PER_SIDE, LOG_PLOT_SIZE).max());
        assertEquals(-128, SableFrames.plotChunks(-1, PLOTS_PER_SIDE, LOG_PLOT_SIZE).min(),
                "the shift is applied to the plot index, not to the count of chunks in a plot");
        assertEquals(side - 128, SableFrames.plotChunks(-1, PLOTS_PER_SIDE, LOG_PLOT_SIZE).max());
    }
}
