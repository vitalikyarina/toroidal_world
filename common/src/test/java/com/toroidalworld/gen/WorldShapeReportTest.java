package com.toroidalworld.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopPresets;
import com.toroidalworld.options.WorldLoopSizes;

import net.minecraft.core.Direction;

class WorldShapeReportTest {
    private static final int OVERWORLD_CHUNK_WIDTH = 192;
    private static final int DIVIDING_NETHER_CHUNK_WIDTH = 24;
    private static final int UNEVEN_NETHER_CHUNK_WIDTH = 20;
    private static final int NARROW_END_CHUNK_WIDTH = 128;

    @Test
    void aNetherWidthTheOverworldDividesByKeepsNamingItsScale() {
        assertEquals(", scale 1:8", WorldShapeReport.netherScaleNote(
                square(OVERWORLD_CHUNK_WIDTH), square(DIVIDING_NETHER_CHUNK_WIDTH)));
    }

    @Test
    void aNetherWidthTheOverworldDoesNotDivideByIsNamedAsBroken() {
        String note = WorldShapeReport.netherScaleNote(
                square(OVERWORLD_CHUNK_WIDTH), square(UNEVEN_NETHER_CHUNK_WIDTH));

        assertTrue(note.contains(WorldLoopSizes.describe(OVERWORLD_CHUNK_WIDTH)), note);
        assertTrue(note.contains(WorldLoopSizes.describe(UNEVEN_NETHER_CHUNK_WIDTH)), note);
    }

    @Test
    void aCylinderIsHeldToTheRuleOnItsLoopingAxis() {
        assertEquals(", scale 1:8", WorldShapeReport.netherScaleNote(
                loopedZ(OVERWORLD_CHUNK_WIDTH), loopedZ(DIVIDING_NETHER_CHUNK_WIDTH)));

        String note = WorldShapeReport.netherScaleNote(
                loopedZ(OVERWORLD_CHUNK_WIDTH), loopedZ(UNEVEN_NETHER_CHUNK_WIDTH));

        assertTrue(note.contains(Direction.Axis.Z.getName()), note);
        assertTrue(note.contains(WorldLoopSizes.describe(UNEVEN_NETHER_CHUNK_WIDTH)), note);
    }

    @Test
    void aNetherThatLoopsOnNeitherOfTheOverworldsAxesSaysNothing() {
        assertEquals("", WorldShapeReport.netherScaleNote(loopedZ(OVERWORLD_CHUNK_WIDTH),
                WorldLoopBounds.ofWidth(Direction.Axis.X, DIVIDING_NETHER_CHUNK_WIDTH)));
    }

    @Test
    void everyShippedPresetsNetherWidthDividesAndKeepsItsScale() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            int netherChunkWidth = preset.chunkWidth() / preset.netherScale();

            assertEquals(", scale 1:" + preset.netherScale(),
                    WorldShapeReport.netherScaleNote(square(preset.chunkWidth()), square(netherChunkWidth)),
                    preset.id());
            assertEquals(", scale 1:" + preset.netherScale(),
                    WorldShapeReport.netherScaleNote(loopedZ(preset.chunkWidth()), loopedZ(netherChunkWidth)),
                    preset.id());
        }
    }

    @Test
    void anEndAtTheOuterIslandThresholdSaysNothing() {
        assertEquals("", WorldShapeReport.endWidthNote(square(WorldLoopSizes.END_MIN_CHUNK_WIDTH)));
    }

    @Test
    void anEndBelowTheOuterIslandThresholdIsNamedAsBroken() {
        String note = WorldShapeReport.endWidthNote(square(NARROW_END_CHUNK_WIDTH));

        assertTrue(note.contains(WorldLoopSizes.describe(NARROW_END_CHUNK_WIDTH)), note);
        assertTrue(note.contains(WorldLoopSizes.describe(WorldLoopSizes.END_MIN_CHUNK_WIDTH)), note);
    }

    @Test
    void aCylinderEndIsHeldToTheThresholdOnItsLoopingAxis() {
        String note = WorldShapeReport.endWidthNote(loopedZ(NARROW_END_CHUNK_WIDTH));

        assertTrue(note.contains(Direction.Axis.Z.getName()), note);
        assertEquals("", WorldShapeReport.endWidthNote(loopedZ(WorldLoopSizes.END_MIN_CHUNK_WIDTH)));
    }

    @Test
    void everyShippedPresetsEndClearsTheThreshold() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertEquals("", WorldShapeReport.endWidthNote(square(preset.endChunkWidth())), preset.id());
            assertEquals("", WorldShapeReport.endWidthNote(loopedZ(preset.endChunkWidth())), preset.id());
        }
    }

    private static WorldLoopBounds square(int chunkWidth) {
        return WorldLoopBounds.ofWidth(chunkWidth);
    }

    private static WorldLoopBounds loopedZ(int chunkWidth) {
        return WorldLoopBounds.ofWidth(Direction.Axis.Z, chunkWidth);
    }
}
