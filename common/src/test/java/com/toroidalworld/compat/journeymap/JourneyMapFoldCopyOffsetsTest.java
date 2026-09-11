package com.toroidalworld.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

class JourneyMapFoldCopyOffsetsTest {
    private static final Rectangle2D.Double SCREEN = new Rectangle2D.Double(0.0, 0.0, 1000.0, 600.0);

    @Test
    void aBaseOnScreenWithNoPeriodDrawsOnceAtTheBase() {
        double[][] offsets = JourneyMapFold.copyOffsets(3, 3, 0.0, 0.0, rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{0.0, 0.0}}, offsets, "an unbounded map got a copy");
    }

    @Test
    void aBaseOffScreenWithNoPeriodDrawsNothing() {
        double[][] offsets = JourneyMapFold.copyOffsets(3, 3, 0.0, 0.0, rect(2000.0, 100.0, 50.0, 50.0), SCREEN);
        assertEquals(0, offsets.length, "an unbounded map drew an overlay past its screen");
    }

    @Test
    void everyCopyMeetingTheScreenIsListedAlongALoopedAxis() {
        double[][] offsets = JourneyMapFold.copyOffsets(5, 0, 400.0, 0.0, rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{0.0, 0.0}, {400.0, 0.0}, {800.0, 0.0}}, offsets,
                "laps 0..2 put the 50-px overlay at 100 inside a 1000-px screen with a 400-px period; lap -1 sits at -300");
    }

    @Test
    void aBaseOffScreenStillDrawsTheCopyThatIsOn() {
        double[][] offsets = JourneyMapFold.copyOffsets(5, 0, 400.0, 0.0, rect(-300.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{400.0, 0.0}, {800.0, 0.0}, {1200.0, 0.0}}, offsets,
                "the base at -300 is off screen; its copies at 100, 500 and 900 are on");
    }

    @Test
    void thePeriodLargerThanTheScreenStillReachesAcrossTheSeam() {
        double[][] offsets = JourneyMapFold.copyOffsets(1, 0, 4000.0, 0.0, rect(-3050.0, 100.0, 100.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{4000.0, 0.0}}, offsets,
                "one lap up puts the overlay at 950, the only copy inside the screen");
    }

    @Test
    void theRecordedRangeCapsTheLaps() {
        double[][] offsets = JourneyMapFold.copyOffsets(1, 0, 400.0, 0.0, rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{0.0, 0.0}, {400.0, 0.0}}, offsets,
                "lap 2 at 900 meets the screen but lies past the recorded range of 1");
    }

    @Test
    void twoLoopedAxesListTheProduct() {
        double[][] offsets = JourneyMapFold.copyOffsets(1, 1, 600.0, 500.0, rect(100.0, 100.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{0.0, 0.0}, {0.0, 500.0}, {600.0, 0.0}, {600.0, 500.0}}, offsets,
                "X laps 0..1 by Z laps 0..1, in X-major order");
    }

    @Test
    void anOverlayWiderThanAPeriodOverlapsItsOwnCopies() {
        double[][] offsets = JourneyMapFold.copyOffsets(2, 0, 100.0, 0.0, rect(0.0, 0.0, 250.0, 50.0), SCREEN);
        assertEquals(5, offsets.length, "laps -2..2 all meet a 1000-px screen with a 250-px overlay on a 100-px period");
    }

    @Test
    void aSingleCopyOverlayKeepsTheCopyNearestTheScreenCentre() {
        double[][] offsets = {{-400.0, 0.0}, {0.0, 0.0}, {400.0, 0.0}};
        double[][] kept = JourneyMapFold.nearestCopyOffset(offsets, rect(100.0, 275.0, 50.0, 50.0), SCREEN);
        assertArrayEquals(new double[][] {{400.0, 0.0}}, kept,
                "the copy at 500..550 sits nearest the screen centre at 500");
    }

    @Test
    void aSingleCopyOverlayOffEveryCopyStaysOff() {
        assertEquals(0, JourneyMapFold.nearestCopyOffset(new double[0][], rect(0.0, 0.0, 1.0, 1.0), SCREEN).length,
                "no visible copy produced one");
    }

    private static Rectangle2D.Double rect(double x, double y, double width, double height) {
        return new Rectangle2D.Double(x, y, width, height);
    }
}
