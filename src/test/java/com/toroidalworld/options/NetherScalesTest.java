package com.toroidalworld.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// The reference walks every candidate up to the width: a scale is allowed when it divides the width evenly and leaves
// a nether no narrower than the smallest working looped world. The production divisor-pair walk must agree with it on
// every width shape — the minimum itself, odd, prime, and comfortably large.
class NetherScalesTest {
    private static final List<Integer> WIDTHS = List.of(16, 17, 20, 32, 64, 81, 128, 160, 1009, 4096);

    private static List<Integer> allowedNaive(int overworldChunkWidth) {
        List<Integer> allowed = new ArrayList<>();
        for (int scale = 1; scale <= overworldChunkWidth; scale++) {
            if (overworldChunkWidth % scale == 0 && overworldChunkWidth / scale >= WorldLoopSizes.MIN_CHUNK_WIDTH) {
                allowed.add(scale);
            }
        }
        return allowed;
    }

    @Nested
    class AllowedScales {
        @Test
        void matchTheExhaustiveReferenceOnEveryWidthShape() {
            for (int width : WIDTHS) {
                assertEquals(allowedNaive(width), NetherScales.allowedFor(width), () -> "width " + width);
            }
        }

        @Test
        void everyAllowedScaleDividesTheWidthAndLeavesAWorkingNether() {
            for (int width : WIDTHS) {
                for (int scale : NetherScales.allowedFor(width)) {
                    assertEquals(0, width % scale, "width " + width + " scale " + scale);
                    assertTrue(NetherScales.netherChunkWidth(width, scale) >= WorldLoopSizes.MIN_CHUNK_WIDTH,
                            "width " + width + " scale " + scale + " leaves too narrow a nether");
                    assertEquals(width, NetherScales.netherChunkWidth(width, scale) * scale,
                            "width " + width + " scale " + scale + " does not divide evenly");
                }
            }
        }

        @Test
        void theSmallestWorldAdmitsOnlyOneToOne() {
            assertEquals(List.of(1), NetherScales.allowedFor(WorldLoopSizes.MIN_CHUNK_WIDTH));
            assertEquals(List.of(1), NetherScales.allowedFor(WorldLoopSizes.MIN_CHUNK_WIDTH - 2));
        }

        @Test
        void vanillasEightToOneNeedsAWorldEightMinimumsWide() {
            assertTrue(NetherScales.allowedFor(8 * WorldLoopSizes.MIN_CHUNK_WIDTH).contains(NetherScales.DEFAULT));
            assertFalse(NetherScales.allowedFor(8 * WorldLoopSizes.MIN_CHUNK_WIDTH - 16).contains(NetherScales.DEFAULT));
        }
    }

    @Nested
    class Normalization {
        @Test
        void returnsTheNearestMemberOfTheAllowedListAndBothOverloadsAgree() {
            for (int width : WIDTHS) {
                List<Integer> allowed = NetherScales.allowedFor(width);
                for (int scale = 0; scale <= 300; scale++) {
                    int normalized = NetherScales.normalize(scale, width);
                    assertTrue(allowed.contains(normalized),
                            "width " + width + ": normalize(" + scale + ") = " + normalized + " is not allowed");
                    for (int candidate : allowed) {
                        assertTrue(Math.abs(normalized - scale) <= Math.abs(candidate - scale),
                                "width " + width + ": normalize(" + scale + ") = " + normalized
                                        + " is further than " + candidate);
                    }
                    assertEquals(normalized, NetherScales.normalize(scale, allowed),
                            "width " + width + ": the list overload disagrees on " + scale);
                }
            }
        }

        @Test
        void aMemberNormalizesToItself() {
            for (int width : WIDTHS) {
                for (int scale : NetherScales.allowedFor(width)) {
                    assertEquals(scale, NetherScales.normalize(scale, width), "width " + width);
                }
            }
        }

        @Test
        void tiesGoToTheLargerScale() {
            assertEquals(4, NetherScales.normalize(3, 128));
            assertEquals(8, NetherScales.normalize(6, 128));
        }
    }

    @Nested
    class Cycling {
        @Test
        void nextWalksTheWholeListInOrderAndWrapsAround() {
            for (int width : WIDTHS) {
                List<Integer> allowed = NetherScales.allowedFor(width);
                for (int index = 0; index < allowed.size(); index++) {
                    assertEquals(allowed.get((index + 1) % allowed.size()),
                            NetherScales.next(allowed.get(index), width),
                            "width " + width + " from " + allowed.get(index));
                }
            }
        }

        @Test
        void anUnknownScaleAdvancesToTheFirstAllowed() {
            assertEquals(1, NetherScales.next(7, 128));
            assertEquals(1, NetherScales.next(-3, 128));
        }
    }
}
