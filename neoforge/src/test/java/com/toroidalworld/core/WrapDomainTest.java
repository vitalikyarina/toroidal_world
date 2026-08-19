package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WrapDomainTest {
    private static final long SEED = 0x5EEDL;
    private static final int SAMPLES = 2000;

    private static final List<WrapDomain> DOMAINS = List.of(
            new WrapDomain(-32, 32),
            new WrapDomain(-2, 3),
            new WrapDomain(0, 1),
            new WrapDomain(-48, 16),
            new WrapDomain(0, 16));

    private static int sampleCoord(Random random, WrapDomain domain) {
        int reach = 3 * domain.domainLength;
        return domain.lowerBound - reach + random.nextInt(6 * domain.domainLength + 1);
    }

    private static int wrapNaive(WrapDomain domain, int coord) {
        int wrapped = coord;
        while (wrapped >= domain.upperBound) wrapped -= domain.domainLength;
        while (wrapped < domain.lowerBound) wrapped += domain.domainLength;
        return wrapped;
    }

    private static double wrapNaive(WrapDomain domain, double coord) {
        double wrapped = coord;
        while (wrapped >= domain.upperBound) wrapped -= domain.domainLength;
        while (wrapped < domain.lowerBound) wrapped += domain.domainLength;
        return wrapped;
    }

    private static String in(WrapDomain domain) {
        return "in [" + domain.lowerBound + ", " + domain.upperBound + ")";
    }

    @Nested
    class HalfOpenInterval {
        @Test
        void upperBoundFoldsToLowerBound() {
            for (WrapDomain domain : DOMAINS) {
                assertEquals(domain.lowerBound, domain.wrap(domain.upperBound), in(domain));
                assertEquals(domain.lowerBound, domain.wrap((double) domain.upperBound), in(domain));
            }
        }

        @Test
        void lastCoordinateInsideStaysPut() {
            for (WrapDomain domain : DOMAINS) {
                int last = domain.upperBound - 1;
                assertEquals(last, domain.wrap(last), in(domain));

                double justBelow = Math.nextDown((double) domain.upperBound);
                assertEquals(justBelow, domain.wrap(justBelow), in(domain));
            }
        }

        @Test
        void lowerBoundStaysPut() {
            for (WrapDomain domain : DOMAINS) {
                assertEquals(domain.lowerBound, domain.wrap(domain.lowerBound), in(domain));
                assertEquals(domain.lowerBound, domain.wrap((double) domain.lowerBound), in(domain));
            }
        }

        @Test
        void justBelowLowerBoundFoldsToTop() {
            for (WrapDomain domain : DOMAINS) {
                assertEquals(domain.upperBound - 1, domain.wrap(domain.lowerBound - 1), in(domain));

                double justBelow = Math.nextDown((double) domain.lowerBound);
                double wrapped = domain.wrap(justBelow);
                assertFalse(domain.isOver(wrapped), () -> "wrap(" + justBelow + ") gave " + wrapped + " " + in(domain));
            }
        }

        @Test
        void overshootCountsFromTheLastCoordinateInside() {
            WrapDomain domain = new WrapDomain(-32, 32);
            assertEquals(0, domain.overshoot(-32));
            assertEquals(0, domain.overshoot(0));
            assertEquals(0, domain.overshoot(31));
            assertEquals(1, domain.overshoot(32));
            assertEquals(6, domain.overshoot(37));
            assertEquals(1, domain.overshoot(-33));
            assertEquals(6, domain.overshoot(-38));
        }
    }

    @Nested
    class FarOut {
        @Test
        void negativeCoordinatesFoldIntoTheDomain() {
            WrapDomain domain = new WrapDomain(-32, 32);
            assertEquals(31, domain.wrap(-33));
            assertEquals(28, domain.wrap(-100));
            assertEquals(28.5, domain.wrap(-99.5));
        }

        @Test
        void wholeWorldWidthsAwayLandOnTheSameSpot() {
            for (WrapDomain domain : DOMAINS) {
                for (int coord : new int[] {domain.lowerBound, domain.upperBound - 1, 0, 7}) {
                    for (int laps = -3; laps <= 3; laps++) {
                        int shifted = coord + laps * domain.domainLength;
                        assertEquals(domain.wrap(coord), domain.wrap(shifted),
                                "wrap(" + shifted + ") " + in(domain));
                    }
                }
            }
        }
    }

    @Nested
    class WidthVariants {
        @Test
        void oddWidthFoldsBothWays() {
            WrapDomain domain = new WrapDomain(-2, 3);
            assertEquals(-2, domain.wrap(3));
            assertEquals(2, domain.wrap(-3));
            assertEquals(-2, domain.wrap(8));
            assertEquals(-2, domain.foldDelta(3));
            assertEquals(2, domain.foldDelta(-3));
        }

        @Test
        void oneUnitWideWorldFoldsEverythingToItsOnlyCoordinate() {
            WrapDomain domain = new WrapDomain(0, 1);
            for (int coord = -5; coord <= 5; coord++) {
                assertEquals(0, domain.wrap(coord));
            }
            assertEquals(5, domain.overshoot(5));
            assertEquals(5, domain.overshoot(-5));
        }

        @Test
        void unevenSplitKeepsItsOwnBounds() {
            WrapDomain domain = new WrapDomain(-48, 16);
            assertEquals(-48, domain.wrap(16));
            assertEquals(-1, domain.wrap(63));
            assertEquals(15, domain.wrap(-49));
        }
    }

    @Nested
    class DisabledAxis {
        private final WrapDomain noop = new WrapDomain.Noop();

        @Test
        void everyOperationIsTheIdentity() {
            for (int coord : new int[] {0, 123, -456, 2_000_000, -2_000_000}) {
                assertEquals(coord, noop.wrap(coord));
                assertEquals(coord + 0.5, noop.wrap(coord + 0.5));
                assertFalse(noop.isOver(coord));
                assertFalse(noop.isOver(coord + 0.5));
                assertEquals(0, noop.overshoot(coord));
                assertEquals(coord, noop.unwrap(7, coord));
                assertEquals(coord + 0.5, noop.unwrap(7.0, coord + 0.5));
                assertEquals(coord, (int) noop.unwrapAround(7.0, coord));
                assertEquals(coord, noop.foldDelta(coord));
                assertEquals(coord + 0.5, noop.foldDelta(coord + 0.5));
                assertEquals((double) coord * coord, noop.sqrDistToBounds((double) coord));
            }
        }

        @Test
        void nothingSpansTheSeamHoweverLongTheStretch() {
            assertFalse(noop.spansSeam(-30_000_000, 30_000_000));
            assertFalse(noop.spansSeam(0, Integer.MAX_VALUE));
        }

        @Test
        void spanQuestionsAnswerByMeaningNotArithmetic() {
            for (double span : new double[] {0.0, 1.0, 60_000_000.0, Double.MAX_VALUE}) {
                assertTrue(noop.fitsInHalf(span));
                assertFalse(noop.coversWorld(span));
            }
        }

        @Test
        void overlapsIsPlainIntersection() {
            assertTrue(noop.overlaps(0, 10, 5, 15));
            assertTrue(noop.overlaps(0, 10, 10, 20));
            assertFalse(noop.overlaps(0, 10, 11, 20));
            assertFalse(noop.overlaps(-30_000_000, -20_000_000, 20_000_000, 30_000_000));
        }

        @Test
        void spansStayASingleUnbrokenStretch() {
            List<double[]> spans = noop.spans(-5_000_000, 5_000_000);
            assertEquals(1, spans.size());
            assertEquals(-5_000_000, spans.get(0)[0]);
            assertEquals(5_000_000, spans.get(0)[1]);
        }
    }

    @Nested
    class Properties {
        @Test
        void wrapIsIdempotentLandsInBoundsAndMatchesTheNaiveFold() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int coord = sampleCoord(random, domain);
                    int wrapped = domain.wrap(coord);
                    assertFalse(domain.isOver(wrapped), () -> "isOver(wrap(" + coord + ")) " + in(domain));
                    assertEquals(wrapped, domain.wrap(wrapped), () -> "wrap∘wrap(" + coord + ") " + in(domain));
                    assertEquals(wrapNaive(domain, coord), wrapped, () -> "wrap(" + coord + ") " + in(domain));

                    double coordD = coord + random.nextDouble();
                    double wrappedD = domain.wrap(coordD);
                    assertFalse(domain.isOver(wrappedD), () -> "isOver(wrap(" + coordD + ")) " + in(domain));
                    assertEquals(wrappedD, domain.wrap(wrappedD), 0.0, () -> "wrap∘wrap(" + coordD + ") " + in(domain));
                    assertEquals(wrapNaive(domain, coordD), wrappedD, 1e-9, () -> "wrap(" + coordD + ") " + in(domain));
                }
            }
        }

        @Test
        void unwrapAroundReturnsTheCopyNearestTheReference() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double ref = sampleCoord(random, domain) + random.nextDouble();
                    double coord = sampleCoord(random, domain) + random.nextDouble();
                    double unwrapped = domain.unwrapAround(ref, coord);

                    assertEquals(domain.wrap(coord), domain.wrap(unwrapped), 1e-6,
                            () -> "unwrapAround(" + ref + ", " + coord + ") changed the position " + in(domain));

                    double best = Double.MAX_VALUE;
                    for (int laps = -4; laps <= 4; laps++) {
                        best = Math.min(best, Math.abs(domain.wrap(coord) + (double) laps * domain.domainLength - ref));
                    }
                    assertEquals(best, Math.abs(unwrapped - ref), 1e-6,
                            () -> "unwrapAround(" + ref + ", " + coord + ") is not the nearest copy " + in(domain));
                    assertTrue(Math.abs(unwrapped - ref) <= domain.domainLength / 2.0 + 1e-9,
                            () -> "unwrapAround(" + ref + ", " + coord + ") is over half a world away " + in(domain));
                }
            }
        }

        @Test
        void unwrapAroundShiftsByWholeWorldsAndTouchesNothingElse() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double ref = sampleCoord(random, domain) + random.nextDouble();
                    double coord = sampleCoord(random, domain) + random.nextDouble();
                    double unwrapped = domain.unwrapAround(ref, coord);
                    double laps = (coord - unwrapped) / domain.domainLength;

                    assertEquals(Math.round(laps), laps, 1e-9,
                            () -> "unwrapAround(" + ref + ", " + coord + ") left its lattice " + in(domain));
                    if (Math.round(laps) == 0L) {
                        assertEquals(coord, unwrapped, 0.0,
                                () -> "unwrapAround(" + ref + ", " + coord + ") moved a coordinate that was already at "
                                        + "its nearest copy " + in(domain));
                    }
                }
            }
        }

        @Test
        void foldingTwiceIsFoldingOnce() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double ref = sampleCoord(random, domain) + random.nextDouble();
                    double coord = sampleCoord(random, domain) + random.nextDouble();
                    double once = domain.unwrapAround(ref, coord);

                    assertEquals(once, domain.unwrapAround(ref, once), 0.0,
                            () -> "unwrapAround(" + ref + ", " + coord + ") is not settled after one fold "
                                    + in(domain));
                }
            }
        }

        @Test
        void theAntipodeResolvesTheSameWhateverTheBoundsAre() {
            for (WrapDomain domain : List.of(new WrapDomain(-32, 32), new WrapDomain(-48, 16))) {
                assertEquals(-12, domain.unwrap(20, -12), in(domain));
                assertEquals(-12.0, domain.unwrap(20.0, -12.0), 0.0, in(domain));
                assertEquals(-12, domain.unwrap(-44, -12), in(domain));
                assertEquals(-12.0, domain.unwrap(-44.0, -12.0), 0.0, in(domain));

                assertEquals(52, domain.unwrapAround(20, 52), in(domain));
                assertEquals(52.0, domain.unwrapAround(20.0, 52.0), 0.0, in(domain));

                assertEquals(52, domain.unwrapAround(20, 116), in(domain));
                assertEquals(52.0, domain.unwrapAround(20.0, 116.0), 0.0, in(domain));
                assertEquals(-12, domain.unwrapAround(20, -76), in(domain));
                assertEquals(-12.0, domain.unwrapAround(20.0, -76.0), 0.0, in(domain));
            }
        }

        @Test
        void foldDeltaTakesTheShortWayRound() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int delta = random.nextInt(2 * domain.domainLength - 1) - (domain.domainLength - 1);
                    int folded = domain.foldDelta(delta);
                    assertEquals(0, Math.floorMod(delta - folded, domain.domainLength),
                            () -> "foldDelta(" + delta + ") left its lattice " + in(domain));
                    assertTrue(Math.abs(folded) * 2 <= domain.domainLength,
                            () -> "foldDelta(" + delta + ") = " + folded + " is over half a world " + in(domain));

                    double deltaD = delta + (delta < 0 ? -random.nextDouble() : random.nextDouble()) % 1.0;
                    if (Math.abs(deltaD) >= domain.domainLength) continue;
                    double foldedD = domain.foldDelta(deltaD);
                    double laps = (deltaD - foldedD) / domain.domainLength;
                    assertEquals(Math.round(laps), laps, 1e-9,
                            () -> "foldDelta(" + deltaD + ") left its lattice " + in(domain));
                    assertTrue(Math.abs(foldedD) <= domain.domainLength / 2.0 + 1e-9,
                            () -> "foldDelta(" + deltaD + ") = " + foldedD + " is over half a world " + in(domain));
                }
            }
        }

        @Test
        void sqrDistToBoundsIsTheMinimumOverWorldCopies() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int delta = random.nextInt(2 * domain.domainLength - 1) - (domain.domainLength - 1);
                    long best = Long.MAX_VALUE;
                    for (int laps = -2; laps <= 2; laps++) {
                        long shifted = delta + (long) laps * domain.domainLength;
                        best = Math.min(best, shifted * shifted);
                    }
                    assertEquals(best, domain.sqrDistToBounds(delta),
                            () -> "sqrDistToBounds(" + delta + ") " + in(domain));
                    assertEquals(best, domain.sqrDistToBounds((double) delta), 1e-9,
                            () -> "sqrDistToBounds(" + (double) delta + ") " + in(domain));
                }
            }
        }

        @Test
        void deltaFromBoundsAgreesWithItsUnwrap() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double from = sampleCoord(random, domain) + random.nextDouble();
                    double to = domain.wrap(sampleCoord(random, domain) + random.nextDouble());
                    double delta = domain.deltaFromBounds(from, to);

                    double laps = (to - (from + delta)) / domain.domainLength;
                    assertEquals(Math.round(laps), laps, 1e-9,
                            () -> "deltaFromBounds(" + from + ", " + to + ") left its lattice " + in(domain));
                    assertTrue(Math.abs(delta) <= domain.domainLength / 2.0 + 1e-9,
                            () -> "deltaFromBounds(" + from + ", " + to + ") is over half a world " + in(domain));
                }
            }
        }

        @Test
        void spansSeamAgreesWithDoubledDistanceAgainstTheWholeWidth() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int from = sampleCoord(random, domain);
                    int to = sampleCoord(random, domain);
                    boolean expected = 2 * Math.abs((long) to - from) > domain.domainLength;
                    assertEquals(expected, domain.spansSeam(from, to),
                            () -> "spansSeam(" + from + ", " + to + ") " + in(domain));
                }

                int radius = domain.domainLength / 2;
                assertFalse(domain.spansSeam(domain.lowerBound, domain.lowerBound + radius));
                if (radius + 1 < domain.domainLength) {
                    assertTrue(domain.spansSeam(domain.lowerBound, domain.lowerBound + radius + 1));
                }
            }
        }

        @Test
        void spansSeamSurvivesTheFullIntRange() {
            WrapDomain domain = new WrapDomain(-32, 32);
            assertTrue(domain.spansSeam(Integer.MIN_VALUE, Integer.MAX_VALUE));
            assertTrue(domain.spansSeam(Integer.MAX_VALUE, Integer.MIN_VALUE));
        }

        @Test
        void foldedSpanIsTheComplementOfTheDirectReading() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int a = domain.wrap(sampleCoord(random, domain));
                    int b = domain.wrap(sampleCoord(random, domain));
                    int min = Math.min(a, b);
                    int max = Math.max(a, b);
                    int start = domain.foldSpanStart(min, max);
                    int end = domain.foldSpanEnd(min, max);

                    if (domain.spansSeam(min, max)) {
                        assertEquals(domain.domainLength, (end - start) + (max - min),
                                () -> "folded [" + min + ", " + max + "] " + in(domain));
                        assertFalse(domain.isOver(start),
                                () -> "foldSpanStart(" + min + ", " + max + ") left the domain " + in(domain));
                        assertTrue(end >= start,
                                () -> "folded [" + min + ", " + max + "] runs backwards " + in(domain));
                    } else {
                        assertEquals(min, start);
                        assertEquals(max, end);
                    }
                }
            }
        }

        @Test
        void overlapsAgreesWithTheLatticeOfWorldCopies() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int aMin = sampleCoord(random, domain);
                    int aMax = aMin + random.nextInt(2 * domain.domainLength + 1);
                    int bMin = sampleCoord(random, domain);
                    int bMax = bMin + random.nextInt(2 * domain.domainLength + 1);

                    boolean expected = false;
                    for (int laps = -16; laps <= 16 && !expected; laps++) {
                        long shift = (long) laps * domain.domainLength;
                        expected = aMin + shift <= bMax && bMin <= aMax + shift;
                    }
                    boolean finalExpected = expected;
                    assertEquals(expected, domain.overlaps(aMin, aMax, bMin, bMax),
                            () -> "overlaps([" + aMin + ", " + aMax + "], [" + bMin + ", " + bMax + "]) should be "
                                    + finalExpected + " " + in(domain));
                }
            }
        }

        @Test
        void spansCoverExactlyTheStretchAndStayInBounds() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double min = sampleCoord(random, domain) + random.nextDouble();
                    double length = random.nextDouble() * 2 * domain.domainLength;
                    List<double[]> spans = domain.spans(min, min + length);

                    double total = 0;
                    for (double[] span : spans) {
                        assertTrue(span[0] >= domain.lowerBound - 1e-9,
                                () -> "span starts below the world " + in(domain));
                        assertTrue(span[1] <= domain.upperBound + 1e-9,
                                () -> "span ends past the world " + in(domain));
                        total += span[1] - span[0];
                    }
                    assertEquals(Math.min(length, domain.domainLength), total, 1e-6,
                            () -> "spans(" + min + ", " + (min + length) + ") cover the wrong total " + in(domain));
                    assertEquals(domain.wrap(min), spans.get(0)[0], 1e-9,
                            () -> "spans(" + min + ", " + (min + length) + ") start off the wrapped low edge " + in(domain));
                    if (spans.size() == 2) {
                        assertEquals(domain.upperBound, spans.get(0)[1], 0.0);
                        assertEquals(domain.lowerBound, spans.get(1)[0], 0.0);
                    }
                }
            }
        }
    }

    @Nested
    class NearsAntipode {
        private final WrapDomain domain = new WrapDomain(-32, 32);

        @Test
        void firesOnlyInsideTheBandBeforeHalf() {
            assertFalse(domain.nearsAntipode(29.9, 2.0));
            assertFalse(domain.nearsAntipode(-30.0, 2.0));
            assertTrue(domain.nearsAntipode(30.5, 2.0));
            assertTrue(domain.nearsAntipode(-32.0, 2.0));
        }

        @Test
        void disabledAxisHasNoAntipodeToNear() {
            WrapDomain noop = new WrapDomain.Noop();
            assertFalse(noop.nearsAntipode(1.0e9, 2.0));
            assertFalse(noop.nearsAntipode(-1.0e9, 2.0));
        }
    }

    @Nested
    class IntDoubleAgreement {
        @Test
        void bothOverloadsAgreeOnIntegerInput() {
            Random random = new Random(SEED);
            for (WrapDomain domain : DOMAINS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int coord = sampleCoord(random, domain);
                    assertEquals(domain.wrap(coord), domain.wrap((double) coord), 0.0,
                            () -> "wrap(" + coord + ") " + in(domain));

                    int ref = sampleCoord(random, domain);
                    int wrapped = domain.wrap(coord);
                    assertEquals(domain.unwrap(ref, wrapped), domain.unwrap((double) ref, (double) wrapped), 0.0,
                            () -> "unwrap(" + ref + ", " + wrapped + ") " + in(domain));

                    int delta = random.nextInt(2 * domain.domainLength - 1) - (domain.domainLength - 1);
                    assertEquals(domain.foldDelta(delta), domain.foldDelta((double) delta), 0.0,
                            () -> "foldDelta(" + delta + ") " + in(domain));
                    assertEquals(domain.sqrDistToBounds(delta), domain.sqrDistToBounds((double) delta), 0.0,
                            () -> "sqrDistToBounds(" + delta + ") " + in(domain));
                }
            }
        }

        @Test
        void bothOverloadsAgreeAtTheBoundsThemselves() {
            for (WrapDomain domain : DOMAINS) {
                for (int coord : new int[] {domain.lowerBound, domain.upperBound, domain.upperBound - 1,
                        domain.lowerBound - 1, domain.upperBound + domain.domainLength}) {
                    assertEquals(domain.wrap(coord), domain.wrap((double) coord), 0.0,
                            () -> "wrap(" + coord + ") " + in(domain));
                }
            }
        }
    }
}
