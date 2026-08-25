package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SeamTransformTest {
    private static final int SAMPLES = 200;
    private static final long SEED = 0x5EA3L;
    private static final int[] COORDS = {0, 1, -1, 7, -7, 255, -256, 4097, -4097};

    private static List<SeamTransform> transforms() {
        List<SeamTransform> built = new ArrayList<>();
        for (int xSign : new int[] {1, -1}) {
            for (int zSign : new int[] {1, -1}) {
                for (int shift : new int[] {0, 1, -1, 256, -96, 95}) {
                    built.add(new SeamTransform(xSign, zSign, shift, -shift));
                    built.add(new SeamTransform(xSign, zSign, -shift, shift));
                }
            }
        }

        return built;
    }

    @Nested
    class Algebra {
        @Test
        void compositionIsAHomomorphismOnCells() {
            for (SeamTransform first : transforms()) {
                for (SeamTransform second : transforms()) {
                    SeamTransform composed = first.then(second);
                    for (int coord : COORDS) {
                        assertEquals(second.applyCellX(first.applyCellX(coord)), composed.applyCellX(coord),
                                first + " then " + second + " disagrees on cell x " + coord);
                        assertEquals(second.applyCellZ(first.applyCellZ(coord)), composed.applyCellZ(coord),
                                first + " then " + second + " disagrees on cell z " + coord);
                    }
                }
            }
        }

        @Test
        void compositionIsAHomomorphismOnCoordinates() {
            for (SeamTransform first : transforms()) {
                for (SeamTransform second : transforms()) {
                    SeamTransform composed = first.then(second);
                    for (int coord : COORDS) {
                        assertEquals(second.applyX(first.applyX(coord + 0.5)), composed.applyX(coord + 0.5),
                                first + " then " + second + " disagrees on x " + coord);
                        assertEquals(second.applyZ(first.applyZ(coord + 0.5)), composed.applyZ(coord + 0.5),
                                first + " then " + second + " disagrees on z " + coord);
                    }
                }
            }
        }

        @Test
        void theInverseUndoesTheTransformInBothForms() {
            for (SeamTransform transform : transforms()) {
                SeamTransform inverse = transform.inverse();
                for (int coord : COORDS) {
                    assertEquals(coord, inverse.applyCellX(transform.applyCellX(coord)),
                            transform + " is not undone on cell x");
                    assertEquals(coord, inverse.applyCellZ(transform.applyCellZ(coord)),
                            transform + " is not undone on cell z");
                    assertEquals(coord + 0.5, inverse.applyX(transform.applyX(coord + 0.5)),
                            transform + " is not undone on x");
                    assertEquals(coord + 0.5, inverse.applyZ(transform.applyZ(coord + 0.5)),
                            transform + " is not undone on z");
                }
            }
        }

        @Test
        void powerIsRepeatedComposition() {
            for (SeamTransform transform : transforms()) {
                for (int exponent = -5; exponent <= 5; exponent++) {
                    SeamTransform repeated = SeamTransform.IDENTITY;
                    SeamTransform step = exponent < 0 ? transform.inverse() : transform;
                    for (int applied = 0; applied < Math.abs(exponent); applied++) {
                        repeated = repeated.then(step);
                    }

                    assertEquals(repeated, transform.power(exponent),
                            transform + " to the power " + exponent + " is not repeated composition");
                }
            }
        }

        @Test
        void aGlideSquaresToAPureTranslation() {
            SeamTransform glide = SeamTransform.glideX(256, 96);
            SeamTransform squared = glide.power(2);
            assertEquals(FoldOrientation.IDENTITY, squared.orientation(), "a glide squared still mirrors");
            assertEquals(new SeamTransform(1, 1, 512, 0), squared, "a glide squared is not a pure translation");
        }

        @Test
        void theOrientationFollowsTheSigns() {
            assertEquals(FoldOrientation.IDENTITY, SeamTransform.IDENTITY.orientation(), "identity mirrors");
            assertEquals(FoldOrientation.MIRROR_Z, SeamTransform.glideX(8, 0).orientation(), "a z glide is not z");
            assertEquals(FoldOrientation.MIRROR_X, SeamTransform.glideZ(0, 8).orientation(), "an x glide is not x");
            assertEquals(FoldOrientation.HALF_TURN, new SeamTransform(-1, -1, 0, 0).orientation(),
                    "a double mirror is not a half turn");
        }
    }

    @Nested
    class Guards {
        @Test
        void anAxisSignIsPlusOrMinusOne() {
            assertThrows(IllegalArgumentException.class, () -> new SeamTransform(0, 1, 0, 0), "sign 0 accepted");
            assertThrows(IllegalArgumentException.class, () -> new SeamTransform(1, 2, 0, 0), "sign 2 accepted");
        }

        @Test
        void aShiftTooLargeToRepresentIsRefusedRatherThanWrapped() {
            SeamTransform wide = SeamTransform.translation(Integer.MAX_VALUE / 2, 0);
            assertThrows(ArithmeticException.class, () -> wide.power(5), "an overflowing shift was wrapped");
        }
    }

    @Nested
    class CellsAgainstCoordinates {
        @Test
        void aCellCentreTransformsToTheTransformedCellCentre() {
            Random random = new Random(SEED);
            for (SeamTransform transform : transforms()) {
                for (int sample = 0; sample < SAMPLES; sample++) {
                    int coord = random.nextInt(20000) - 10000;
                    assertEquals(transform.applyCellX(coord) + 0.5, transform.applyX(coord + 0.5),
                            transform + " puts the cell centre elsewhere on x");
                    assertEquals(transform.applyCellZ(coord) + 0.5, transform.applyZ(coord + 0.5),
                            transform + " puts the cell centre elsewhere on z");
                }
            }
        }

        @Test
        void aMirrorShiftsCellsByOneAgainstCoordinates() {
            SeamTransform mirror = SeamTransform.glideX(0, 0);
            assertEquals(-1, mirror.applyCellZ(0), "the cell at 0 does not reflect onto -1");
            assertEquals(0.0, mirror.applyZ(0.0), "the coordinate 0 does not reflect onto itself");
            assertTrue(mirror.applyCellZ(5) == -6 && mirror.applyZ(5.0) == -5.0,
                    "the cell form and the coordinate form of a mirror are the same map");
        }
    }
}
