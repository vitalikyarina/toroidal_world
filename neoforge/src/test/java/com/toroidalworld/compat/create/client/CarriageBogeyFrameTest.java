package com.toroidalworld.compat.create.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class CarriageBogeyFrameTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;
    private static final int MIRROR_LINE_BLOCKS = MIRROR_LINE_CHUNK * 16;
    private static final int FIRST_CARRIAGE = 0;
    private static final int SECOND_CARRIAGE = 1;
    private static final double BOGEY_HEIGHT = 64.0;
    private static final double INSIDE_X = 250.0;
    private static final double INSIDE_Z = 10.0;
    private static final double PAST_THE_X_BOUND = 300.0;
    private static final double ACROSS_THE_SEAM_X = -254.0;
    private static final double FAR_ALONG_Z = 5 * WORLD_BLOCKS + 7.0;

    private static final Object TRAIN = new Object();
    private static final Object ANOTHER_TRAIN = new Object();

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

    private static final class CountingLookup implements Supplier<Vec3> {
        private final Vec3 answer;

        private int calls;

        private CountingLookup(Vec3 answer) {
            this.answer = answer;
        }

        @Override
        public Vec3 get() {
            this.calls++;
            return this.answer;
        }
    }

    private static CountingLookup lookupPastTheBound() {
        return new CountingLookup(new Vec3(PAST_THE_X_BOUND, BOGEY_HEIGHT, INSIDE_Z));
    }

    @Test
    void aLeadingBogeyPastTheXBoundIsCanonicalised() {
        Vec3 raw = new Vec3(PAST_THE_X_BOUND, BOGEY_HEIGHT, INSIDE_Z);
        for (WorldFold fold : TRANSLATING) {
            Vec3 canonical = new CarriageBogeyFrame().lead(fold, TRAIN, FIRST_CARRIAGE, raw);

            assertEquals(new Vec3(PAST_THE_X_BOUND - WORLD_BLOCKS, BOGEY_HEIGHT, INSIDE_Z), canonical, "in " + fold);
        }
    }

    @Test
    void aLeadingBogeyPastTheGlideSeamIsCanonicalisedMirrored() {
        Vec3 canonical = new CarriageBogeyFrame().lead(MIRRORED, TRAIN, FIRST_CARRIAGE,
                new Vec3(PAST_THE_X_BOUND, BOGEY_HEIGHT, INSIDE_Z));

        assertEquals(new Vec3(PAST_THE_X_BOUND - WORLD_BLOCKS, BOGEY_HEIGHT,
                2 * MIRROR_LINE_BLOCKS - INSIDE_Z), canonical);
    }

    @Test
    void aTrailingBogeyAcrossTheSeamIsWrittenBesideTheLeadingOne() {
        for (WorldFold fold : TRANSLATING) {
            CarriageBogeyFrame frame = new CarriageBogeyFrame();
            CountingLookup lookup = lookupPastTheBound();
            frame.lead(fold, TRAIN, FIRST_CARRIAGE, new Vec3(INSIDE_X, BOGEY_HEIGHT, INSIDE_Z));

            Vec3 trailing = frame.trail(fold, TRAIN, FIRST_CARRIAGE,
                    new Vec3(ACROSS_THE_SEAM_X, BOGEY_HEIGHT, INSIDE_Z), lookup);

            assertEquals(new Vec3(ACROSS_THE_SEAM_X + WORLD_BLOCKS, BOGEY_HEIGHT, INSIDE_Z), trailing, "in " + fold);
            assertEquals(0, lookup.calls, "lookups in " + fold);
        }
    }

    @Test
    void aTrailingBogeyAcrossTheGlideSeamIsWrittenMirroredBesideTheLeadingOne() {
        CarriageBogeyFrame frame = new CarriageBogeyFrame();
        frame.lead(MIRRORED, TRAIN, FIRST_CARRIAGE, new Vec3(INSIDE_X, BOGEY_HEIGHT, INSIDE_Z));

        Vec3 trailing = frame.trail(MIRRORED, TRAIN, FIRST_CARRIAGE,
                new Vec3(ACROSS_THE_SEAM_X, BOGEY_HEIGHT, INSIDE_Z), lookupPastTheBound());

        assertEquals(new Vec3(ACROSS_THE_SEAM_X + WORLD_BLOCKS, BOGEY_HEIGHT,
                2 * MIRROR_LINE_BLOCKS - INSIDE_Z), trailing);
    }

    @Test
    void aTrailingBogeyWithNoLeadingOneSeenAnchorsOnTheCanonicalisedLookup() {
        for (WorldFold fold : TRANSLATING) {
            CountingLookup lookup = lookupPastTheBound();

            Vec3 trailing = new CarriageBogeyFrame().trail(fold, TRAIN, FIRST_CARRIAGE,
                    new Vec3(INSIDE_X, BOGEY_HEIGHT, INSIDE_Z), lookup);

            assertEquals(new Vec3(INSIDE_X - WORLD_BLOCKS, BOGEY_HEIGHT, INSIDE_Z), trailing, "in " + fold);
            assertEquals(1, lookup.calls, "lookups in " + fold);
        }
    }

    @Test
    void aTrailingBogeyOfAnotherTrainAnchorsOnTheLookup() {
        for (WorldFold fold : TRANSLATING) {
            CarriageBogeyFrame frame = new CarriageBogeyFrame();
            CountingLookup lookup = lookupPastTheBound();
            frame.lead(fold, TRAIN, FIRST_CARRIAGE, new Vec3(INSIDE_X, BOGEY_HEIGHT, INSIDE_Z));

            Vec3 trailing = frame.trail(fold, ANOTHER_TRAIN, FIRST_CARRIAGE,
                    new Vec3(INSIDE_X, BOGEY_HEIGHT, INSIDE_Z), lookup);

            assertEquals(new Vec3(INSIDE_X - WORLD_BLOCKS, BOGEY_HEIGHT, INSIDE_Z), trailing, "in " + fold);
            assertEquals(1, lookup.calls, "lookups in " + fold);
        }
    }

    @Test
    void aTrailingBogeyOfAnotherCarriageAnchorsOnTheLookup() {
        for (WorldFold fold : TRANSLATING) {
            CarriageBogeyFrame frame = new CarriageBogeyFrame();
            CountingLookup lookup = lookupPastTheBound();
            frame.lead(fold, TRAIN, FIRST_CARRIAGE, new Vec3(INSIDE_X, BOGEY_HEIGHT, INSIDE_Z));

            Vec3 trailing = frame.trail(fold, TRAIN, SECOND_CARRIAGE,
                    new Vec3(INSIDE_X, BOGEY_HEIGHT, INSIDE_Z), lookup);

            assertEquals(new Vec3(INSIDE_X - WORLD_BLOCKS, BOGEY_HEIGHT, INSIDE_Z), trailing, "in " + fold);
            assertEquals(1, lookup.calls, "lookups in " + fold);
        }
    }

    @Test
    void aCarriageOnTheUnboundedAxisKeepsThatCoordinate() {
        CarriageBogeyFrame frame = new CarriageBogeyFrame();
        frame.lead(CYLINDER, TRAIN, FIRST_CARRIAGE, new Vec3(INSIDE_X, BOGEY_HEIGHT, FAR_ALONG_Z));

        Vec3 trailing = frame.trail(CYLINDER, TRAIN, FIRST_CARRIAGE,
                new Vec3(ACROSS_THE_SEAM_X, BOGEY_HEIGHT, FAR_ALONG_Z), lookupPastTheBound());

        assertEquals(new Vec3(ACROSS_THE_SEAM_X + WORLD_BLOCKS, BOGEY_HEIGHT, FAR_ALONG_Z), trailing);
    }

    @Test
    void withoutAFoldEveryBogeyKeepsTheRawPosition() {
        CarriageBogeyFrame frame = new CarriageBogeyFrame();
        CountingLookup lookup = lookupPastTheBound();
        Vec3 leading = new Vec3(PAST_THE_X_BOUND, BOGEY_HEIGHT, INSIDE_Z);
        Vec3 raw = new Vec3(ACROSS_THE_SEAM_X, BOGEY_HEIGHT, INSIDE_Z);

        assertSame(leading, frame.lead(null, TRAIN, FIRST_CARRIAGE, leading));
        assertSame(raw, frame.trail(null, TRAIN, FIRST_CARRIAGE, raw, lookup));
        assertEquals(0, lookup.calls);
    }
}
