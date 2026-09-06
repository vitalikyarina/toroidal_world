package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.world.phys.Vec3;

class JomlVectorsTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int LOWER = MIN_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int UPPER = MAX_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int WIDTH = UPPER - LOWER;

    private static final double ANCHOR_X = UPPER - 8.0;
    private static final double ACROSS_X = LOWER + 8.0;
    private static final double NEAR_X = ANCHOR_X - 4.0;
    private static final double Y = 70.0;
    private static final double Z = 4.0;

    private static final WorldLoopBounds SQUARE = new WorldLoopBounds(MIN_CHUNK, MAX_CHUNK, MIN_CHUNK, MAX_CHUNK);
    private static final List<WorldFold> FOLDS =
            List.of(WorldFolds.of(FlatShape.torus(SQUARE)), new DeckGroupFold(FlatShape.torus(SQUARE)));

    private static Vec3 anchor() {
        return new Vec3(ANCHOR_X, Y, Z);
    }

    private static Vector3d at(double x) {
        return new Vector3d(x, Y, Z);
    }

    @Test
    void readCarriesTheThreeComponents() {
        assertEquals(new Vec3(1.5, -2.5, 3.5), JomlVectors.read(new Vector3d(1.5, -2.5, 3.5)));
    }

    @Test
    void centreIsTheMidpointOfEveryAxis() {
        assertEquals(new Vec3(1.0, 5.0, -2.0), JomlVectors.centre(-3.0, 0.0, -6.0, 5.0, 10.0, 2.0));
    }

    @Test
    void aTargetAcrossTheSeamIsSeatedBesideTheAnchor() {
        for (WorldFold fold : FOLDS) {
            assertEquals(at(ACROSS_X + WIDTH), JomlVectors.seat(fold, anchor(), at(ACROSS_X)));
        }
    }

    @Test
    void aSeatedTargetIsANewVectorAndLeavesTheArgumentWhereItWas() {
        for (WorldFold fold : FOLDS) {
            Vector3d across = at(ACROSS_X);
            Vector3d seated = JomlVectors.seat(fold, anchor(), across);

            assertNotSame(across, seated);
            assertEquals(at(ACROSS_X), across);
        }
    }

    @Test
    void theSeatCarriesTheHeightItWasGiven() {
        for (WorldFold fold : FOLDS) {
            assertEquals(Y, JomlVectors.seat(fold, anchor(), at(ACROSS_X)).y);
        }
    }

    @Test
    void aTargetOnTheAnchorsOwnSideComesBackByIdentity() {
        for (WorldFold fold : FOLDS) {
            Vector3d near = at(NEAR_X);

            assertSame(near, JomlVectors.seat(fold, anchor(), near));
        }
    }

    @Test
    void anUnwrappedWorldGivesTheTargetBackByIdentity() {
        Vector3d across = at(ACROSS_X);

        assertSame(across, JomlVectors.seat(WorldFolds.NOOP, anchor(), across));
    }

    @Test
    void seatedKeepsTheSourceWhenTheFoldHandedItsInputBack() {
        Vector3d source = at(ACROSS_X);
        Vec3 raw = JomlVectors.read(source);

        assertSame(source, JomlVectors.seated(source, raw, raw));
        assertEquals(new Vector3d(ACROSS_X + WIDTH, Y, Z),
                JomlVectors.seated(source, raw, new Vec3(ACROSS_X + WIDTH, Y, Z)));
    }

    @Test
    void writeSetsTheTargetInPlaceAndHandsItBack() {
        Vector3d target = at(ACROSS_X);

        assertSame(target, JomlVectors.write(new Vec3(1.5, -2.5, 3.5), target));
        assertEquals(new Vector3d(1.5, -2.5, 3.5), target);
    }
}
