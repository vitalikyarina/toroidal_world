package com.toroidalworld.core;

import java.util.List;

public class WrapDomain {
    public final int lowerBound;
    public final int upperBound;
    public final int domainLength;

    private final int seamRadius;

    private final double halfLength;

    private final List<ForeignSpan> foreignSpans;

    public WrapDomain(int lowerBound, int upperBound) {
        this(lowerBound, upperBound, List.of());
    }

    public WrapDomain(int lowerBound, int upperBound, List<ForeignSpan> foreignSpans) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.domainLength = Math.abs(upperBound - lowerBound);
        this.seamRadius = this.domainLength / 2;
        this.halfLength = this.domainLength / 2.0;
        this.foreignSpans = List.copyOf(foreignSpans);
    }

    public boolean isForeign(int coord) {
        for (ForeignSpan span : foreignSpans) {
            if (span.contains(coord)) {
                return true;
            }
        }

        return false;
    }

    public boolean isForeign(double coord) {
        for (ForeignSpan span : foreignSpans) {
            if (span.contains(coord)) {
                return true;
            }
        }

        return false;
    }

    public int lapsOver(double coord) {
        if (!isOver(coord)) {
            return 0;
        }

        long laps = (long) Math.floor((coord - lowerBound) / domainLength);
        if (coord - (double) laps * domainLength < lowerBound) {
            laps--;
        }

        return Math.toIntExact(laps);
    }

    public double wrap(double coord) {
        if (!isOver(coord)) {
            return coord;
        }

        double wrapped = coord - (double) lapsOver(coord) * domainLength;
        return wrapped >= upperBound ? lowerBound : wrapped;
    }

    public int wrap(int coord) {
        return isOver(coord) ? lowerBound + Math.floorMod(coord - lowerBound, domainLength) : coord;
    }

    public int wrapFrom(int anchor, int coord) {
        if (isForeign(anchor) || isForeign(coord)) {
            return coord;
        }

        return anchor + Math.floorMod(coord - anchor, domainLength);
    }

    public double unwrap(double anchor, double wrapped) {
        return unwrapAround(anchor, wrapped);
    }

    public int unwrap(int anchor, int wrapped) {
        return unwrapAround(anchor, wrapped);
    }

    public double unwrapAround(double refCoord, double coord) {
        double laps = lapsToward(refCoord, coord);
        return laps == 0.0 ? coord : coord - laps * domainLength;
    }

    public int unwrapAround(int refCoord, int coord) {
        long laps = lapsToward(refCoord, coord);
        return laps == 0 ? coord : (int) (coord - laps * domainLength);
    }

    private double lapsToward(double refCoord, double coord) {
        double quotient = (coord - refCoord) / domainLength;
        double laps = Math.round(quotient);
        if (laps == 0.0) {
            return 0.0;
        }

        if (isForeign(refCoord) || isForeign(coord)) {
            return 0.0;
        }

        if (quotient - laps == -0.5 && laps > 0) {
            return laps - 1;
        }

        return laps;
    }

    private long lapsToward(int refCoord, int coord) {
        long delta = (long) coord - refCoord;
        if (delta >= -seamRadius && delta <= seamRadius) {
            return 0;
        }

        if (isForeign(refCoord) || isForeign(coord)) {
            return 0;
        }

        long laps = Math.floorDiv(delta, domainLength);
        long doubledRemainder = 2 * (delta - laps * domainLength);
        if (doubledRemainder > domainLength || (doubledRemainder == domainLength && laps < 0)) {
            return laps + 1;
        }

        return laps;
    }

    public int otherCopy(int coord, int delta) {
        if (isForeign(coord)) {
            return coord;
        }

        return delta > 0 ? coord - domainLength : coord + domainLength;
    }

    public boolean loops() {
        return true;
    }

    public boolean isWholeLaps(int delta) {
        return Math.floorMod(delta, domainLength) == 0;
    }

    public boolean isOver(double coord) {
        return (coord < lowerBound || coord >= upperBound) && !isForeign(coord);
    }

    public boolean isOver(int coord) {
        return (coord < lowerBound || coord >= upperBound) && !isForeign(coord);
    }

    public int overshoot(int coord) {
        if (!isOver(coord)) {
            return 0;
        }

        return coord >= upperBound ? coord - (upperBound - 1) : lowerBound - coord;
    }

    public boolean containsSpan(double min, double max) {
        if (isForeign(min) && isForeign(max)) {
            return true;
        }

        return min >= lowerBound && min < upperBound && max <= upperBound;
    }

    public boolean fitsInHalf(double span) {
        return 2 * span <= domainLength;
    }

    public boolean coversWorld(double span) {
        return span >= domainLength;
    }

    public boolean foldsOntoItself(int coordinateCount) {
        return coordinateCount > domainLength;
    }

    public int stepsToCoverTheWorld(int step) {
        if (step <= 0) {
            return Integer.MAX_VALUE;
        }

        int halfWorld = (domainLength + 1) / 2;
        return (halfWorld + step - 1) / step;
    }

    public boolean spansSeam(double fromCoord, double toCoord) {
        if (isForeign(fromCoord) || isForeign(toCoord)) {
            return false;
        }

        return !fitsInHalf(Math.abs(toCoord - fromCoord));
    }

    public boolean spansSeam(int fromCoord, int toCoord) {
        if (isForeign(fromCoord) || isForeign(toCoord)) {
            return false;
        }

        return Math.abs((long) toCoord - fromCoord) > seamRadius;
    }

    public int foldSpanStart(int minCoord, int maxCoord) {
        return spansSeam(minCoord, maxCoord) ? maxCoord : minCoord;
    }

    public int foldSpanEnd(int minCoord, int maxCoord) {
        return spansSeam(minCoord, maxCoord) ? minCoord + domainLength : maxCoord;
    }

    public boolean overlaps(int aMin, int aMax, int bMin, int bMax) {
        if (isForeign(aMin) || isForeign(aMax) || isForeign(bMin) || isForeign(bMax)) {
            return aMin <= bMax && bMin <= aMax;
        }

        long lowestShift = (long) aMin - bMax;
        long highestShift = (long) aMax - bMin;
        return Math.floorDiv(highestShift, domainLength) * (long) domainLength >= lowestShift;
    }

    public int[] laps(int regionMin, int regionMax) {
        return lapsBetween(lowerBound, upperBound - 1, regionMin, regionMax);
    }

    public int[] lapsBetween(int spanMin, int spanMax, int regionMin, int regionMax) {
        if (isForeign(spanMin) || isForeign(spanMax) || isForeign(regionMin) || isForeign(regionMax)) {
            return spanMin <= regionMax && regionMin <= spanMax ? new int[] {0, 0} : new int[] {0, -1};
        }

        return new int[] {
                Math.toIntExact(Math.ceilDiv((long) regionMin - spanMax, domainLength)),
                Math.toIntExact(Math.floorDiv((long) regionMax - spanMin, domainLength))};
    }

    public double foldDelta(double delta) {
        if (delta > halfLength) {
            return delta - domainLength;
        }

        if (delta < -halfLength) {
            return delta + domainLength;
        }

        return delta;
    }

    public int foldDelta(int delta) {
        if (delta > seamRadius) {
            return delta - domainLength;
        }

        if (delta < -seamRadius) {
            return delta + domainLength;
        }

        return delta;
    }

    public double mapFrom(WrapDomain source, double coord, double declaredScale) {
        return wrap(coord * scaleFrom(source, declaredScale));
    }

    public double scaleFrom(WrapDomain source, double declaredScale) {
        return source.scaleTo(this, declaredScale);
    }

    protected double scaleTo(WrapDomain destination, double declaredScale) {
        return (double) destination.domainLength / domainLength;
    }

    public List<double[]> spans(double min, double max) {
        if (isForeign(min) && isForeign(max)) {
            return List.of(new double[] {min, max});
        }

        double length = Math.min(max - min, domainLength);
        double start = wrap(min);
        double end = start + length;
        if (end <= upperBound) {
            return List.of(new double[] {start, end});
        }

        return List.of(
                new double[] {start, upperBound},
                new double[] {lowerBound, lowerBound + (end - upperBound)});
    }

    public List<int[]> cellSpans(int min, int max) {
        if (isForeign(min) && isForeign(max)) {
            return List.of(new int[] {min, max});
        }

        int length = (int) Math.min((long) max - min + 1, domainLength);
        int start = wrap(min);
        int end = start + length - 1;
        if (end < upperBound) {
            return List.of(new int[] {start, end});
        }

        return List.of(
                new int[] {start, upperBound - 1},
                new int[] {lowerBound, lowerBound + (end - upperBound)});
    }

    public static final class Noop extends WrapDomain {
        public Noop() {
            super(0, 0);
        }

        @Override
        public double wrap(double coord) {
            return coord;
        }

        @Override
        public int wrap(int coord) {
            return coord;
        }

        @Override
        public int wrapFrom(int anchor, int coord) {
            return coord;
        }

        @Override
        public double unwrap(double anchor, double wrapped) {
            return wrapped;
        }

        @Override
        public int unwrap(int anchor, int wrapped) {
            return wrapped;
        }

        @Override
        public double unwrapAround(double refCoord, double coord) {
            return coord;
        }

        @Override
        public int unwrapAround(int refCoord, int coord) {
            return coord;
        }

        @Override
        public int otherCopy(int coord, int delta) {
            return coord;
        }

        @Override
        public boolean loops() {
            return false;
        }

        @Override
        public boolean isWholeLaps(int delta) {
            return delta == 0;
        }

        @Override
        public boolean isOver(double coord) {
            return false;
        }

        @Override
        public boolean isOver(int coord) {
            return false;
        }

        @Override
        public int overshoot(int coord) {
            return 0;
        }

        @Override
        public boolean spansSeam(int fromCoord, int toCoord) {
            return false;
        }

        @Override
        public boolean containsSpan(double min, double max) {
            return true;
        }

        @Override
        public boolean fitsInHalf(double span) {
            return true;
        }

        @Override
        public boolean coversWorld(double span) {
            return false;
        }

        @Override
        public boolean foldsOntoItself(int coordinateCount) {
            return false;
        }

        @Override
        public int stepsToCoverTheWorld(int step) {
            return Integer.MAX_VALUE;
        }

        @Override
        public double scaleFrom(WrapDomain source, double declaredScale) {
            return declaredScale;
        }

        @Override
        protected double scaleTo(WrapDomain destination, double declaredScale) {
            return declaredScale;
        }

        @Override
        public boolean overlaps(int aMin, int aMax, int bMin, int bMax) {
            return aMin <= bMax && bMin <= aMax;
        }

        @Override
        public int[] laps(int regionMin, int regionMax) {
            return new int[] {0, 0};
        }

        @Override
        public int[] lapsBetween(int spanMin, int spanMax, int regionMin, int regionMax) {
            return spanMin <= regionMax && regionMin <= spanMax ? new int[] {0, 0} : new int[] {0, -1};
        }

        @Override
        public double foldDelta(double delta) {
            return delta;
        }

        @Override
        public int foldDelta(int delta) {
            return delta;
        }

        @Override
        public List<double[]> spans(double min, double max) {
            return List.of(new double[] {min, max});
        }

        @Override
        public List<int[]> cellSpans(int min, int max) {
            return List.of(new int[] {min, max});
        }
    }
}
