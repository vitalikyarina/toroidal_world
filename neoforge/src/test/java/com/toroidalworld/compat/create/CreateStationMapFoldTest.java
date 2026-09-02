package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class CreateStationMapFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;
    private static final int MIRROR_LINE_BLOCKS = MIRROR_LINE_CHUNK * 16;
    private static final int MARKER_HEIGHT = 64;
    private static final int MAP_CENTRE_X = 250;
    private static final int MAP_CENTRE_Z = 10;
    private static final int PAST_THE_X_BOUND = 300;
    private static final int ACROSS_THE_SEAM_X = -250;
    private static final int FAR_ALONG_Z = 5 * WORLD_BLOCKS + 7;
    private static final double TOGGLED_X = -250.5;
    private static final double TOGGLED_Z = 10.5;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-WORLD_CHUNKS, WORLD_CHUNKS), AxisBounds.Unbounded.INSTANCE);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));
    private static final WorldFold CYLINDER = new DeckGroupFold(FlatShape.cylinder(X_ONLY));

    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED, CYLINDER);
    private static final List<WorldFold> EVERY_SHAPE = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED, CYLINDER);

    @Test
    void aTargetPastTheXBoundLandsOneWorldWidthBack() {
        BlockPos target = new BlockPos(PAST_THE_X_BOUND, MARKER_HEIGHT, MAP_CENTRE_Z);
        for (WorldFold fold : TRANSLATING) {
            BlockPos canonical = CreateStationMapFold.canonicalTarget(fold, target);

            assertEquals(new BlockPos(PAST_THE_X_BOUND - WORLD_BLOCKS, MARKER_HEIGHT, MAP_CENTRE_Z), canonical,
                    "in " + fold);
        }
    }

    @Test
    void aTargetPastTheGlideSeamOfAMirroredWorldLandsOnTheMirroredColumn() {
        BlockPos canonical = CreateStationMapFold.canonicalTarget(MIRRORED,
                new BlockPos(PAST_THE_X_BOUND, MARKER_HEIGHT, MAP_CENTRE_Z));

        assertEquals(new BlockPos(PAST_THE_X_BOUND - WORLD_BLOCKS, MARKER_HEIGHT,
                2 * MIRROR_LINE_BLOCKS - MAP_CENTRE_Z - 1), canonical);
    }

    @Test
    void aTargetInsideTheBoundsIsTheTargetItself() {
        BlockPos inside = new BlockPos(30, MARKER_HEIGHT, MAP_CENTRE_Z);
        for (WorldFold fold : EVERY_SHAPE) {
            assertSame(inside, CreateStationMapFold.canonicalTarget(fold, inside), "in " + fold);
        }
    }

    @Test
    void aTargetFarAlongTheUnboundedAxisKeepsThatCoordinate() {
        BlockPos canonical = CreateStationMapFold.canonicalTarget(CYLINDER,
                new BlockPos(PAST_THE_X_BOUND, MARKER_HEIGHT, FAR_ALONG_Z));

        assertEquals(new BlockPos(PAST_THE_X_BOUND - WORLD_BLOCKS, MARKER_HEIGHT, FAR_ALONG_Z), canonical);
    }

    @Test
    void aTargetAcrossTheSeamFromTheMapCentreIsSeatedBesideIt() {
        BlockPos target = new BlockPos(ACROSS_THE_SEAM_X, MARKER_HEIGHT, MAP_CENTRE_Z);
        for (WorldFold fold : TRANSLATING) {
            BlockPos seated = CreateStationMapFold.targetInMapFrame(fold, MAP_CENTRE_X, MAP_CENTRE_Z, target);

            assertEquals(new BlockPos(ACROSS_THE_SEAM_X + WORLD_BLOCKS, MARKER_HEIGHT, MAP_CENTRE_Z), seated,
                    "in " + fold);
        }
    }

    @Test
    void aTargetAcrossTheGlideSeamFromTheMapCentreIsSeatedMirrored() {
        BlockPos seated = CreateStationMapFold.targetInMapFrame(MIRRORED, MAP_CENTRE_X, MAP_CENTRE_Z,
                new BlockPos(ACROSS_THE_SEAM_X, MARKER_HEIGHT, MAP_CENTRE_Z));

        assertEquals(new BlockPos(ACROSS_THE_SEAM_X + WORLD_BLOCKS, MARKER_HEIGHT,
                2 * MIRROR_LINE_BLOCKS - MAP_CENTRE_Z - 1), seated);
    }

    @Test
    void aTargetSeatedIntoTheMapFrameKeepsTheUnboundedAxis() {
        BlockPos seated = CreateStationMapFold.targetInMapFrame(CYLINDER, MAP_CENTRE_X, MAP_CENTRE_Z,
                new BlockPos(ACROSS_THE_SEAM_X, MARKER_HEIGHT, FAR_ALONG_Z));

        assertEquals(new BlockPos(ACROSS_THE_SEAM_X + WORLD_BLOCKS, MARKER_HEIGHT, FAR_ALONG_Z), seated);
    }

    @Test
    void aTargetBesideTheMapCentreIsTheTargetItself() {
        BlockPos beside = new BlockPos(30, MARKER_HEIGHT, MAP_CENTRE_Z);
        for (WorldFold fold : EVERY_SHAPE) {
            assertSame(beside, CreateStationMapFold.targetInMapFrame(fold, 20, MAP_CENTRE_Z, beside), "in " + fold);
        }
    }

    @Test
    void aToggledCentreAcrossTheSeamIsSeatedBesideTheMapCentre() {
        for (WorldFold fold : TRANSLATING) {
            Vec3 seated = CreateStationMapFold.centreInMapFrame(fold, MAP_CENTRE_X, MAP_CENTRE_Z,
                    TOGGLED_X, TOGGLED_Z);

            assertEquals(new Vec3(TOGGLED_X + WORLD_BLOCKS, 0.0, TOGGLED_Z), seated, "in " + fold);
        }
    }

    @Test
    void aToggledCentreAcrossTheGlideSeamIsSeatedMirrored() {
        Vec3 seated = CreateStationMapFold.centreInMapFrame(MIRRORED, MAP_CENTRE_X, MAP_CENTRE_Z,
                TOGGLED_X, TOGGLED_Z);

        assertEquals(new Vec3(TOGGLED_X + WORLD_BLOCKS, 0.0, 2 * MIRROR_LINE_BLOCKS - TOGGLED_Z), seated);
    }

    @Test
    void aToggledCentreBesideTheMapCentreKeepsItsCoordinates() {
        for (WorldFold fold : EVERY_SHAPE) {
            Vec3 seated = CreateStationMapFold.centreInMapFrame(fold, 20, MAP_CENTRE_Z, 30.5, TOGGLED_Z);

            assertEquals(new Vec3(30.5, 0.0, TOGGLED_Z), seated, "in " + fold);
        }
    }

    @Test
    void withoutAServerEveryDimensionMethodHandsBackWhatItWasGiven() {
        BlockPos target = new BlockPos(PAST_THE_X_BOUND, MARKER_HEIGHT, MAP_CENTRE_Z);

        assertSame(target, CreateStationMapFold.canonicalTarget(Level.OVERWORLD, target));
        assertSame(target, CreateStationMapFold.targetInMapFrame(Level.OVERWORLD, MAP_CENTRE_X, MAP_CENTRE_Z, target));
        assertEquals(new Vec3(TOGGLED_X, 0.0, TOGGLED_Z), CreateStationMapFold.centreInMapFrame(Level.OVERWORLD,
                MAP_CENTRE_X, MAP_CENTRE_Z, TOGGLED_X, TOGGLED_Z));
    }
}
