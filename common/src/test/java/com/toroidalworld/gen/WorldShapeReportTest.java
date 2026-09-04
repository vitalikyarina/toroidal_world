package com.toroidalworld.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
    void aNetherWidthTheOverworldDividesByNamesItsScaleAndIsNotBroken() {
        WorldShapeReport.Note note = WorldShapeReport.netherScaleNote(
                square(OVERWORLD_CHUNK_WIDTH), square(DIVIDING_NETHER_CHUNK_WIDTH));

        assertEquals(", scale 1:8", note.text());
        assertFalse(note.broken(), note.text());
    }

    @Test
    void aNetherWidthTheOverworldDoesNotDivideByIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.netherScaleNote(
                square(OVERWORLD_CHUNK_WIDTH), square(UNEVEN_NETHER_CHUNK_WIDTH));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(OVERWORLD_CHUNK_WIDTH)), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(UNEVEN_NETHER_CHUNK_WIDTH)), note.text());
    }

    @Test
    void aCylinderIsHeldToTheRuleOnItsLoopingAxis() {
        WorldShapeReport.Note healthy = WorldShapeReport.netherScaleNote(
                loopedZ(OVERWORLD_CHUNK_WIDTH), loopedZ(DIVIDING_NETHER_CHUNK_WIDTH));

        assertEquals(", scale 1:8", healthy.text());
        assertFalse(healthy.broken(), healthy.text());

        WorldShapeReport.Note note = WorldShapeReport.netherScaleNote(
                loopedZ(OVERWORLD_CHUNK_WIDTH), loopedZ(UNEVEN_NETHER_CHUNK_WIDTH));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains(Direction.Axis.Z.getName()), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(UNEVEN_NETHER_CHUNK_WIDTH)), note.text());
    }

    @Test
    void aNetherThatLoopsOnNeitherOfTheOverworldsAxesSaysNothing() {
        assertEquals(WorldShapeReport.Note.NONE, WorldShapeReport.netherScaleNote(loopedZ(OVERWORLD_CHUNK_WIDTH),
                WorldLoopBounds.ofWidth(Direction.Axis.X, DIVIDING_NETHER_CHUNK_WIDTH)));
    }

    @Test
    void everyShippedPresetsNetherWidthDividesAndKeepsItsScale() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            int netherChunkWidth = preset.chunkWidth() / preset.netherScale();

            for (WorldShapeReport.Note note : List.of(
                    WorldShapeReport.netherScaleNote(square(preset.chunkWidth()), square(netherChunkWidth)),
                    WorldShapeReport.netherScaleNote(loopedZ(preset.chunkWidth()), loopedZ(netherChunkWidth)))) {
                assertEquals(", scale 1:" + preset.netherScale(), note.text(), preset.id());
                assertFalse(note.broken(), preset.id());
            }
        }
    }

    @Test
    void anEndAtTheOuterIslandThresholdSaysNothing() {
        assertEquals(WorldShapeReport.Note.NONE,
                WorldShapeReport.endWidthNote(square(WorldLoopSizes.END_MIN_CHUNK_WIDTH)));
    }

    @Test
    void anEndBelowTheOuterIslandThresholdIsNamedAsBroken() {
        WorldShapeReport.Note note = WorldShapeReport.endWidthNote(square(NARROW_END_CHUNK_WIDTH));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(NARROW_END_CHUNK_WIDTH)), note.text());
        assertTrue(note.text().contains(WorldLoopSizes.describe(WorldLoopSizes.END_MIN_CHUNK_WIDTH)), note.text());
    }

    @Test
    void aCylinderEndIsHeldToTheThresholdOnItsLoopingAxis() {
        WorldShapeReport.Note note = WorldShapeReport.endWidthNote(loopedZ(NARROW_END_CHUNK_WIDTH));

        assertTrue(note.broken(), note.text());
        assertTrue(note.text().contains(Direction.Axis.Z.getName()), note.text());
        assertEquals(WorldShapeReport.Note.NONE,
                WorldShapeReport.endWidthNote(loopedZ(WorldLoopSizes.END_MIN_CHUNK_WIDTH)));
    }

    @Test
    void everyShippedPresetsEndClearsTheThreshold() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertEquals(WorldShapeReport.Note.NONE,
                    WorldShapeReport.endWidthNote(square(preset.endChunkWidth())), preset.id());
            assertEquals(WorldShapeReport.Note.NONE,
                    WorldShapeReport.endWidthNote(loopedZ(preset.endChunkWidth())), preset.id());
        }
    }

    private static WorldLoopBounds square(int chunkWidth) {
        return WorldLoopBounds.ofWidth(chunkWidth);
    }

    private static WorldLoopBounds loopedZ(int chunkWidth) {
        return WorldLoopBounds.ofWidth(Direction.Axis.Z, chunkWidth);
    }
}
