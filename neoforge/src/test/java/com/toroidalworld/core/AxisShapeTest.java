package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.world.phys.Vec3;

// The questions callers used to answer from a raw width, asked of the domain instead. Each is checked on real wrapping
// axes against a reference dumber than the production formula, and then on the disabled axis — where the width is a
// meaningless zero, and the arithmetic those callers did would divide by it, filter out everything, or cap a search
// that has no far side to stop at.
class AxisShapeTest {
    // Even centered, odd, one unit wide, uneven split — the same shapes the rest of the core suite is checked on.
    private static final List<WrapDomain> LOOPED = List.of(
            new WrapDomain(-32, 32),
            new WrapDomain(-2, 3),
            new WrapDomain(0, 1),
            new WrapDomain(-48, 16));

    private static final WrapDomain NOOP = new WrapDomain.Noop();

    private static final int[] COORDS = {0, 7, 123, -456, 2_000_000, -2_000_000};

    private static String in(WrapDomain domain) {
        return "in [" + domain.lowerBound + ", " + domain.upperBound + ")";
    }

    private static WorldLoopTransformer xOnly(int minChunk, int maxChunk) {
        return new WorldLoopTransformer(
                new WorldLoopBounds(new AxisBounds.Looped(minChunk, maxChunk), AxisBounds.Unbounded.INSTANCE));
    }

    @Nested
    class WrapIntoTheCallersOwnWindow {
        @Test
        void landsInTheWindowThatStartsAtTheAnchorAndNamesTheSamePlace() {
            for (WrapDomain domain : LOOPED) {
                for (int anchorStep = -70; anchorStep <= 70; anchorStep += 7) {
                    for (int coordStep = -70; coordStep <= 70; coordStep += 3) {
                        int anchor = anchorStep;
                        int coord = coordStep;
                        int inFrame = domain.wrapFrom(anchor, coord);

                        assertTrue(inFrame >= anchor && inFrame < anchor + domain.domainLength,
                                () -> "wrapFrom(" + anchor + ", " + coord + ") = " + inFrame
                                        + " left the window " + in(domain));
                        assertEquals(domain.wrap(coord), domain.wrap(inFrame),
                                () -> "wrapFrom(" + anchor + ", " + coord + ") changed the place " + in(domain));

                        int naive = coord;
                        while (naive < anchor) naive += domain.domainLength;
                        while (naive >= anchor + domain.domainLength) naive -= domain.domainLength;
                        assertEquals(naive, inFrame, () -> "wrapFrom(" + anchor + ", " + coord + ") " + in(domain));
                    }
                }
            }
        }

        @Test
        void aCoordinateAlreadyInTheWindowKeepsItsNumber() {
            WrapDomain domain = new WrapDomain(-32, 32);

            assertEquals(100, domain.wrapFrom(100, 100));
            assertEquals(163, domain.wrapFrom(100, 163));
        }

        @Test
        void disabledAxisHasOneReadingOfEveryCoordinate() {
            for (int coord : COORDS) {
                assertEquals(coord, NOOP.wrapFrom(1234, coord));
                assertEquals(coord, NOOP.wrapFrom(-1234, coord));
            }
        }
    }

    @Nested
    class OtherCopy {
        @Test
        void isOneWholeWorldAwayOnTheFarSideOfTheReference() {
            for (WrapDomain domain : LOOPED) {
                for (int coordStep = -70; coordStep <= 70; coordStep += 3) {
                    int coord = coordStep;
                    int ahead = domain.otherCopy(coord, 5);
                    int behind = domain.otherCopy(coord, -5);

                    assertTrue(ahead < coord, () -> "otherCopy(" + coord + ", ahead) went further ahead " + in(domain));
                    assertTrue(behind > coord, () -> "otherCopy(" + coord + ", behind) went further back " + in(domain));
                    assertEquals(domain.domainLength, coord - ahead, () -> "otherCopy(" + coord + ") " + in(domain));
                    assertEquals(domain.domainLength, behind - coord, () -> "otherCopy(" + coord + ") " + in(domain));
                    assertEquals(domain.wrap(coord), domain.wrap(ahead),
                            () -> "otherCopy(" + coord + ") names another place " + in(domain));
                    assertEquals(domain.wrap(coord), domain.wrap(behind),
                            () -> "otherCopy(" + coord + ") names another place " + in(domain));
                }
            }
        }

        @Test
        void disabledAxisOffersNoSecondCopy() {
            for (int coord : COORDS) {
                assertEquals(coord, NOOP.otherCopy(coord, 500));
                assertEquals(coord, NOOP.otherCopy(coord, -500));
            }
        }
    }

    @Nested
    class FoldsOntoItself {
        @Test
        void agreesWithWhetherWrappingSuchARunRepeatsAPlace() {
            for (WrapDomain domain : LOOPED) {
                for (int runLength = 1; runLength <= 2 * domain.domainLength + 2; runLength++) {
                    int count = runLength;
                    Set<Integer> seen = new HashSet<>();
                    boolean repeats = false;
                    for (int step = 0; step < count; step++) {
                        repeats |= !seen.add(domain.wrap(-7 + step));
                    }

                    boolean expected = repeats;
                    assertEquals(expected, domain.foldsOntoItself(count),
                            () -> "foldsOntoItself(" + count + ") " + in(domain));
                }
            }
        }

        @Test
        void aRunExactlyAWorldWideStillNamesEachPlaceOnce() {
            for (WrapDomain domain : LOOPED) {
                assertFalse(domain.foldsOntoItself(domain.domainLength), () -> "a full lap " + in(domain));
                assertTrue(domain.foldsOntoItself(domain.domainLength + 1), () -> "one past a full lap " + in(domain));
            }
        }

        @Test
        void disabledAxisNeverRepeatsHoweverLongTheRun() {
            assertFalse(NOOP.foldsOntoItself(1));
            assertFalse(NOOP.foldsOntoItself(60_000_000));
            assertFalse(NOOP.foldsOntoItself(Integer.MAX_VALUE));
        }
    }

    @Nested
    class StepsToCoverTheWorld {
        @Test
        void isTheFewestStepsThatReachHalfTheWorld() {
            for (WrapDomain domain : LOOPED) {
                int halfWorld = (domain.domainLength + 1) / 2;
                for (int stepSize = 1; stepSize <= 40; stepSize++) {
                    int step = stepSize;
                    int naive = 0;
                    while ((long) naive * step < halfWorld) {
                        naive++;
                    }

                    assertEquals(naive, domain.stepsToCoverTheWorld(step),
                            () -> "stepsToCoverTheWorld(" + step + ") " + in(domain));
                }
            }
        }

        @Test
        void oneStepFewerFallsShortOfHalfTheWorld() {
            for (WrapDomain domain : LOOPED) {
                int halfWorld = (domain.domainLength + 1) / 2;
                for (int stepSize = 1; stepSize <= 40; stepSize++) {
                    int step = stepSize;
                    int steps = domain.stepsToCoverTheWorld(step);

                    assertTrue((long) steps * step >= halfWorld,
                            () -> "stepsToCoverTheWorld(" + step + ") stops short " + in(domain));
                    assertTrue((long) (steps - 1) * step < halfWorld,
                            () -> "stepsToCoverTheWorld(" + step + ") overshoots " + in(domain));
                }
            }
        }

        @Test
        void disabledAxisIsNeverCovered() {
            for (int step : new int[] {1, 34, 4096}) {
                assertEquals(Integer.MAX_VALUE, NOOP.stepsToCoverTheWorld(step));
            }
        }

        @Test
        void aStepThatDoesNotAdvanceCoversNothing() {
            for (WrapDomain domain : LOOPED) {
                assertEquals(Integer.MAX_VALUE, domain.stepsToCoverTheWorld(0), () -> "a step of no width " + in(domain));
                assertEquals(Integer.MAX_VALUE, domain.stepsToCoverTheWorld(-8), () -> "a backwards step " + in(domain));
            }
        }
    }

    @Nested
    class MappingBetweenWorlds {
        // What the dimensions themselves say about a nether: an eighth of the overworld, whatever their widths are.
        private static final double DECLARED = 1.0 / 8.0;

        private final WrapDomain wide = new WrapDomain(-512, 512);
        private final WrapDomain narrow = new WrapDomain(-64, 64);

        @Test
        void twoClosedAxesTakeTheirOwnWidthsRatioWhateverTheDimensionsDeclare() {
            assertEquals(0.125, narrow.scaleFrom(wide, 3.0));
            assertEquals(8.0, wide.scaleFrom(narrow, 3.0));
            assertEquals(32.0, narrow.mapFrom(wide, 256.0, 3.0));
        }

        @Test
        void aMappedCoordinateIsFoldedIntoTheDestination() {
            assertEquals(-64.0, narrow.mapFrom(wide, 512.0, DECLARED));
            assertEquals(-32.0, narrow.mapFrom(wide, 768.0, DECLARED));
        }

        @Test
        void anAxisWithoutAWidthKeepsTheScaleTheDimensionsDeclare() {
            assertEquals(DECLARED, narrow.scaleFrom(NOOP, DECLARED));
            assertEquals(DECLARED, NOOP.scaleFrom(wide, DECLARED));
            assertEquals(DECLARED, NOOP.scaleFrom(NOOP, DECLARED));

            assertEquals(12.5, narrow.mapFrom(NOOP, 100.0, DECLARED));
            assertEquals(1.25e8, NOOP.mapFrom(wide, 1.0e9, DECLARED));
        }

        @Test
        void noWidthIsEverDividedBy() {
            for (WrapDomain destination : List.of(wide, NOOP)) {
                for (WrapDomain source : List.of(narrow, NOOP)) {
                    double mapped = destination.mapFrom(source, 1234.0, DECLARED);

                    assertTrue(Double.isFinite(mapped),
                            () -> "mapFrom gave " + mapped + " crossing " + in(source) + " to " + in(destination));
                }
            }
        }

        @Test
        void aWorldLoopedInOneAxisScalesThatAxisAndDeclaresTheOther() {
            Vec3 mapped = xOnly(-4, 4).mapFrom(xOnly(-32, 32), new Vec3(256.0, 70.0, 800.0), DECLARED);

            assertEquals(32.0, mapped.x);
            assertEquals(70.0, mapped.y);
            assertEquals(100.0, mapped.z);
        }

        @Test
        void anUnwrappedWorldMapsByTheDeclaredScaleAlone() {
            Vec3 mapped = WorldLoopTransformer.NOOP.mapFrom(
                    WorldLoopTransformer.NOOP, new Vec3(80.0, 70.0, -160.0), DECLARED);

            assertEquals(10.0, mapped.x);
            assertEquals(70.0, mapped.y);
            assertEquals(-20.0, mapped.z);
        }

        @Test
        void aPositionThatMapsToItselfComesBackUntouched() {
            WorldLoopTransformer world = xOnly(-32, 32);
            Vec3 position = new Vec3(100.0, 70.0, -3000.0);

            assertSame(position, world.mapFrom(world, position, 1.0));
        }
    }
}
