package com.toroidalworld.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.Test;

class JourneyMapFoldStaleGridTest {
    private static final File COMBIO = new File("saves/combio");
    private static final File COMBIO_COPY = new File("saves/combio (1)");
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    @Test
    void theFirstCallDropsNothing() {
        assertNull(JourneyMapFold.staleGridReason(null, OVERWORLD, null, COMBIO),
                "the grid was dropped before anything had been drawn into it");
    }

    @Test
    void anUnchangedWorldAndDimensionDropNothing() {
        assertNull(JourneyMapFold.staleGridReason(OVERWORLD, OVERWORLD, COMBIO, COMBIO),
                "the grid was dropped while neither the world nor the dimension had changed");
    }

    @Test
    void aChangedDimensionDropsTheGrid() {
        assertEquals(JourneyMapFold.DIMENSION_CHANGED,
                JourneyMapFold.staleGridReason(OVERWORLD, NETHER, COMBIO, COMBIO),
                "the overworld tiles survived into the nether");
    }

    @Test
    void aChangedWorldDropsTheGrid() {
        assertEquals(JourneyMapFold.WORLD_CHANGED,
                JourneyMapFold.staleGridReason(OVERWORLD, OVERWORLD, COMBIO, COMBIO_COPY),
                "two save folders of one dimension were read as one world");
    }

    @Test
    void aWorldChangeOutranksADimensionChange() {
        assertEquals(JourneyMapFold.WORLD_CHANGED,
                JourneyMapFold.staleGridReason(OVERWORLD, NETHER, COMBIO, COMBIO_COPY),
                "the reason named the dimension while the world had changed too");
    }

    @Test
    void aWorldDirectoryThatIsNotKnownYetDropsNothing() {
        assertNull(JourneyMapFold.staleGridReason(OVERWORLD, OVERWORLD, null, COMBIO),
                "the first world directory was read as a change away from something");
        assertNull(JourneyMapFold.staleGridReason(OVERWORLD, OVERWORLD, COMBIO, null),
                "a directory the renderer has not been handed yet dropped the grid");
    }
}
