package com.toroidalworld.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class FloatingCrumbsTest {
    private static final int SIDE = 16;

    private static final int HEIGHT = 8;

    private static final class Grid {
        private final boolean[] solid = new boolean[SIDE * SIDE * HEIGHT];
        private int blocks;

        Grid set(int x, int z, int y) {
            this.solid[x + z * SIDE + y * SIDE * SIDE] = true;
            this.blocks++;
            return this;
        }

        boolean at(int x, int z, int y) {
            return this.solid[x + z * SIDE + y * SIDE * SIDE];
        }

        List<Integer> sweep() {
            List<Integer> cleared = new ArrayList<>();
            this.result = FloatingCrumbs.clearCrumbs(this.solid, HEIGHT, this.blocks, cleared::add);
            return cleared;
        }

        private FloatingCrumbs.Sweep result;
    }

    @Test
    void aLoneBlockInTheInteriorIsSwept() {
        Grid grid = new Grid().set(8, 8, 4);

        List<Integer> cleared = grid.sweep();

        assertEquals(1, cleared.size());
        assertEquals(new FloatingCrumbs.Sweep(1, 1, 1), grid.result);
        assertFalse(grid.at(8, 8, 4));
    }

    @Test
    void aBlockTouchingAChunkSideStands() {
        for (int[] site : new int[][] {{0, 8}, {15, 8}, {8, 0}, {8, 15}}) {
            Grid grid = new Grid().set(site[0], site[1], 4);

            List<Integer> cleared = grid.sweep();

            assertTrue(cleared.isEmpty(), "swept a block against a chunk side at x=" + site[0] + " z=" + site[1]);
            assertEquals(new FloatingCrumbs.Sweep(0, 0, 0), grid.result);
            assertTrue(grid.at(site[0], site[1], 4));
        }
    }

    @Test
    void sixConnectivityHoldsTheComponentTogether() {
        Grid grid = new Grid().set(8, 8, 4).set(8, 8, 5).set(9, 8, 5).set(9, 9, 5);

        List<Integer> cleared = grid.sweep();

        assertEquals(4, cleared.size());
        assertEquals(new FloatingCrumbs.Sweep(1, 1, 4), grid.result);
    }

    @Test
    void aDiagonalNeighbourIsAnotherComponent() {
        Grid grid = new Grid().set(8, 8, 4).set(9, 9, 5);

        grid.sweep();

        assertEquals(new FloatingCrumbs.Sweep(2, 2, 2), grid.result);
    }

    @Test
    void aComponentAtTheCeilingStandsAndOneBelowItGoes() {
        Grid grid = new Grid();
        int placed = 0;
        for (int x = 4; x < 12 && placed < FloatingCrumbs.CRUMB_CEILING_BLOCKS; x++) {
            for (int z = 4; z < 12 && placed < FloatingCrumbs.CRUMB_CEILING_BLOCKS; z++) {
                grid.set(x, z, 4);
                placed++;
            }
        }

        List<Integer> cleared = grid.sweep();

        assertEquals(FloatingCrumbs.CRUMB_CEILING_BLOCKS, placed);
        assertTrue(cleared.isEmpty(), "swept a component of exactly the ceiling");
        assertEquals(new FloatingCrumbs.Sweep(1, 0, 0), grid.result);
    }

    @Test
    void oneBlockUnderTheCeilingGoes() {
        Grid grid = new Grid();
        int placed = 0;
        for (int x = 4; x < 12 && placed < FloatingCrumbs.CRUMB_CEILING_BLOCKS - 1; x++) {
            for (int z = 4; z < 12 && placed < FloatingCrumbs.CRUMB_CEILING_BLOCKS - 1; z++) {
                grid.set(x, z, 4);
                placed++;
            }
        }

        List<Integer> cleared = grid.sweep();

        assertEquals(FloatingCrumbs.CRUMB_CEILING_BLOCKS - 1, cleared.size());
        assertEquals(new FloatingCrumbs.Sweep(1, 1, FloatingCrumbs.CRUMB_CEILING_BLOCKS - 1), grid.result);
    }

    @Test
    void aCrumbBesideTerrainThatReachesASideStands() {
        Grid grid = new Grid();
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                grid.set(x, z, 0);
            }
        }

        grid.set(8, 8, 1);

        List<Integer> cleared = grid.sweep();

        assertTrue(cleared.isEmpty(), "swept a block sitting on terrain");
        assertEquals(new FloatingCrumbs.Sweep(0, 0, 0), grid.result);
    }

    @Test
    void aCrumbOverTerrainGoesAndTheTerrainStands() {
        Grid grid = new Grid();
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                grid.set(x, z, 0);
            }
        }

        grid.set(8, 8, 3);

        List<Integer> cleared = grid.sweep();

        assertEquals(1, cleared.size());
        assertEquals(new FloatingCrumbs.Sweep(1, 1, 1), grid.result);
        assertTrue(grid.at(8, 8, 0));
        assertFalse(grid.at(8, 8, 3));
    }
}
