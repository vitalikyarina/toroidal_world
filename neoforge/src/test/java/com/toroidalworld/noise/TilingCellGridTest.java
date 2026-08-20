package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.options.WorldLoopPresets;

class TilingCellGridTest {
    private static final int TYPE_CELL_WIDTH = NoiseConstants.AQUIFER_FLUID_TYPE_CELL_WIDTH;
    private static final int LEVEL_CELL_WIDTH = NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH;

    private static final int SWEEP_MIN_CHUNKS = 16;
    private static final int SWEEP_MAX_CHUNKS = 2000;

    private static final int[] PERIODICITY_CHUNK_WIDTHS = {16, 17, 18, 19, 20, 32, 36, 101, 582};

    // Coprime with both vanilla cell widths, so a sweep of it lands on every residue of either grid.
    private static final int PERIODICITY_STEP = 7;

    private static TilingCellGrid grid(int chunkWidth, int vanillaCellWidth) {
        return TilingCellGrid.of(new WorldLoopTransformer(WorldLoopBounds.ofWidth(chunkWidth)), vanillaCellWidth);
    }

    @Nested
    class VanillaWidthKept {
        @Test
        void everyPresetKeepsBothVanillaCellWidths() {
            for (WorldLoopPresets preset : WorldLoopPresets.values()) {
                for (int chunkWidth : new int[] {
                        preset.chunkWidth(),
                        preset.chunkWidth() / preset.netherScale(),
                        preset.endChunkWidth()}) {
                    String world = preset.id() + " at " + chunkWidth + " chunks";
                    assertEquals(TYPE_CELL_WIDTH, grid(chunkWidth, TYPE_CELL_WIDTH).xCellWidth(), world);
                    assertEquals(LEVEL_CELL_WIDTH, grid(chunkWidth, LEVEL_CELL_WIDTH).xCellWidth(), world);
                }
            }
        }

        @Test
        void everyWidthOfWholeFourChunksKeepsTheSixtyFourBlockGrid() {
            for (int chunkWidth = SWEEP_MIN_CHUNKS; chunkWidth <= SWEEP_MAX_CHUNKS; chunkWidth += 4) {
                assertEquals(TYPE_CELL_WIDTH, grid(chunkWidth, TYPE_CELL_WIDTH).xCellWidth(),
                        "in a " + chunkWidth + "-chunk world");
            }
        }

        @Test
        void everyWidthKeepsTheSixteenBlockGrid() {
            for (int chunkWidth = SWEEP_MIN_CHUNKS; chunkWidth <= SWEEP_MAX_CHUNKS; chunkWidth++) {
                assertEquals(LEVEL_CELL_WIDTH, grid(chunkWidth, LEVEL_CELL_WIDTH).xCellWidth(),
                        "in a " + chunkWidth + "-chunk world");
            }
        }

        @Test
        void anUnboundedAxisKeepsTheVanillaCellWidth() {
            WorldLoopTransformer transformer = new WorldLoopTransformer(
                    new WorldLoopBounds(new AxisBounds.Looped(-9, 9), AxisBounds.Unbounded.INSTANCE));
            TilingCellGrid grid = TilingCellGrid.of(transformer, TYPE_CELL_WIDTH);

            assertEquals(72, grid.xCellWidth());
            assertEquals(TYPE_CELL_WIDTH, grid.zCellWidth());
        }
    }

    @Nested
    class NearestDivisor {
        @Test
        void eighteenChunksLandsOnSeventyTwoBlocks() {
            assertEquals(72, grid(18, TYPE_CELL_WIDTH).xCellWidth());
        }

        @Test
        void aPrimeMultipleOfAChunkTakesTheNearestDivisorItHas() {
            assertEquals(101, grid(101, TYPE_CELL_WIDTH).xCellWidth());
        }

        @Test
        void anExactTieTakesTheFinerGrid() {
            assertEquals(48, grid(582, TYPE_CELL_WIDTH).xCellWidth());
        }

        @Test
        void theCellWidthDividesTheAxisWidthOnEveryWidth() {
            for (int chunkWidth = SWEEP_MIN_CHUNKS; chunkWidth <= SWEEP_MAX_CHUNKS; chunkWidth++) {
                int width = chunkWidth * 16;
                assertEquals(0, width % grid(chunkWidth, TYPE_CELL_WIDTH).xCellWidth(),
                        "in a " + chunkWidth + "-chunk world");
            }
        }

        @Test
        void eachAxisTakesItsOwnWidth() {
            WorldLoopTransformer transformer = new WorldLoopTransformer(
                    new WorldLoopBounds(new AxisBounds.Looped(-9, 9), new AxisBounds.Looped(-16, 16)));
            TilingCellGrid grid = TilingCellGrid.of(transformer, TYPE_CELL_WIDTH);

            assertEquals(72, grid.xCellWidth());
            assertEquals(TYPE_CELL_WIDTH, grid.zCellWidth());
        }
    }

    @Nested
    class Periodicity {
        @Test
        void theCellOriginIsTheSameAWorldWidthAway() {
            for (int chunkWidth : PERIODICITY_CHUNK_WIDTHS) {
                assertPeriodic(chunkWidth, TYPE_CELL_WIDTH);
                assertPeriodic(chunkWidth, LEVEL_CELL_WIDTH);
            }
        }

        @Test
        void theVanillaCellWidthIsNotPeriodicOnEighteenChunks() {
            WorldLoopTransformer transformer = new WorldLoopTransformer(WorldLoopBounds.ofWidth(18));
            WrapDomain domain = transformer.coords.x;
            int width = domain.domainLength;
            boolean differs = false;

            for (int x = domain.lowerBound; x < domain.upperBound && !differs; x++) {
                differs = vanillaCellOrigin(domain, x) != vanillaCellOrigin(domain, x + width);
            }

            assertTrue(differs, "the 64-block grid must break a lap away, or the periodicity test cannot go red");
        }

        private void assertPeriodic(int chunkWidth, int vanillaCellWidth) {
            WorldLoopTransformer transformer = new WorldLoopTransformer(WorldLoopBounds.ofWidth(chunkWidth));
            TilingCellGrid grid = TilingCellGrid.of(transformer, vanillaCellWidth);
            WrapDomain domain = transformer.coords.x;
            int width = domain.domainLength;

            for (int x = domain.lowerBound - width; x < domain.upperBound + width; x += PERIODICITY_STEP) {
                String at = "at x=" + x + " in a " + chunkWidth + "-chunk world on a "
                        + vanillaCellWidth + "-block vanilla grid";
                assertEquals(domain.wrap(grid.cellOriginX(x)), domain.wrap(grid.cellOriginX(x + width)), at);
                assertEquals(domain.wrap(grid.cellOriginZ(x)), domain.wrap(grid.cellOriginZ(x + width)), at);
            }
        }

        private int vanillaCellOrigin(WrapDomain domain, int blockCoord) {
            return domain.wrap(Math.floorDiv(blockCoord, TYPE_CELL_WIDTH) * TYPE_CELL_WIDTH);
        }
    }
}
